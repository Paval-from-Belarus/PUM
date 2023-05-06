package common;

import transfer.NetworkPacket;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.function.Consumer;

import static transfer.NetworkPacket.BytesPerCommand;


public class ClientService implements Runnable {

public static class ServerAccessException extends IOException {
      public ServerAccessException(String msg) {
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
private TailWriter tailWriter = (s) -> {
};

ClientService(String uri, int port) throws ServerAccessException {
      try {
	    socket = new Socket(uri, port);
	    socket.setSoTimeout(DEFAULT_SERVER_TIMEOUT);
      } catch (UnknownHostException e) {
	    throw new ServerAccessException("Server is too busy");
      } catch (IOException e) {
	    throw new ServerAccessException("Unknown system error");
      }
}
public ClientService setTailWriter(TailWriter tailWriter) {
      this.tailWriter = tailWriter;
      return this;
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
      byte[] bytes = new byte[NetworkPacket.BytesPerCommand];
      int cbRead = input.read(bytes);
      Optional<NetworkPacket> packet = Optional.empty();
      if (cbRead == BytesPerCommand) {
	    packet = NetworkPacket.valueOf(bytes);
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
	    tailWriter.write(output);
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