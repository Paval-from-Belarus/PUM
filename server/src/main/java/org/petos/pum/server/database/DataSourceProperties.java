package org.petos.pum.server.database;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author Paval Shlyk
 * @since 19/08/2023
 */
@Component
@ConfigurationProperties(prefix="storage.data-source")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DataSourceProperties {
@Getter @Setter
private String driverClass;
@Getter @Setter
private String url;
@Getter @Setter
private  String username;
@Getter @Setter
private  String password;
}
