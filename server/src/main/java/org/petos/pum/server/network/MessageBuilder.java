package org.petos.pum.server.network;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConverter;
import transfer.BinaryObjectMapper;
import transfer.NetworkPacket;

import java.util.Map;

import static transfer.NetworkExchange.*;

/**
 * @author Paval Shlyk
 * @since 15/08/2023
 */
public class MessageBuilder implements MessageConverter {

public MessageBuilder(Map<RequestType, Class<?>> mapper, BinaryObjectMapper serializer) {
      this.mapper = mapper;
      this.serializer = serializer;
}

@Override
public Object fromMessage(@NotNull Message<?> message, @NotNull Class<?> targetClass) {
      return null; //do something interesting
}

@Override
@Nullable
public Message<?> toMessage(@NotNull Object transferred, MessageHeaders headers) {
      Message<?> result;
      if (transferred instanceof NetworkPacket packet) {
	    Object payload = null;
	    if (packet.containsCode(RequestCode.TRANSFER_ATTACHED_FORMAT) && mapper.containsKey(packet.type(RequestType.class))) {
		  Class<?> attachedClazz = mapper.get(packet.type(RequestType.class));
		  payload = serializer.construct(packet.data(), attachedClazz);
	    } else {
		  if (packet.containsCode(RequestCode.TRANSFER_ATTACHED_FORMAT)) {
			return null;
		  }
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
private static class NetworkMessage implements Message<Object> {
      private NetworkMessage(MessageHeaders headers, Object payload) {
	    this.headers = headers;
	    this.payload = payload;
      }

      @Override @Nullable
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
private final Map<RequestType, Class<?>> mapper;
private final BinaryObjectMapper serializer;
}
