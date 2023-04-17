package org.petos.packagemanager.transfer;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Optional;

import org.petos.packagemanager.transfer.NetworkExchange.ResponseType;
import org.petos.packagemanager.transfer.NetworkExchange.RequestType;

/**
 * Network packet can hold two value<br>
 * Command type (specific value)<br>
 * The general interface of NetworkPacket is used to communicate in both sides<br>
 * But response packet also holds server response code
 * and payload (as String)<br>
 * Network packet is alternative for httpHeader<br>
 * It's specify the data following after<br>
 */
public class NetworkPacket{
//the size of command header that used to transfer payload
//payload is the not a part of `control packet`
public static int controlSize(){
      return BytesPerCommand;
}
public final static int CONTROL_SIGN = 0xAACC7798; //the last bit is used to determine packet type
private final static int CONTROL_REQUEST = CONTROL_SIGN;
private final static int CONTROL_RESPONSE = CONTROL_SIGN | 0x01;
private final static int RESPONSE_CONTROL_MASK = 0x01;
private final static int RESPONSE_CODE_MASK = 0xFE;
private final static int REQUEST_CONTROL_MASK = 0xFFFF;
public static int BytesPerCommand = 12;
public enum PacketDirection {Response, Request}
private byte[] data;
//each network packet exists only with one (Response or Request) type
private ResponseType responseType;
private RequestType requestType;
private final PacketDirection direction;
private int responseCode;
private int payloadSize; //in bytes?

{
      this.data = new byte[0];

}

/**
 * @param responseCode ― should be integer in specific range
 */
public NetworkPacket(ResponseType type, int responseCode) {
      this.responseType = type;
      this.responseCode = responseCode;
      this.direction = PacketDirection.Response;
}

public NetworkPacket(RequestType type) {
      this.requestType = type;
      this.direction = PacketDirection.Request;
}

public NetworkPacket(ResponseType type, int responseCode, byte[] data) {
      this(type, responseCode);
      setPayload(data);
}

public NetworkPacket(RequestType type, byte[] data) {
      this(type);
      setPayload(data);
}
public int payloadSize(){
      return payloadSize;
}
public void setPayloadSize(int size){
      this.payloadSize = size;
}
public void setPayload(@NotNull byte[] data) {
      this.data = data;
      this.payloadSize = data.length;
}
public void setPayload(Integer... values){
      ByteBuffer buffer = ByteBuffer.allocate(values.length * 4);
      for(Integer value : values)
	    buffer.putInt(value);
      setPayload(buffer.array());
}
public boolean hasData() {
      return this.data.length > 0;
}

public final byte[] data() {
      return this.data;
}
public int code(){
      return responseCode;
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
public final <T> T type(Class<T> type){
      return type.cast(type());
}

public @NotNull byte[] rawPacket() {
      byte[] result;
      if (direction == PacketDirection.Request)
	    result = getRawPacket(this, requestType);
      else
	    result = getRawPacket(this, responseType);
      return result;
}
public @NotNull String stringData(){
 	return bytesToString(data);
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
	    packet = new NetworkPacket(RequestType.values()[enumId]);
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
      int responseSign = type.ordinal() | packet.code() << 1;
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
      int requestSign = type.ordinal();
      ByteBuffer buffer = ByteBuffer.allocate(BytesPerCommand + payload.length);
      buffer.putInt(CONTROL_REQUEST);
      buffer.putInt(requestSign);
      buffer.putInt(payloadSize);
      buffer.put(payload);
      return buffer.array();
}
public static @NotNull String bytesToString(@NotNull byte[] bytes) {
      CharBuffer buffer = CharBuffer.allocate(bytes.length);
      for(byte letter : bytes) {
	    buffer.put((char) letter);
      }
      return String.valueOf(buffer.array());
}
/**
 * @return -1 if impossible to find packet
 * @return index of control bytes
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
public String toString(){
      byte[] bytes = rawPacket();
      StringBuilder strText = new StringBuilder(bytes.length);
      for(byte letter : bytes)
	    strText.append((char)letter);
      return strText.toString();
}
}
