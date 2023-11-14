package org.petos.pum.repository.service;

import com.google.protobuf.DynamicMessage;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.petos.pum.networks.dto.packages.HeaderInfo;
import org.petos.pum.networks.dto.transfer.PackageInfo;
import org.petos.pum.networks.dto.transfer.PackageRequest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * @author Paval Shlyk
 * @since 14/11/2023
 */
@Service
@RequiredArgsConstructor
public class RepositoryService {
private final KafkaTemplate<String, PackageInfo> kafkaTemplate;

@KafkaListener(topics = "package-requests", groupId = "first")
@SneakyThrows
public void doNothing(DynamicMessage message) {
      PackageRequest request = PackageRequest.parseFrom(message.toByteArray());
      HeaderInfo.Builder header = HeaderInfo.newBuilder()
				      .addAliases("zero field")
				      .addAliases("first field")
				      .setPackageId(42)
				      .setPackageType("Any type");
      PackageInfo packageInfo = PackageInfo.newBuilder()
				    .setId(request.getId())
				    .setHeader(header)
				    .build();
      kafkaTemplate.send("package-info", packageInfo);
}
}
