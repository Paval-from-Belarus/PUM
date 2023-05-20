package transfer;

import org.jetbrains.annotations.NotNull;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static transfer.NetworkPacket.BytesPerCommand;


public class SimplexService implements NetworkService {
private final Socket socket;
private final UrlInfo url;
private NetworkPacket request;
private Consumer<Exception> errorHandler = (e) -> {
};
private ResponseHandler responseHandler = (p) -> {
};
private TailWriter tailWriter = (s) -> {
};
private static @NotNull Socket getFirstSocket(UrlInfo info) throws IOException {
      List<UrlInfo> urls = new ArrayList<>(info.mirrors().length + 1);
      urls.add(info);
      Socket socket = null;
      urls.addAll(Arrays.asList(info.mirrors()));
      for (var url : urls) {
	    try {
		  socket = new Socket(url.url(), url.port());
	    } catch (ConnectException ignored) {

	    }
      }
      if (socket == null)
	    throw new ConnectException("Server doesn't response");
      return socket;

}
public SimplexService(UrlInfo info) throws ServerAccessException {
      Socket socket;
      try {
	    socket = getFirstSocket(info);
	    socket.setSoTimeout(DEFAULT_SERVER_TIMEOUT);
      } catch (IOException e) {
	    throw new ServerAccessException(e);
      }
      this.socket = socket;
      this.url = info;
}
public UrlInfo getUrlInfo(){
      return url;
}
public SimplexService setTailWriter(TailWriter tailWriter) {
      this.tailWriter = tailWriter;
      return this;
}
public SimplexService setExceptionHandler(Consumer<Exception> handler) {
      this.errorHandler = handler;
      return this;
}

public SimplexService setRequest(NetworkPacket packet) {
      assert packet.direction() == NetworkPacket.PacketDirection.Request;
      this.request = packet;
      return this;
}

public SimplexService setResponseHandler(ResponseHandler handler) {
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
	    output.write(request.construct());
	    tailWriter.write(output);
	    Optional<NetworkPacket> packet;
	    do {
		  packet = getResponse(input);
	    } while (packet.isEmpty());
	    responseHandler.accept(packet.get());
      } catch (Exception e) {
	    errorHandler.accept(e);
      }
}
@Override
public void close() throws Exception {
	socket.close();
}
}