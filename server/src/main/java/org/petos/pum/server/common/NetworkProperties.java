package org.petos.pum.server.common;

import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author Paval Shlyk
 * @since 15/08/2023
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Component
@ConfigurationProperties(prefix = "networks.connect")
public class NetworkProperties {
	@Getter @Setter
	private int port;
	@Getter @Setter
	private int timeout;
	@Getter @Setter
	private boolean single;

}
