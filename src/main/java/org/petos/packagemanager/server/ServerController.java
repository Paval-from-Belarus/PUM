package org.petos.packagemanager.server;

import org.petos.packagemanager.transfer.NetworkExchange;

public interface ServerController {
      static void defaultResponse(NetworkExchange exchange){
	    exchange.setResponse(NetworkExchange.ResponseType.Approve, NetworkExchange.NO_PAYLOAD);
      }
      void accept(final NetworkExchange exchange) throws Exception;
      default void error(NetworkExchange exchange){ //last attemp to send message to client
	    exchange.setResponse(NetworkExchange.ResponseType.Decline, NetworkExchange.INTERNAL_ERROR);
      }
}
