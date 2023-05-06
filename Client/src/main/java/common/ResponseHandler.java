package common;


import transfer.NetworkPacket;

import java.io.IOException;
import java.net.Socket;

@FunctionalInterface
public interface ResponseHandler {
	void accept(NetworkPacket response, Socket socket) throws IOException;
}
