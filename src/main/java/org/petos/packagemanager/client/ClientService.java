package org.petos.packagemanager.client;

import org.petos.packagemanager.transfer.NetworkPacket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.petos.packagemanager.transfer.NetworkPacket.BytesPerCommand;
import static org.petos.packagemanager.transfer.NetworkPacket.valueOf;

public class ClientService implements Runnable {
public static class ServerAccessException extends IOException {
	public ServerAccessException(String msg){
	      super(msg);
	}
}
private static final int DEFAULT_SERVER_TIMEOUT = 1000_000;
private final Socket socket;
private NetworkPacket request;
private Consumer<Exception> errorHandler = (e) -> {
};
private ResponseHandler responseHandler = (n, s) -> {
};

ClientService(String uri, int port) throws ServerAccessException {
      try {
	    socket = new Socket(uri, port);
	    socket.setSoTimeout(DEFAULT_SERVER_TIMEOUT);
      } catch (UnknownHostException e) {
	    throw new ServerAccessException("Servier is too busy");
      } catch (IOException  e) {
	    throw new ServerAccessException("Unknown system error");
      }
}

public ClientService setExceptionHandler(Consumer<Exception> handler) {
      this.errorHandler = handler;
      return this;
}

public ClientService setRequest(NetworkPacket packet) {
      assert packet.direction() == NetworkPacket.PacketDirection.Request;
      this.request = packet;
      return this;
}

public ClientService setResponseHandler(ResponseHandler handler) {
      responseHandler = handler;
      return this;
}

private Optional<NetworkPacket> getResponse(InputStream input) throws IOException {
      byte[] bytes = new byte[BytesPerCommand];
      int cbRead = input.read(bytes);
      Optional<NetworkPacket> packet = Optional.empty();
      if(cbRead == BytesPerCommand){
	    packet = valueOf(bytes);
	    if (packet.isPresent()) {
		  var selfPacket = packet.get();
		  byte[] payload = input.readNBytes(selfPacket.payloadSize());
		  selfPacket.setPayload(payload);
	    }
      }
      return packet;
}

@Override
public void run() {
      try (DataOutputStream output = new DataOutputStream(socket.getOutputStream());
	   DataInputStream input = new DataInputStream(socket.getInputStream())) {
	    output.write(request.rawPacket());
	    Optional<NetworkPacket> packet;
	    do {
		  packet = getResponse(input);
	    } while (packet.isEmpty());
	    responseHandler.accept(packet.get(), socket);
      } catch (Exception e) {
	    errorHandler.accept(e);
      }
}
}