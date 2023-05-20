package transfer;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.function.Consumer;

public interface NetworkService extends Runnable, AutoCloseable {
int DEFAULT_SERVER_TIMEOUT = 1000_000;

class ServerAccessException extends IOException {
      public ServerAccessException(String msg) {
	    super(msg);
      }
      public ServerAccessException(Throwable t) {
	    super(t);
      }
}

NetworkService setTailWriter(TailWriter writer);

NetworkService setExceptionHandler(Consumer<Exception> handler);

NetworkService setRequest(NetworkPacket packet);

NetworkService setResponseHandler(ResponseHandler handler);

}
