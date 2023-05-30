package transfer;

import java.io.IOException;
import java.util.function.Consumer;

public interface NetworkService extends Runnable, AutoCloseable {
int REQUEST_TIMEOUT = 2500_000;
int RESPONSE_TIMEOUT = 2500_000;//the default time for client operation
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
