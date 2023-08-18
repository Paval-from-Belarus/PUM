package org.petos.pum.server.network;

import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.annotation.Transformer;
import org.springframework.stereotype.Service;
import transfer.NetworkPacket;

/**
 * @author Paval Shlyk
 * @since 15/08/2023
 */

public class DummyActivator {
//@EndpointId("anyEndPoint")
/*
Let's imagine that define following code for activator:
@Bean
public IntegrationFlow someFlow() {
	return IntegrationFlow
			.from("inputChannel")
			.handle(dummyActivator, "someMethodToHandle") //dummyActivator instance should be created?
			.get();
}
 */
@ServiceActivator(inputChannel = "inputChannel")
public String someMethodToHandle() {
      return "";
}
@Transformer(inputChannel = "inputChannel", outputChannel = "nextServiceChannel")
public NetworkPacket exampleTransformer(byte[] bytes) {
      return NetworkPacket.valueOf(bytes).get();//year)
}
/*
Let's imagine that we have following bean
@Bean
public IntegrationFlow() someFlow() {
	return IntegrationFlow()
		.from("transformChannel")
		.transform("nextServiceChannel")
		.get();

}
 */

}
