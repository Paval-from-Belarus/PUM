package org.petos.pum.server.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import transfer.BinaryObjectMapper;
import transfer.BinaryObjectProperties;

import java.net.URISyntaxException;
import java.net.URL;

/**
 * @author Paval Shlyk
 * @since 16/08/2023
 */
@Component
@ConfigurationProperties(prefix = "network.serialization")
public class SerializationProperties {
@Autowired
public SerializationProperties(String path) {
      ClassLoader loader = SerializationProperties.class.getClassLoader();
      URL resource = loader.getResource(path);
      Assert.notNull(resource, String.format("Serialization properties' resource file is not found by %s", path));
      this.binaryMapper = new BinaryObjectMapper();
      BinaryObjectProperties properties = new BinaryObjectProperties(binaryMapper);
      try {
	    properties.configure(resource.toURI());
      } catch (Exception e) {
	    throw new IllegalStateException("Binary mapper cannot be configured: " + e.getMessage());
      }
}
public BinaryObjectMapper serializer() {
      return binaryMapper;
}
private final BinaryObjectMapper binaryMapper;
}
