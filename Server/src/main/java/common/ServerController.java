package common;


import transfer.NetworkExchange;

import static transfer.NetworkExchange.*;


public interface ServerController {
      static void defaultResponse(NetworkExchange exchange){
	    exchange.setResponse(ResponseType.Approve, ResponseCode.NO_PAYLOAD);
      }
      void accept(final NetworkExchange exchange) throws Exception;
      default void error(NetworkExchange exchange){ //last attemp to send message to client
	    exchange.setResponse(ResponseType.Decline, ResponseCode.INTERNAL_ERROR);
      }
}
