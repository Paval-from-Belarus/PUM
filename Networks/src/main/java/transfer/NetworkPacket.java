package transfer;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import transfer.NetworkExchange.ResponseType;
import transfer.NetworkExchange.RequestType;

/**
 * Network packet can hold two value<br>
 * Command type (specific value)<br>
 * The general interface of NetworkPacket is used to communicate in both sides<br>
 * But response packet also holds server response code
 * and payload (as String)<br>
 * Network packet is alternative for httpHeader<br>
 * It's specify the data following after<br>
 */
public class NetworkPacket {
//the size of command header that used to transfer
//payload is the not a part of `control packet`
public static int controlSize() {
      return BytesPerCommand;
}

public final static int CONTROL_SIGN = 0xAACC7798; //the last bit is used to determine packet type
private final static int CONTROL_REQUEST = CONTROL_SIGN;
private final static int CONTROL_RESPONSE = CONTROL_SIGN | 0x01;
private final static int RESPONSE_CONTROL_MASK = 0x01;
private final static int REQUEST_CONTROL_MASK = 0x1F;
private final static int RESPONSE_CODE_MASK = 0xFFFFFFFE;
private final static int REQUEST_CODE_MASK = 0xFFFFFFE0;
public static int BytesPerCommand = 12;

public enum PacketDirection {Response, Request}

private byte[] data;
private int dataOffset;
//each network packet exists only with one (Response or Request) type
private ResponseType responseType;
private RequestType requestType;
private final PacketDirection direction;
private final int packetCode;
private int payloadSize; //in bytes?

{
      this.data = new byte[0];
      this.dataOffset = 0;
}

/**
 * @param requestCode ― should be integer in specific range
 */

public NetworkPacket(RequestType type, int requestCode) {
      this.requestType = type;
      this.direction = PacketDirection.Request;
      this.packetCode = requestCode;
}

public NetworkPacket(RequestType type, int requestCode, byte[] data) {
      this(type, requestCode);
      setPayload(data);
}

public NetworkPacket(ResponseType type, int responseCode, byte[] data) {
      this(type, responseCode);
      setPayload(data);
}

public NetworkPacket(ResponseType type, int responseCode) {
      this.responseType = type;
      this.packetCode = responseCode;
      this.direction = PacketDirection.Response;
}

public int payloadSize() {
      return payloadSize;
}

public void setPayloadSize(int size) {
      this.payloadSize = size;
}
public void setPayload(@NotNull String data) {
      this.data = data.getBytes(StandardCharsets.US_ASCII);
      this.payloadSize = this.data.length;
}
public void setPayload(@NotNull byte[] data) {
      this.data = data;
      this.payloadSize = data.length;
}

public void setPayload(byte[]... values) {
      int capacity = 0;
      for (byte[] bytes : values) {
	    capacity += bytes.length;
      }
      ByteBuffer buffer = ByteBuffer.allocate(capacity);
      for (byte[] bytes : values) {
	    buffer.put(bytes);
      }
      setPayload(buffer.array());
}

public boolean hasData() {
      return this.data.length > 0;
}

/**
 * It's preferable to use this method, because <code>code()</code> method return <i>raw packet code</i>, which pottentially
 * can contain other code values (differ from code)
 */
public boolean containsCode(int code) {
      return (packetCode | code) == packetCode;
}
public static boolean containsCode(int complex, int simple) {
      return (complex & simple) != 0;
}
public final byte[] data() {
      return this.data;
}
public @NotNull String stringData() {
      return Serializer.toString(data);
}

/**
 * Offset in byte array to get string value
 */
public @NotNull String stringFrom(int offset) {
      final int BUFF_LENGTH = Math.max(0, data.length - offset);
      String result = "";
      if (BUFF_LENGTH != 0 && data.length != 0) {
	    byte[] payload = new byte[BUFF_LENGTH];
	    System.arraycopy(data, offset, payload, 0, payload.length);
	    result = Serializer.toString(payload);
      }
      return result;
}
public int intFrom(int offset) {
      return ByteBuffer.wrap(data()).getInt(offset);
}
public int code(int mask) {
      return packetCode & mask;
}
public int code() {
      return packetCode;
}

public final PacketDirection direction() {
      return this.direction;
}

public final Enum<?> type() {
      if (direction == PacketDirection.Request)
	    return requestType;
      else
	    return responseType;
}

public final <T> T type(Class<T> type) {
      return type.cast(type());
}

public @NotNull byte[] construct() {
      byte[] result;
      if (direction == PacketDirection.Request)
	    result = getRawPacket(this, requestType);
      else
	    result = getRawPacket(this, responseType);
      return result;
}


public static Optional<NetworkPacket> valueOf(byte[] rawBytes) {
      ByteBuffer wrapper = ByteBuffer.wrap(rawBytes);
      int controlSign = wrapper.getInt(0);
      if ((controlSign & CONTROL_SIGN) != CONTROL_SIGN)
	    return Optional.empty();
      NetworkPacket packet;
      int typeSign = wrapper.getInt(4);
      if (controlSign == CONTROL_REQUEST) {
	    int enumId = typeSign & REQUEST_CONTROL_MASK;
	    int code = (typeSign & REQUEST_CODE_MASK) >>> 5;
	    packet = new NetworkPacket(RequestType.values()[enumId], code);
      } else {
	    int enumId = typeSign & RESPONSE_CONTROL_MASK;
	    int code = (typeSign & RESPONSE_CODE_MASK) >>> 1;
	    packet = new NetworkPacket(ResponseType.values()[enumId], code);
      }
      int payloadSize = wrapper.getInt(8);
      packet.setPayloadSize(payloadSize);
      return Optional.of(packet);
}

private static @NotNull byte[] getRawPacket(NetworkPacket packet, ResponseType type) {
      byte[] payload = packet.data();
      int payloadSize = packet.payloadSize();
      int responseSign = type.ordinal() | (packet.code() << 1); //only two commands
      ByteBuffer buffer = ByteBuffer.allocate(BytesPerCommand + payload.length);
      buffer.putInt(CONTROL_RESPONSE);
      buffer.putInt(responseSign);
      buffer.putInt(payloadSize);
      buffer.put(payload);
      return buffer.array();
}

private static @NotNull byte[] getRawPacket(NetworkPacket packet, RequestType type) {
      int payloadSize = packet.payloadSize();
      byte[] payload = packet.data();
      int requestSign = type.ordinal() | (packet.code() << 5);//only 32 commands
      ByteBuffer buffer = ByteBuffer.allocate(BytesPerCommand + payload.length);
      buffer.putInt(CONTROL_REQUEST);
      buffer.putInt(requestSign);
      buffer.putInt(payloadSize);
      buffer.put(payload);
      return buffer.array();
}
/**
 * @return -1 if impossible to find packet or index of control bytes
 */
private static int getPayloadOffset(final byte[] rawBytes) {
      if (rawBytes.length < BytesPerCommand)
	    return -1;
      ByteBuffer wrapper = ByteBuffer.wrap(rawBytes);
      int index;
      for (index = BytesPerCommand - 4; index < rawBytes.length - 4; index += 4) {
	    int controlSign = wrapper.getInt(index);
	    if (controlSign == CONTROL_SIGN)
		  break;
      }
      if (index > rawBytes.length)
	    return -1;// not found
      return index + 4;
}

@Override
public String toString() {
      byte[] bytes = construct();
      StringBuilder strText = new StringBuilder(bytes.length);
      for (byte letter : bytes)
	    strText.append((char) letter);
      return strText.toString();
}
}
