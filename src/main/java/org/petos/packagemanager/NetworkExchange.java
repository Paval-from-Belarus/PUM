package org.petos.packagemanager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.VarHandle;
import java.net.Socket;
import java.util.Optional;

import static org.petos.packagemanager.NetworkPacket.*;

public class NetworkExchange {
public enum ResponseType {Approve, Decline}

public enum RequestType {GetAll, GetName, GetId, Publish, Unknown}

//Common codes
public static int NO_PAYLOAD = 1;
public static int FORBIDDEN = 2;
//Success codes
public static int CREATED = 4;
//Errors codes
public static int INTERNAL_ERROR = 128;
public static int ILLEGAL_REQUEST = 256;

NetworkExchange(NetworkPacket request, NetworkConnection connection) {
      this.request = request;
      this.connection = connection;
}

private NetworkPacket response;
private final NetworkPacket request;
private PacketDirection direction;
private final NetworkConnection connection;

public void setResponse(ResponseType type, int code) {
      response = new NetworkPacket(type, code);
}

public void setResponse(ResponseType type, int code, String data) {
      response = new NetworkPacket(type, code, data);
}

public final NetworkPacket request() {
      return request;
}

public Optional<NetworkPacket> response() {
      if (response == null)
	    return Optional.empty();
      return Optional.of(response);
}

public InputStream getInputStream() {
      return null;
}

//previously will be sent Approve message and
public OutputStream getOutputStream() {
      return null;
}

public void finishExchange() throws IOException {
      connection.close();
}

}
