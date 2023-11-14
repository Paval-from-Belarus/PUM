package org.petos.pum.http.service.impl;

import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Message;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.petos.pum.http.service.PackageInfoService;
import org.petos.pum.networks.dto.packages.*;
import org.petos.pum.networks.dto.transfer.PackageInfo;
import org.petos.pum.networks.dto.transfer.PackageRequest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
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
private final Map<UUID, Consumer<? super Message>> packageInfoMap = new ConcurrentHashMap<>();


@SneakyThrows
private PackageInfo buildInfo(DynamicMessage message) {
      return PackageInfo.parseFrom(message.toByteArray());
}

private UUID nextUniqueId() {
      UUID uuid;
      do {
	    uuid = UUID.randomUUID();
      } while ((packageInfoMap.putIfAbsent(uuid, (value) -> {})) != null);
      return uuid;
}

private <T extends Message> void consumePackageInfo(UUID id, T info) {
      Consumer<? super Message> consumer = packageInfoMap.get(id);
      if (consumer == null) {
	    final String msg = String.format("Attempt to fetch non existing consumer by id=%s", id.toString());
	    LOGGER.warn(msg);
	    return;
      }
      consumer.accept(info);
}

@KafkaListener(topics = "package-info", groupId = "first")
public void processKafkaSupplier(DynamicMessage message) {
      LOGGER.trace("Message {} consumed", message.toString());
      PackageInfo info = buildInfo(message);
      UUID requestId = UUID.fromString(info.getId());
      if (info.hasHeader()) {
	    consumePackageInfo(requestId, info.getHeader());
	    return;
      }
      if (info.hasShort()) {
	    consumePackageInfo(requestId, info.getShort());
	    return;
      }
      if (info.hasFull()) {
	    consumePackageInfo(requestId, info.getFull());
	    return;
      }
      if (info.hasPayload()) {
	    consumePackageInfo(requestId, info.getPayload());
      }
}

@Override
public Mono<HeaderInfo> getHeaderInfo(String packageName) {
      HeaderRequest header = HeaderRequest.newBuilder()
				 .setPackageAlias("package_name")
				 .build();
      UUID sessionId = nextUniqueId();
      PackageRequest request = PackageRequest.newBuilder()
				   .setId(sessionId.toString())
				   .setHeader(header)
				   .build();
      requestTemplate.send("package-requests", request);
      return Mono
		 .<HeaderInfo>create(sink -> {
		       packageInfoMap.put(sessionId, (info) -> {
			     sink.success((HeaderInfo) info);
		       });
		 })
		 .timeout(Duration.ofMillis(1000), Mono.empty());
}

@Override
public Flux<ShortInstanceInfo> getAllInstanceInfo(int packageId) {
      return null;
}

@Override
public Mono<FullInstanceInfo> getFullInfo(int packageId, String version) {
      return null;
}

@Override
public Mono<PayloadInfo> getPayloadInfo(int packageId, String version) {
      return null;
}
}
