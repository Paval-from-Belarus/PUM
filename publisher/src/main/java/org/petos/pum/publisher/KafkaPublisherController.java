package org.petos.pum.publisher;

import com.google.protobuf.DynamicMessage;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.petos.pum.networks.dto.credentials.AuthorizationInfo;
import org.petos.pum.networks.dto.credentials.RegistrationErrorInfo;
import org.petos.pum.networks.dto.credentials.ValidationInfo;
import org.petos.pum.networks.dto.transfer.PublisherInfo;
import org.petos.pum.networks.dto.transfer.PublisherRequest;
import org.petos.pum.networks.dto.transfer.ResponseStatus;
import org.petos.pum.publisher.service.PublisherService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Controller;

import java.util.Optional;


/**
 * @author Paval Shlyk
 * @since 01/11/2023
 */
@Controller
@RequiredArgsConstructor
public class KafkaPublisherController {
private final PublisherService publisherService;
private final KafkaTemplate<String, PublisherInfo> kafkaTemplate;

@KafkaListener(topics = "publisher-requests")
@SneakyThrows
public void doPublisher(DynamicMessage message) {
      PublisherRequest request = PublisherRequest.parseFrom(message.toByteArray());
      PublisherInfo.Builder builder = PublisherInfo.newBuilder();
      builder.setId(request.getId());
      if (request.hasRegistration()) {
	    Optional<RegistrationErrorInfo> errorInfo = publisherService.register(request.getRegistration());
	    if (errorInfo.isPresent()) {
		  builder.setRegistration(errorInfo.get());
		  sendPublisherInfo(builder, ResponseStatus.ILLEGAL);
	    } else {
		  sendPublisherInfo(builder, ResponseStatus.OK);
	    }
	    return;
      }
      if (request.hasAuthorization()) {
	    Optional<AuthorizationInfo> authorizationInfo = publisherService.authorize(request.getAuthorization());
	    if (authorizationInfo.isPresent()) {
		  builder.setAuthorization(authorizationInfo.get());
		  sendPublisherInfo(builder, ResponseStatus.OK);
	    } else {
		  sendPublisherInfo(builder, ResponseStatus.ILLEGAL);
	    }
	    return;
      }
      if (request.hasValidation()) {
	    ValidationInfo info = publisherService.validate(request.getValidation());
	    builder.setValidation(info);
	    if (info.hasError()) {
		  sendPublisherInfo(builder, ResponseStatus.ILLEGAL);
	    } else {
		  assert info.hasPublisherId();
		  sendPublisherInfo(builder, ResponseStatus.OK);
	    }
	    return;
      }
      sendPublisherInfo(builder, ResponseStatus.ILLEGAL); //in case when no
}

private void sendPublisherInfo(PublisherInfo.Builder builder, ResponseStatus status) {
      PublisherInfo response = builder
				   .setStatus(status)
				   .build();
      kafkaTemplate.send("publisher-info", response);
}
}
