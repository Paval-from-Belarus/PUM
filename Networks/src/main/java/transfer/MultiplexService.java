package transfer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class MultiplexService implements NetworkService {
public MultiplexService(List<UrlInfo> list) throws ServerAccessException {
      if (list.size() == 0) {
	    throw new ServerAccessException("Servers are not specified");
      }
      services = new ArrayList<>(list.size());
      for (var info : list) {
	    SimplexService service = null;
	    try {
		service = new SimplexService(info);
	    } catch (ServerAccessException ignored) {
	    }
	    if (service != null) {
		  services.add(service);
	    }
      }
      if (services.size() == 0) {
	    throw new ServerAccessException("The no server responses");
      }
      scheduler = (ThreadPoolExecutor) Executors.newFixedThreadPool(services.size());
      tasks = new Vector<>();
}

@Override
public NetworkService setTailWriter(TailWriter writer) {
      services.forEach(service -> service.setTailWriter(writer));
      return this;
}

@Override
public NetworkService setExceptionHandler(Consumer<Exception> handler) {
      services.forEach(service -> service.setExceptionHandler(handler));
      return this;
}

@Override
public NetworkService setRequest(NetworkPacket packet) {
      services.forEach(service -> service.setRequest(packet));
      return this;
}

@Override
public NetworkService setResponseHandler(ResponseHandler handler) {
      for (var service : services) {
	    service.setResponseHandler((NetworkPacket p) -> {
		  //handle is a method that will be invoked in certain thread
		  long id = Thread.currentThread().getId();
		  UrlInfo url = service.getUrlInfo();
		  var footprint = new TaskFootprint(id, url);
		  int index = tasks.indexOf(footprint);
		  if (index == -1) {
			tasks.add(footprint);
		  } else {
			tasks.set(index, footprint);
		  }
		  handler.accept(p);
	    });
      }
      return this;
}

/**
 * Last info that caused closing of the service. It's impossible to get the last info in case of closing
 * the <code>MutliplexService</code> not in the ResponseHandler
 */
public Optional<UrlInfo> lastInfo() {
      if (lastThread == null)
	    throw new IllegalStateException("The service was not closed");
      return tasks.stream().filter(task -> task.threadId == lastThread).map(TaskFootprint::url).findAny();
}


@Override
public void run() {
      services.forEach(scheduler::execute);
      scheduler.shutdown();
      try {
	    boolean isTerminated = false;
	    //the possible cause in case of buggy SimplexService is
		    while (!isTerminated) { //do while client is working
		  isTerminated = scheduler.awaitTermination(RESPONSE_TIMEOUT, TimeUnit.MILLISECONDS);
	    }
      } catch (InterruptedException ignored) {
	    //simply, awaiting is finished
      }
}

/**
 * The only single thread can close the service. To get the last uri was used to get info, ResponseHandler
 * should close the service.
 */
@Override
public synchronized void close() throws Exception {
      if (lastThread != null) {
	    return;
      }
      scheduler.shutdownNow(); //force close all threads
      for (var service : services) {
	    service.close();
      }
      lastThread = Thread.currentThread().getId(); //in single thread
}

private final List<SimplexService> services;
private final ThreadPoolExecutor scheduler;
private final Vector<TaskFootprint> tasks;
private Long lastThread;

private record TaskFootprint(long threadId, UrlInfo url) {
      @Override
      public boolean equals(Object object) {
	    boolean result = false;
	    if (object instanceof TaskFootprint other) {
		  result = other.threadId == this.threadId;
	    }
	    return result;
      }
}
}
