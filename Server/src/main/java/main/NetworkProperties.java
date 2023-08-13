package main;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

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
