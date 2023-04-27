package org.petos.packagemanager.transfer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

import static org.petos.packagemanager.transfer.NetworkPacket.*;
/**Current format support up to 2 GiB of payload
 * Restriction of Integer
 * */
public class NetworkExchange {
public enum ResponseType {Approve, Decline}

public enum RequestType {GetAll, GetId, GetInfo, GetFamily, GetPayload, Unknown,
      GetDependency, //get payload by dependency id
      /*Those requests should be following sequentially*/
      /**PublishInfo is used to public common info about package
       * */
      PublishInfo, PublishPayload,
      UpgradeVersion,
      @Deprecated UnPublish} //it's not recommended to use unpubslish request because can break local dependencies
//let's image that no UnPublish request (almost)

//Common codes
public static int NO_PAYLOAD = 1;
public static int FORBIDDEN = 2;
public static int STR_FORMAT = 4;
public static int SHORT_INFO = 8;
public static int INT_FORMAT = 16;
public static int VERBOSE_FORMAT = 32; //append strictly after NP command message ended by /n/r/n/r
public static int BIN_FORMAT = 64;
public static int ALL_PACKAGES_RESPONSE = STR_FORMAT | SHORT_INFO;
public static int PACKAGE_ID_RESPONSE = INT_FORMAT;
public static int PACKAGE_INFO_FORMAT = STR_FORMAT;
public static int PACKAGE_PAYLOAD_FORMAT = STR_FORMAT | BIN_FORMAT;
public static int PUBLISH_INFO_RESPONSE = INT_FORMAT;
public static int PUBLISH_PAYLOAD_RESPONSE = INT_FORMAT;
//Success codes
public static int CREATED = 8;
//Errors codes
public static int INTERNAL_ERROR = 128;
public static int ILLEGAL_REQUEST = 256;

public NetworkExchange(NetworkPacket request, NetworkConnection connection) {
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

public void setResponse(ResponseType type, int code, byte[] data) {
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
      return connection.getInput();
}

//previously will be sent Approve message and
public OutputStream getOutputStream() {
      return connection.getOutput();
}

public void finishExchange() throws IOException {
      connection.close();
}

}
