package main;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.Transformers;
import org.springframework.integration.ip.dsl.Tcp;

import java.util.logging.Logger;

@Configuration
@EnableIntegration
public class AppConfiguration {
Logger logger = Logger.getLogger(AppConfiguration.class.getName());
private final NetworkProperties networkProperties;
@Autowired
public AppConfiguration(NetworkProperties properties) {
      this.networkProperties = properties;
}
@Bean
public IntegrationFlow integrationFlow() {
      logger.info("Hello");
      var nioServer = Tcp.netServer(networkProperties.getPort())
			  .soTimeout(networkProperties.getTimeout())
			  .singleUseConnections(networkProperties.isSingle())
//			  .deserializer()
			  .get();
      return IntegrationFlow.from(Tcp.inboundGateway(nioServer).get())
				 .transform(Transformers.objectToString())
				 .get();
}
}
