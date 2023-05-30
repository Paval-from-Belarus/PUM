package common;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import transfer.*;

import static transfer.NetworkExchange.*;

public class Server {
private static final Logger logger = LogManager.getLogger(Server.class);
final private int port;
private ForkJoinPool threadPool;

{
      threadPool = new ForkJoinPool();
}

public Server(int port) {
      this.port = port;
      this.rootHandler = this::defaultController;
}

private ServerController rootHandler;

public Server setController(ServerController handle) {
      this.rootHandler = handle;
      return this;
}

private void defaultController(NetworkExchange message) {
      message.setResponse(ResponseType.Approve, ResponseCode.NO_PAYLOAD);
}

public void start() {
      try (ServerSocket server = new ServerSocket(port)) {
	    while (!server.isClosed()) {
		  Socket client = server.accept();
		  ClientService service = new ClientService(client, this.rootHandler);
		  threadPool.submit(service);
	    }
      } catch (IOException e) {
	    throw new RuntimeException(e); //something really hard
      }
}

static class ClientService implements Runnable {
      private final int DEFAULT_TIMEOUT = 2500_000;
      private final Socket socket;
      private final ServerController handler;

      public ClientService(Socket socket, ServerController handler) {
	    this.handler = handler;
	    this.socket = socket;
      }

      private Optional<NetworkPacket> getRequest(InputStream input) throws IOException {
	    byte[] controlBuffer = new byte[NetworkPacket.BytesPerCommand];
	    Optional<NetworkPacket> optional = Optional.empty();
	    int cbRead = input.read(controlBuffer);
	    if (cbRead == NetworkPacket.BytesPerCommand && (optional = NetworkPacket.valueOf(controlBuffer)).isPresent()) {
		  var packet = optional.get();
		  byte[] payload = new byte[packet.payloadSize()];
		  cbRead = input.read(payload);
		  if (cbRead == packet.payloadSize())
			packet.setPayload(payload);
		  else
			optional = Optional.empty();
	    }
	    return optional;
      }

      private void sendResponse(NetworkPacket response, NetworkConnection connection, OutputStream output) throws IOException {
	    byte[] collected = connection.collect();
	    int payloadSize = collected.length + response.payloadSize();
	    response.setPayloadSize(payloadSize);
	    output.write(response.construct());
	    output.write(collected);
      }

      @Override
      public void run() {
	    try (DataInputStream input = new DataInputStream(socket.getInputStream())) {
		  DataOutputStream output = new DataOutputStream(socket.getOutputStream());
		  socket.setSoTimeout(DEFAULT_TIMEOUT);
		  Optional<NetworkPacket> request = getRequest(input);
		  if (request.isPresent()) {
			NetworkConnection connection = new NetworkConnection(input);
			NetworkExchange exchange = new NetworkExchange(request.get(), connection);
			try {
			      handler.accept(exchange);
			} catch (Exception e) {
			      handler.error(exchange);//it's obligatory for Controller to process error
			      logger.warn("Error in common.Server Controller: " + e.getMessage());
			} finally {
			      if (exchange.response().isPresent())
				    sendResponse(exchange.response().get(), connection, output);
			      connection.close();
			}
		  } else {
			NetworkPacket packet = new NetworkPacket(ResponseType.Decline, ResponseCode.NO_PAYLOAD);
			output.write(packet.construct());
		  }
	    } catch (SocketTimeoutException e) {
		  logger.warn("Client not response");
	    } catch (IOException e) {
		  logger.error("Socket error: " + e.getMessage());
		  throw new RuntimeException(e);
	    }

      }
}

public static void main(String[] args) {
      Server server = new Server(3344);
      PackageStorage storage = new PackageStorage();
      Serializer serializer = new Serializer();
      try {
	    URL resource =  Server.class.getClassLoader().getResource("transfer-packages.xml");
	    assert resource != null;
	    TransferConfig config = new TransferConfig(serializer);
	    config.configure(resource.toURI());
      } catch (URISyntaxException e) {
	    throw new RuntimeException(e);
      } catch (ClassNotFoundException e) {
	    throw new RuntimeException(e);
      }
      server.setController(new ServerDispatcher(storage, serializer));
      server.start();
      storage.close();
}
}
