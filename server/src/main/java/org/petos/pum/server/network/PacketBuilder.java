package org.petos.pum.server.network;

import org.jetbrains.annotations.NotNull;
import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;
import org.petos.pum.networks.transfer.NetworkPacket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;


/**
 * @author Paval Shlyk
 * @since 15/08/2023
 */

public class PacketBuilder implements Serializer<NetworkPacket>, Deserializer<NetworkPacket> {
@Override
public @NotNull NetworkPacket deserialize(@NotNull InputStream input) throws IOException {
      Optional<NetworkPacket> optional;
      byte[] buffer = new byte[NetworkPacket.PACKET_SIZE];
      int cbRead = input.read(buffer);
      if (cbRead == NetworkPacket.PACKET_SIZE && (optional = NetworkPacket.valueOf(buffer)).isPresent()) {
	    NetworkPacket packet = optional.get();
	    byte[] payload = new byte [packet.payloadSize()];
	    cbRead = input.read(payload);
	    if (cbRead == packet.payloadSize()) {
		  packet.setPayload(payload);
	    } else {
		  throw new IOException("Network packet payload is missing");
	    }
      } else {
	    throw new IOException("Failed during constructing Network packet");
      }
      return optional.get();
}

@Override
public void serialize(@NotNull NetworkPacket object, @NotNull OutputStream outputStream) throws IOException {
	outputStream.write(object.construct());
}
}
