package org.petos.pum.server.properties;

import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

/**
 * @author Paval Shlyk
 * @since 15/08/2023
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Component
@ConfigurationProperties(prefix = "networks.connection")
public class NetworkProperties {
	@Getter @Setter
	private int port;
	@Getter @Setter
	private int timeout;
	@Getter @Setter
	private boolean single;
}