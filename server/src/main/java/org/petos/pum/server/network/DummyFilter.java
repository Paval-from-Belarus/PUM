package org.petos.pum.server.network;

import org.springframework.integration.annotation.Filter;
import org.springframework.integration.annotation.MessageEndpoint;

/**
 * @author Paval Shlyk
 * @since 15/08/2023
 */
@MessageEndpoint
public class DummyFilter {
@Filter(inputChannel = "inputChannel", outputChannel = "outputChannel",
adviceChain = "advice", discardChannel = "discard")
public boolean filter(String s) {
      return false;
}

}
