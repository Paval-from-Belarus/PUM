package org.petos.pum.repository.controller;

import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.petos.pum.networks.dto.packages.*;
import org.petos.pum.networks.dto.transfer.PackageInfo;
import org.petos.pum.networks.dto.transfer.PackageRequest;
import org.petos.pum.networks.dto.transfer.ResponseStatus;
import org.petos.pum.repository.service.RepositoryService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.List;
import java.util.Optional;

/**
 * @author Paval Shlyk
 * @since 15/11/2023
 */
@Controller
@RequiredArgsConstructor
public class KafkaRepositoryController {
private static final Logger LOGGER = LogManager.getLogger(KafkaRepositoryController.class);
private final KafkaTemplate<String, PackageInfo> kafkaTemplate;
private final RepositoryService repositoryService;

private void setInstanceInfo(PackageInfo.Builder originBuilder, PackageRequest request) {
      assert request.hasInstance();
      InstanceRequest instance = request.getInstance();
      if (instance.getType().equals(InstanceInfoType.SHORT)) {
	    List<ShortInstanceInfo> shortInfoList = repositoryService.getShortInfo(instance);
	    if (shortInfoList.isEmpty()) {
		  sendPackageInfo(originBuilder, ResponseStatus.NOT_FOUND);
		  return;
	    }
	    int restCount = shortInfoList.size();
	    for (ShortInstanceInfo shortInfo : shortInfoList) {
		  PackageInfo.Builder cloned = originBuilder.clone();
		  cloned.setShort(shortInfo);
		  restCount -= 1;
		  if (restCount == 0) {
			sendPackageInfo(cloned, ResponseStatus.LAST);
		  } else {
			sendPackageInfo(cloned, ResponseStatus.OK);
		  }
	    }
	    return;
      }
      if (instance.getType().equals(InstanceInfoType.FULL)) {
	    List<FullInstanceInfo> fullInfoList = repositoryService.getFullInfo(instance);
	    if (fullInfoList.isEmpty()) {
		  sendPackageInfo(originBuilder, ResponseStatus.NOT_FOUND);
		  return;
	    }
	    int restCount = fullInfoList.size();
	    for (FullInstanceInfo fullInfo : fullInfoList) {
		  PackageInfo.Builder cloned = originBuilder.clone();
		  cloned.setFull(fullInfo);
		  restCount -= 1;
		  if (restCount == 0) {
			sendPackageInfo(cloned, ResponseStatus.LAST);
		  } else {
			sendPackageInfo(cloned, ResponseStatus.OK);
		  }
	    }
      } else {
	    sendPackageInfo(originBuilder, ResponseStatus.ILLEGAL);
      }
}

private PackageRequest parseRequest(DynamicMessage message) {
      PackageRequest request;
      try {
	    request = PackageRequest.parseFrom(message.toByteArray());
      } catch (InvalidProtocolBufferException e) {
	    LOGGER.error(e);
	    throw new IllegalStateException(e);
      }
      return request;
}

@KafkaListener(topics = "package-requests")
public void doNothing(DynamicMessage message) {
      LOGGER.trace("Message is accepted: {}", message);
      PackageRequest request = parseRequest(message);
      PackageInfo.Builder builder = PackageInfo.newBuilder();
      builder.setId(request.getId());
      if (request.hasHeader()) {
	    Optional<HeaderInfo> headerInfo = repositoryService.getHeaderInfo(request.getHeader());
	    if (headerInfo.isPresent()) {
		  builder.setHeader(headerInfo.get());
		  sendPackageInfo(builder, ResponseStatus.OK);
	    } else {
		  sendPackageInfo(builder, ResponseStatus.NOT_FOUND);
	    }
	    return;
      }
      if (request.hasInstance()) {
	    setInstanceInfo(builder, request);
	    return;
      }
      if (request.hasPayload()) {
	    Optional<EndpointInfo> payloadInfo = repositoryService.getPayloadInfo(request.getPayload());
	    if (payloadInfo.isPresent()) {
		  builder.setEndpoint(payloadInfo.get());
		  sendPackageInfo(builder, ResponseStatus.OK);
	    } else {
		  sendPackageInfo(builder, ResponseStatus.NOT_FOUND);
	    }
	    return;
      }
      if (request.hasPublishing()) {
	    Optional<EndpointInfo> endpoint = repositoryService.publish(request.getPublishing());
	    if (endpoint.isPresent()) {
		  builder.setEndpoint(endpoint.get());
		  sendPackageInfo(builder, ResponseStatus.OK);
	    } else {
		  sendPackageInfo(builder, ResponseStatus.ILLEGAL);
	    }
	    return;
      }
      sendPackageInfo(builder, ResponseStatus.ILLEGAL);

}
//this endpoint process request from publisher

private void sendPackageInfo(PackageInfo.Builder builder, ResponseStatus status) {
      PackageInfo response = builder
				 .setStatus(status)
				 .build();
      kafkaTemplate.send("package-info", response);
}

@ExceptionHandler(Exception.class)
public void handleException(Exception exception) {
      LOGGER.error(exception);
      PackageInfo response = PackageInfo.newBuilder()
				 .setStatus(ResponseStatus.BUSY)
				 .build();
      kafkaTemplate.send("package-info", response);
}

}
