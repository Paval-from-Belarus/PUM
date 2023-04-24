package org.petos.packagemanager.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.petos.packagemanager.packages.PackageInfoDTO;
import org.petos.packagemanager.transfer.NetworkConnection;
import org.petos.packagemanager.transfer.NetworkExchange;
import org.petos.packagemanager.transfer.NetworkExchange.*;
import org.petos.packagemanager.transfer.NetworkPacket;

public class Server {
private static final Logger logger = LogManager.getLogger(Server.class);
final private int port;
private ForkJoinPool threadPool;
private Map<Integer, PackageInfoDTO> table;

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
      private final int DEFAULT_TIMEOUT = 5000_000;
      private Socket socket;
      private ServerController handler;

      public ClientService(Socket socket, ServerController handler) {
	    this.handler = handler;
	    this.socket = socket;
      }

      private Optional<NetworkPacket> getRequest(InputStream input) throws IOException {
	    byte[] controlBuffer = new byte[NetworkPacket.controlSize()];
	    Optional<NetworkPacket> optional = Optional.empty();
	    int cbRead = input.read(controlBuffer);
	    if (cbRead == NetworkPacket.controlSize() && (optional = NetworkPacket.valueOf(controlBuffer)).isPresent()) {
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
	    output.write(response.rawPacket());
	    output.write(collected);
      }

      @Override
      public void run() {
	    try (DataInputStream input = new DataInputStream(socket.getInputStream())) {
		  DataOutputStream output = new DataOutputStream(socket.getOutputStream());
		  socket.setSoTimeout(DEFAULT_TIMEOUT);
		  Optional<NetworkPacket> packet;
		  do {
			packet = getRequest(input);
		  } while (packet.isEmpty());
		  NetworkConnection connection = new NetworkConnection(input);
		  NetworkExchange message = new NetworkExchange(packet.get(), connection);
		  try {
			handler.accept(message);
			if (message.response().isEmpty())
			      ServerController.defaultResponse(message);
		  } catch (Exception e) {
			handler.error(message);//it's obligatory for Controller to process error
			logger.warn("Error in Server Controller: " + e.getMessage());
		  } finally {
			if (message.response().isPresent())
			      sendResponse(message.response().get(), connection, output);
			connection.close();
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
      server.setController(new ServerDispatcher(storage));
      server.start();
      storage.close();
}
}
