package org.petos.pum.http.service.impl;

import com.google.protobuf.DynamicMessage;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.petos.pum.networks.dto.AuthorizationDto;
import org.petos.pum.networks.dto.RegistrationDto;
import org.petos.pum.http.service.PublisherInfoService;
import org.petos.pum.networks.dto.credentials.*;
import org.petos.pum.networks.dto.transfer.PackageInfo;
import org.petos.pum.networks.dto.transfer.PublisherInfo;
import org.petos.pum.networks.dto.transfer.PublisherRequest;
import org.petos.pum.networks.dto.transfer.ResponseStatus;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * @author Paval Shlyk
 * @since 01/12/2023
 */
@Service
@RequiredArgsConstructor
public class PublisherInfoServiceImpl extends AbstractReactiveKafkaService implements PublisherInfoService {
private static final Logger LOGGER = LogManager.getLogger(PackageInfoServiceImpl.class);
private final KafkaTemplate<String, PublisherRequest> requestTemplate;
@SneakyThrows
private PublisherInfo buildInfo(DynamicMessage message) {
      return PublisherInfo.parseFrom(message.toByteArray());
}
@KafkaListener(topics = "publisher-info", groupId = "first")
public void processKafkaSupplier(DynamicMessage message) {
      LOGGER.trace("Message {} consumed", message.toString());
      PublisherInfo info = buildInfo(message);
      UUID requestId = UUID.fromString(info.getId());
      if (info.hasRegistration()) {
	    consumeResponse(requestId, info.getRegistration(), info.getStatus());
      }
      if (info.hasAuthorization()) {
	    consumeResponse(requestId, info.getAuthorization(), info.getStatus());
      }
      if (info.hasValidation()) {
	    consumeResponse(requestId, info.getValidation(), info.getStatus());
      }

      consumeResponse(requestId, null, info.getStatus());
}
@Override
public Mono<ValidationInfo> validate(String token) {
      ValidationRequest info = ValidationRequest.newBuilder()
				   .setToken(token)
				   .build();
      UUID sessionId = nextUniqueSessionId();
      PublisherRequest request = PublisherRequest.newBuilder()
				     .setId(sessionId.toString())
				     .setValidation(info)
				     .build();
      sendRequest(request);
      return nextMonoResponse(sessionId);
}

@Override
public Mono<RegistrationErrorInfo> register(RegistrationDto dto) {
      RegistrationRequest info = RegistrationRequest.newBuilder()
				     .setName(dto.name())
				     .setPassword(dto.name())
				     .setEmail(dto.email())
				     .build();
      UUID sessionId = nextUniqueSessionId();
      PublisherRequest request = PublisherRequest.newBuilder()
				     .setId(sessionId.toString())
				     .setRegistration(info)
				     .build();
      sendRequest(request);
      return nextMonoResponse(sessionId);
}

@Override
public Mono<AuthorizationInfo> authorize(AuthorizationDto dto) {
      AuthorizationRequest info = AuthorizationRequest.newBuilder()
				      .setName(dto.name())
				      .setPassword(dto.password())
				      .build();
      UUID sessionId = nextUniqueSessionId();
      PublisherRequest request = PublisherRequest.newBuilder()
				     .setId(sessionId.toString())
				     .setAuthorization(info)
				     .build();
      sendRequest(request);
      return nextMonoResponse(sessionId);
}

private void sendRequest(PublisherRequest request) {
      requestTemplate.send("publisher-requests", request);
}
}
