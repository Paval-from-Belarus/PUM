package org.petos.pum.server.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import requests.*;
import transfer.BinaryObjectMapper;
import transfer.BinaryObjectProperties;
import transfer.NetworkExchange;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static transfer.NetworkExchange.*;

/**
 * @author Paval Shlyk
 * @since 16/08/2023
 */
@Component
public class SerializationProperties {
private static final String CONFIG_PATH = "transfer-packages.xml";
public SerializationProperties() {
      ClassLoader loader = SerializationProperties.class.getClassLoader();
      URL resource = loader.getResource(CONFIG_PATH);
      Assert.notNull(resource, String.format("Serialization properties' resource file is not found by %s", CONFIG_PATH));
      this.requestMap = new HashMap<>();
      requestMap.putAll(Map.of(
          RequestType.GetId, IdRequest.class,
          RequestType.GetInfo, InfoRequest.class,
          RequestType.GetVersion, VersionRequest.class,
          RequestType.GetPayload, PayloadRequest.class,
          RequestType.Authorize, security.Author.class,
//          RequestType.GetAll, Object.class //GetInfo has not attached dto, as GetRepo
          RequestType.PublishInfo, PublishInfoRequest.class,
          RequestType.PublishPayload, PublishInstanceRequest.class
      ));
      this.binaryMapper = new BinaryObjectMapper();
      BinaryObjectProperties properties = new BinaryObjectProperties(binaryMapper);
      try {
	    properties.configure(resource.toURI());
      } catch (Exception e) {
	    throw new IllegalStateException("Binary mapper cannot be configured: " + e.getMessage());
      }
}

public BinaryObjectMapper mapper() {
      return binaryMapper;
}
public Map<RequestType, Class<?>> requestMap() {
      return Collections.unmodifiableMap(requestMap);
}
private final BinaryObjectMapper binaryMapper;
private final Map<RequestType, Class<?>> requestMap;
}
