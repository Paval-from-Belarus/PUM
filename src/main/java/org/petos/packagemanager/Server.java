package org.petos.packagemanager;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.petos.packagemanager.NetworkExchange.*;
import org.petos.packagemanager.server.ServerController;
import org.petos.packagemanager.server.ServerDispatcher;

public class Server {
private static final Logger logger = LogManager.getLogger(Server.class);
final private int port;
private ForkJoinPool threadPool;
private Map<Integer, PackageInfo> table;

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
      message.setResponse(ResponseType.Approve, NetworkExchange.NO_PAYLOAD);
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
      private final int DEFAULT_TIMEOUT = 5000;
      private Socket socket;
      private ServerController handler;

      public ClientService(Socket socket, ServerController handler) {
	    this.handler = handler;
	    this.socket = socket;
      }

      private Optional<NetworkPacket> getRequest(InputStream input) throws IOException {
	    List<Byte> bytes = new ArrayList<>();
	    ByteBuffer buffer = ByteBuffer.wrap(new byte[4]);
	    int signCnt = 0;
	    while (input.read(buffer.array()) > 0 && signCnt != 2) {
		  int controlSign = buffer.getInt();
		  signCnt += controlSign == NetworkPacket.BytesPerCommand ? 1 : 0;
		  for (byte value : buffer.array())
			bytes.add(value);
	    }
	    if (signCnt < 2)
		  return Optional.empty();
	    int index = 0;
	    byte[] rawBytes = new byte[bytes.size()];
	    for (byte value : bytes) {
		  rawBytes[index] = value;
		  index += 1;
	    }
	    return NetworkPacket.valueOf(rawBytes);

      }

      @Override
      public void run() {
	    try (DataInputStream input = new DataInputStream(socket.getInputStream());
		 DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {
		  socket.setSoTimeout(DEFAULT_TIMEOUT);
		  Optional<NetworkPacket> packet;
		  do {
			packet = getRequest(input);
		  } while (packet.isEmpty());
		  NetworkConnection connection = new NetworkConnection(input, output);
		  NetworkExchange message = new NetworkExchange(packet.get(), connection);
		  try {
			handler.accept(message);
		  } catch (Exception e) {
			handler.error(message);
			logger.warn("Error in Server Controller: " + e.getMessage());
		  }
	    }
	    catch (SocketTimeoutException e){
		  logger.warn("Client not response");
	    }
	    catch (IOException e) {
		  logger.error("Socket error: " + e.getMessage());
		  throw new RuntimeException(e);
	    }

      }
}

public static void main(String[] args) {
      Server server = new Server(3344);
      server.setController(new ServerDispatcher());
      server.start();
}
}
