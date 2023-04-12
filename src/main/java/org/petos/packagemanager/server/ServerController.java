package org.petos.packagemanager.server;

import org.petos.packagemanager.NetworkExchange;

public interface ServerController {
      void accept(final NetworkExchange exchange);
      default void error(NetworkExchange exchange){ //last attemp to send message to client
	    exchange.setResponse(NetworkExchange.ResponseType.Decline, NetworkExchange.INTERNAL_ERROR);
      }
}
