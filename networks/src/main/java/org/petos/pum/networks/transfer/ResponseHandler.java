package org.petos.pum.networks.transfer;


import java.io.IOException;

@FunctionalInterface
public interface ResponseHandler {
	void accept(NetworkPacket response) throws IOException;
}
