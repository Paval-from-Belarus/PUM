package org.petos.pum.http.service.impl;

import com.google.protobuf.DynamicMessage;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.petos.pum.networks.dto.DependencyDto;
import org.petos.pum.networks.dto.PublicationDto;
import org.petos.pum.http.service.PackageInfoService;
import org.petos.pum.networks.dto.packages.*;
import org.petos.pum.networks.dto.transfer.PackageInfo;
import org.petos.pum.networks.dto.transfer.PackageRequest;
import org.petos.pum.networks.dto.transfer.ResponseStatus;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * @author Paval Shlyk
 * @since 14/11/2023
 */
@Service
@RequiredArgsConstructor
public class PackageInfoServiceImpl extends AbstractReactiveKafkaService implements PackageInfoService {
private static final Logger LOGGER = LogManager.getLogger(PackageInfoServiceImpl.class);
private final KafkaTemplate<String, PackageRequest> requestTemplate;


@SneakyThrows
private PackageInfo buildInfo(DynamicMessage message) {
      return PackageInfo.parseFrom(message.toByteArray());
}

@KafkaListener(topics = "package-info", groupId = "first")
public void processKafkaSupplier(DynamicMessage message) {
      LOGGER.trace("Message {} consumed", message.toString());
      PackageInfo info = buildInfo(message);
      UUID requestId = UUID.fromString(info.getId());
      if (info.hasHeader()) {
	    consumeResponse(requestId, info.getHeader(), info.getStatus());
	    return;
      }
      if (info.hasShort()) {
	    consumeResponse(requestId, info.getShort(), info.getStatus());
	    return;
      }
      if (info.hasFull()) {
	    consumeResponse(requestId, info.getFull(), info.getStatus());
	    return;
      }
      if (info.hasEndpoint()) {
	    consumeResponse(requestId, info.getEndpoint(), info.getStatus());
	    return;
      }
      assert info.getStatus().equals(ResponseStatus.NOT_FOUND);//otherwise, I don't know what is it...
      consumeResponse(requestId, null, info.getStatus());
}

private void sendRequest(PackageRequest request) {
      requestTemplate.send("package-requests", request);
}


@Override
public Mono<HeaderInfo> getHeaderInfo(String packageName) {
      HeaderRequest header = HeaderRequest.newBuilder()
				 .setPackageAlias(packageName)
				 .build();
      UUID sessionId = nextUniqueSessionId();
      PackageRequest request = PackageRequest.newBuilder()
				   .setId(sessionId.toString())
				   .setHeader(header)
				   .build();
      sendRequest(request);
      return nextMonoResponse(sessionId);
}

@Override
public Flux<ShortInstanceInfo> getAllInstanceInfo(int packageId) {
      InstanceRequest instance = InstanceRequest.newBuilder()
				     .setPackageId(packageId)
				     .setType(InstanceInfoType.SHORT)
				     .build();
      UUID sessionId = nextUniqueSessionId();
      PackageRequest request = PackageRequest.newBuilder()
				   .setId(sessionId.toString())
				   .setInstance(instance)
				   .build();
      sendRequest(request);
      return nextFluxResponse(sessionId);
}

@Override
public Mono<FullInstanceInfo> getFullInfo(int packageId, String version) {
      InstanceRequest instance = InstanceRequest.newBuilder()
				     .setPackageId(packageId)
				     .setVersion(version)
				     .setType(InstanceInfoType.FULL)
				     .build();
      UUID sessionId = nextUniqueSessionId();
      PackageRequest request = PackageRequest.newBuilder()
				   .setId(sessionId.toString())
				   .setInstance(instance)
				   .build();
      sendRequest(request);
      return nextMonoResponse(sessionId);
}

@Override
public Mono<EndpointInfo> getPayloadInfo(int packageId, String version, String archive) {
      PayloadArchiveType archiveType = PayloadArchiveType.valueOf(archive.toUpperCase());
      PayloadRequest payload = PayloadRequest.newBuilder()
				   .setPackageId(packageId)
				   .setVersion(version)
				   .setType(archiveType)
				   .build();
      UUID sessionId = nextUniqueSessionId();
      PackageRequest request = PackageRequest.newBuilder()
				   .setId(sessionId.toString())
				   .setPayload(payload)
				   .build();
      sendRequest(request);
      return nextMonoResponse(sessionId);
}

@Override
public Mono<EndpointInfo> publish(long publisherId, PublicationDto dto) {
      List<ShortInstanceInfo> dependencies = collectDependencies(dto);
      List<String> aliases = collectAliases(dto);
      PublishingRequest.Builder publishing = PublishingRequest.newBuilder()
						 .setPackageName(dto.packageName())
						 .setPackageType(dto.packageType())
						 .setVersion(dto.version())
						 .setLicense(dto.license())
						 .setPublisherId(publisherId)
						 .addAllAliases(aliases)
						 .addAllDependencies(dependencies);
      UUID sessionId = nextUniqueSessionId();
      PackageRequest request = PackageRequest.newBuilder()
				   .setId(sessionId.toString())
				   .setPublishing(publishing)
				   .build();
      sendRequest(request);
      return nextMonoResponse(sessionId);
}

private List<String> collectAliases(PublicationDto dto) {
      List<String> aliases = dto.aliases();
      if (aliases == null) {
	    aliases = List.of();
      }
      return aliases;
}

private List<ShortInstanceInfo> collectDependencies(PublicationDto dto) {
      List<DependencyDto> dependencies = dto.dependencies();
      if (dependencies == null) {
	    return List.of();
      }
      return dependencies.stream()
		 .map(dependency -> ShortInstanceInfo.newBuilder()
					.setPackageId(dependency.packageId())
					.setVersion(dependency.version())
					.build())
		 .toList();
}
}
