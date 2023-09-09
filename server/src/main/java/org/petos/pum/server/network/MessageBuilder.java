package org.petos.pum.server.network;


import org.jetbrains.annotations.NotNull;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConverter;
import org.petos.pum.networks.transfer.BinaryObjectMapper;
import org.petos.pum.networks.transfer.NetworkExchange;
import org.petos.pum.networks.transfer.NetworkPacket;

import java.util.HashMap;
import java.util.Map;

import static org.petos.pum.networks.transfer.NetworkExchange.*;

/**
 * @author Paval Shlyk
 * @since 15/08/2023
 */
public class MessageBuilder implements MessageConverter {
/**
 * @param serializer
 * @param requestMap
 */
public MessageBuilder(BinaryObjectMapper serializer, Map<NetworkExchange.RequestType, Class<?>> requestMap) {
      this.serializer = serializer; //serializer is supposed to be thread-safe class
      this.requestMap = requestMap; //copy to reduce multi-threading issues
}

@Override
public Object fromMessage(Message<?> message, Class<?> targetClass) {
      return serializer.serialize(message.getPayload());
}

@Override
@Nullable
public Message<?> toMessage(@NotNull Object transferred, @Nullable MessageHeaders headers) {
      Message<?> result;
      if (transferred instanceof NetworkPacket packet) {
	    Object payload = null;
	    if (headers == null) {
		  headers = new MessageHeaders(new HashMap<>());
	    }
	    if (packet.containsCode(RequestCode.TRANSFER_ATTACHED_FORMAT) && packet.hasData()) {
		  payload = constructPayload(packet.type(RequestType.class), packet.data());
	    }
	    headers.put(RequestHeaders.PACKET_TYPE, packet.type());
	    headers.put(RequestHeaders.RESPONSE_FORMAT, RequestHeaders.ResponseFormat.valuesOf(packet.code()));
	    headers.put(RequestHeaders.TRANSFER_FORMAT, RequestHeaders.TransferFormat.valuesOf(packet.code()));
	    result = new NetworkMessage(headers, payload);
      } else {
	    result = null;
      }
      return result;
}

private @Nullable Object constructPayload(RequestType request, byte[] data) {
      Object result;
      if (requestMap.containsKey(request)) {
	    result = serializer.construct(data, requestMap.get(request));
      } else {
	    result = null;
      }
      return result;
}

private static class NetworkMessage implements Message<Object> {
      private NetworkMessage(MessageHeaders headers, Object payload) {
	    this.headers = headers;
	    this.payload = payload;
      }

      @Override
      @Nullable
      public Object getPayload() {
	    return payload;
      }

      @Override
      public MessageHeaders getHeaders() {
	    return headers;
      }

      private final MessageHeaders headers;
      private final Object payload;
}

private final BinaryObjectMapper serializer;
private final Map<RequestType, Class<?>> requestMap;
}
