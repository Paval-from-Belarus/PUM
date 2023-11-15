package org.petos.pum.http.service.impl;

import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Message;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.petos.pum.http.exception.ResourceNotFoundException;
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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author Paval Shlyk
 * @since 14/11/2023
 */
@Service
@RequiredArgsConstructor
public class PackageInfoServiceImpl implements PackageInfoService {
private static final Logger LOGGER = LogManager.getLogger(PackageInfoServiceImpl.class);
private final KafkaTemplate<String, PackageRequest> requestTemplate;
private final Map<UUID, BiConsumer<? super Message, ResponseStatus>> packageInfoMap = new ConcurrentHashMap<>();


@SneakyThrows
private PackageInfo buildInfo(DynamicMessage message) {
      return PackageInfo.parseFrom(message.toByteArray());
}

private UUID nextUniqueSessionId() {
      UUID uuid;
      do {
	    uuid = UUID.randomUUID();
      } while ((packageInfoMap.putIfAbsent(uuid, (value, status) -> {
      })) != null);
      return uuid;
}

private <T extends Message> void consumePackageInfo(UUID id, @Nullable T info, ResponseStatus status) {
      BiConsumer<? super Message, ResponseStatus> consumer = packageInfoMap.get(id);
      if (consumer == null) {
	    final String msg = String.format("Attempt to fetch non existing consumer by id=%s", id.toString());
	    LOGGER.warn(msg);
	    return;
      }
      consumer.accept(info, status);
}

@KafkaListener(topics = "package-info", groupId = "first")
public void processKafkaSupplier(DynamicMessage message) {
      LOGGER.trace("Message {} consumed", message.toString());
      PackageInfo info = buildInfo(message);
      UUID requestId = UUID.fromString(info.getId());
      if (info.hasHeader()) {
	    consumePackageInfo(requestId, info.getHeader(), info.getStatus());
	    return;
      }
      if (info.hasShort()) {
	    consumePackageInfo(requestId, info.getShort(), info.getStatus());
	    return;
      }
      if (info.hasFull()) {
	    consumePackageInfo(requestId, info.getFull(), info.getStatus());
	    return;
      }
      if (info.hasPayload()) {
	    consumePackageInfo(requestId, info.getPayload(), info.getStatus());
	    return;
      }
      assert info.getStatus().equals(ResponseStatus.NOT_FOUND);//otherwise, I don't know what is it...
      consumePackageInfo(requestId, null, info.getStatus());
}

private void sendRequest(PackageRequest request) {
      requestTemplate.send("package-requests", request);
}

private <T> Mono<T> nextMonoResponse(UUID sessionId) {
      return Mono.<T>create(sink -> {
	    packageInfoMap.put(sessionId, (info, status) -> {
		  if (info == null || status == ResponseStatus.NOT_FOUND) {
			sink.error(newMonoNotFoundException(sessionId));
		  } else {
			LOGGER.trace("Mono is committed by sessionId {}", sessionId);
			sink.success((T) info);
		  }
		  packageInfoMap.remove(sessionId);
	    });
	    sink.onDispose(() -> {
		  packageInfoMap.remove(sessionId);
		  LOGGER.trace("Mono is canceled by sessionId {}", sessionId.toString());
	    });
      });
}

private ResourceNotFoundException newMonoNotFoundException(UUID sessionId) {
      final String msg = String.format("Failed to find mono for given session %s", sessionId.toString());
      LOGGER.trace(msg);
      return new ResourceNotFoundException(msg);

}

private <T> Flux<T> nextFluxResponse(UUID sessionId) {
      return Flux.create(sink -> {
	    packageInfoMap.put(sessionId, (info, status) -> {
		  if (info == null || status == ResponseStatus.NOT_FOUND) {
			sink.error(newMonoNotFoundException(sessionId));
			packageInfoMap.remove(sessionId);
		  } else {
			sink.next((T) info);
			LOGGER.trace("Flux is appended with sessionId {}", sessionId);
			if (status == ResponseStatus.LAST) {
			      sink.complete();
			}
		  }
	    });
	    sink.onDispose(() -> {
		  packageInfoMap.remove(sessionId);
		  LOGGER.trace("Flux is canceled by sessionId {}", sessionId);
	    });
      });
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
public Mono<PayloadInfo> getPayloadInfo(int packageId, String version, String archive) {
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
}
