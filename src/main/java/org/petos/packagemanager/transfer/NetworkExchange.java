package org.petos.packagemanager.transfer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

import static org.petos.packagemanager.transfer.NetworkPacket.*;
/**Current format support up to 2 GiB of payload.<br><i>P. S.</i>
 * Restrictions of Integer
 * */
public class NetworkExchange {
/**
 * <code>Approve</code> and <code>Decline</code> support
 * all type of code. It depends of previous request.
 * */
public enum ResponseType {Approve, Decline}

/**
 * the maximum count of request type is 32. The common rule for each RequestType is ignore code. It's not good practise to have several implementations
 * <h3>GetId</h3>
 * Request doesn't have specific format code (all codes, differents to NO_CODE will be ignored)
 * The single parameter is the Package Alias (name or short name of package)
 * The single parameter is
 * <h3>GetInfo</h3>
 * Request support two codes: <code>INT_FORMAT</code> and <code>STR_FORMAT</code>. Other format code should be ignored<br>
 * The parameters of GetInfo is PackageId and VersionId. FormatCode determines the format of VersionId (this is string label or int offset in package family)<br>
 * <h3>GetPayload</h3>
 * Supports two formats (as GetInfo). Generally, convention is the same as for GetInfo. Used to get instance payload
 * <h3>GetVersion</h3>
 * Supports two formats (as GetInfo, GetPayload). The general purpose of method is to convert (PackageId + VersionOffset) or
 * (PackageId + VersionLabel) ― generalized format instance invocation ― to the unique version (pure VersionId handle). The Server ensure
 * that each package instance has unique versionId (version handle). But to eliminate possible issues (very old client request the frequently updated package)
 * the server response has two values: versionID (handle) and versionLabel (String). By this way, is possible to upgrade each package
 * Convert
 */
public enum RequestType {GetAll, GetId, GetInfo, GetPayload, GetVersion, Unknown,
      /**
       * Those requests should be following sequentially
       * PublishInfo is used to public common info about package
       * */
      PublishInfo, PublishPayload,
      DeprecateVersion,
      @Deprecated UnPublish, @Deprecated GetFamily} //it's not recommended to use unpubslish request because can break local dependencies
//let's image that no UnPublish request (almost)
public static class RequestCode {
public static final int NO_CODE = 0;
public static final int INT_FORMAT = 1;
public static final int STR_FORMAT = 2;
public static final int TAIL_FORMAT = 4; //after common message (in NetworkPackage) also binary data is available
}
//Common codes
public static final int NO_PAYLOAD = 1;
public static final int FORBIDDEN = 2;
public static final int STR_FORMAT = 4;
public static final int SHORT_INFO = 8;
public static final int INT_FORMAT = 16;
public static final int VERBOSE_FORMAT = 32; //append strictly after NP command message ended by /n/r/n/r
public static final int BIN_FORMAT = 64;
public static final int ALL_PACKAGES_RESPONSE = STR_FORMAT | SHORT_INFO;
public static final int PACKAGE_ID_RESPONSE = INT_FORMAT;
public static final int PACKAGE_INFO_FORMAT = STR_FORMAT;
public static final int PAYLOAD_FORMAT = STR_FORMAT | BIN_FORMAT;
public static final int VERSION_INFO_FORMAT = STR_FORMAT;
public static final int PUBLISH_INFO_RESPONSE = INT_FORMAT;
public static final int PUBLISH_PAYLOAD_RESPONSE = INT_FORMAT;
//Success codes
public static final int CREATED = 8;
//Errors codes
public static final int INTERNAL_ERROR = 128;
public static final int ILLEGAL_REQUEST = 256;

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
