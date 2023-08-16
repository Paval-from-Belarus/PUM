package org.petos.pum.server;

import org.petos.pum.server.common.NetworkProperties;
import org.petos.pum.server.common.SerializationProperties;
import org.petos.pum.server.network.MessageBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.Transformers;
import org.springframework.integration.ip.dsl.Tcp;
import org.springframework.integration.ip.tcp.connection.MessageConvertingTcpMessageMapper;
import org.springframework.integration.ip.tcp.serializer.MapJsonSerializer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import transfer.BinaryObjectMapper;
import transfer.BinaryObjectProperties;
import transfer.NetworkPacket;

/**
 * @author Paval Shlyk
 * @since 15/08/2023
 */
@SpringBootApplication
@Configuration
@EnableIntegration
public class ServerApp {
public static void main(String[] args) {
      SpringApplication.run(ServerApp.class, args);
}
@Autowired
public ServerApp(NetworkProperties networkProperties, SerializationProperties serializationProperties) {
      this.networkProperties = networkProperties;
      this.serializationProperties = serializationProperties;
}

@Bean
public MessageConvertingTcpMessageMapper messageConverter() {
      MessageBuilder builder = new MessageBuilder(serializationProperties.serializer());
      new MapJsonSerializer()
      return new MessageConvertingTcpMessageMapper(builder);
}
@Bean
public IntegrationFlow integrationFlow() {
      var netServer = Tcp.netServer(networkProperties.getPort())
			  .singleUseConnections(networkProperties.isSingle())
			  .soTimeout(networkProperties.getTimeout())
//                          .serializer()
//                          .deserializer()
			  .serializer()
			  .mapper(messageConverter())
			  .get();
      Object someService = new Object();
      IntegrationFlow
	  .from(Tcp.inboundGateway(netServer)
		    .requestChannel("unsecureInput")
		    .replyChannel("unsecureOutput"));
      Transformers.objectToString()
      return IntegrationFlow
		 .from(Tcp.inboundGateway(netServer))
		 .route(p -> null, m -> null)
		 .<String>filter((payload) -> !"junk".equals(payload))
		 .transform(someService, "methodOfService")
		 .channel("nextServiceChannel")
		 .get();
//      MessageHandlerChain
}

@Bean
public IntegrationFlow outputChannel(NetworkPacket packet) {
      return f -> f
		      .handle
}
@Bean
@ServiceActivator(inputChannel = "tcpInput")
public MessageHandler sendChatMessageHandler(String data) {
      return new MessageHandler(){

	    @Override
	    public void handleMessage(Message<?> message) throws MessagingException {

	    }
      };
}
@Bean
public MessageChannel inputChannel() {
      QueueChannel channel = new QueueChannel(12);
      DirectChannel directChannel = new DirectChannel();
      directChannel.setDatatypes();
//      msg.getHeaders().getReplyChannel()
      MessageChannel other = new DirectChannel();
      return channel;
}
@Bean
@ServiceActivator(inputChannel = "input")
public String myService(@Header("value")String header, @Payload String payload) {
	return "";
}

private final NetworkProperties networkProperties;
private final SerializationProperties serializationProperties;
}
