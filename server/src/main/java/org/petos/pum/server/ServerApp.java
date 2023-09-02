package org.petos.pum.server;

import org.petos.pum.server.network.ManagerController;
import org.petos.pum.server.properties.NetworkProperties;
import org.petos.pum.server.properties.SerializationProperties;
import org.petos.pum.server.repositories.JpaConfig;
import org.petos.pum.server.network.MessageBuilder;
import org.petos.pum.server.network.PacketBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.ip.dsl.Tcp;
import org.springframework.integration.ip.tcp.connection.MessageConvertingTcpMessageMapper;
import org.springframework.messaging.MessageChannel;

/**
 * @author Paval Shlyk
 * @since 15/08/2023
 */
@SpringBootApplication
@Configuration
@EnableIntegration
@Import({JpaConfig.class})
public class ServerApp {
public static void main(String[] args) {
      SpringApplication.run(ServerApp.class, args);
}

@Autowired
ManagerController repository;
@Autowired
public ServerApp(NetworkProperties networkProperties, SerializationProperties serializationProperties) {
      this.networkProperties = networkProperties;
      this.serializationProperties = serializationProperties;
}

@Bean
public MessageBuilder messageBuilder() {
      return new MessageBuilder(serializationProperties.mapper(), serializationProperties.requestMap());
}
@Bean
public MessageConvertingTcpMessageMapper messageMapper() {
      return new MessageConvertingTcpMessageMapper(messageBuilder());

}
@Bean
public PacketBuilder builder() {
      return new PacketBuilder();
}
@Bean
public IntegrationFlow integrationFlow() {
      var netServer = Tcp.netServer(networkProperties.getPort())
			  .singleUseConnections(networkProperties.isSingle())
			  .soTimeout(networkProperties.getTimeout())
			  .serializer(builder())
			  .deserializer(builder())
			  .mapper(messageMapper());
//			  .interceptorFactoryChain() FactoryChain to add SSL support
      var gateway = Tcp.inboundGateway(netServer)
			.replyChannel("output")
			.errorChannel("output");
//      Object someService = new Object();
//      IntegrationFlow
//	  .from(Tcp.inboundGateway(netServer)
//		    .requestChannel("unsecureInput")
//		    .replyChannel("unsecureOutput"))
//      Transformers.objectToString()
      return IntegrationFlow
		 .from(gateway)
		 .channel("input")
		 .handle(repository)
		 .channel("output")
		 .get();
//		 .from(Tcp.inboundGateway(netServer))
//		 .route(p -> null, m -> null)
//		 .<String>filter((payload) -> !"junk".equals(payload))
//		 .transform(someService, "methodOfService")
//		 .handle("repository")
//		 .channel("nextServiceChannel")
//		 .get();
//      MessageHandlerChain
}
//@Bean
//@ServiceActivator(inputChannel = "tcpInput")
//public MessageHandler sendChatMessageHandler(String data) {
//      return new MessageHandler() {
//
//	    @Override
//	    public void handleMessage(Message<?> message) throws MessagingException {
//
//	    }
//      };
//}

//@Bean
//public MessageChannel insecureInput() {
//
//}
//
//@Bean
//public MessageChannel insecureOutput() {
//
//}
//
//@Bean
//public MessageChannel secureInput() {
//
//}
//
//@Bean
//public MessageChannel secureOutput() {
//
//}


@Bean({"input", "inputChannel"})
public MessageChannel inputChannel() {
//      MessageChannel channel = new QueueChannel(12);
//      directChannel.setDatatypes();
//      msg.getHeaders().getReplyChannel()
      DirectChannel channel = new DirectChannel();
      return channel;
}
@Bean({"output", "outputChannel"})
public MessageChannel outputChannel() {
	return new DirectChannel();
}
//@Bean
//public Object advice() {
//      return new RequestHandlerRetryAdvice();
//}
//@Bean
//@ServiceActivator(inputChannel = "input")
//public String myService(@Header("value") String header, @Payload String payload) {
//      return "";
//}

private final NetworkProperties networkProperties;
private final SerializationProperties serializationProperties;
}
