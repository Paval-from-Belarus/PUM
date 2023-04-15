package org.petos.packagemanager.server;

import org.petos.packagemanager.transfer.NetworkExchange;

public interface ServerController {
      void accept(final NetworkExchange exchange) throws Exception;
      default void error(NetworkExchange exchange){ //last attemp to send message to client
	    exchange.setResponse(NetworkExchange.ResponseType.Decline, NetworkExchange.INTERNAL_ERROR);
      }
}
