package org.petos.pum.repository.service.impl;

import com.google.protobuf.ByteString;
import lombok.RequiredArgsConstructor;
import org.petos.pum.networks.dto.packages.*;
import org.petos.pum.repository.dao.PackageAliasRepository;
import org.petos.pum.repository.dao.PackageInfoRepository;
import org.petos.pum.repository.dao.PackageInstanceArchiveRepository;
import org.petos.pum.repository.dao.PackageInstanceRepository;
import org.petos.pum.repository.model.*;
import org.petos.pum.repository.service.RepositoryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Paval Shlyk
 * @since 14/11/2023
 */
@Service
@RequiredArgsConstructor
public class RepositoryServiceImpl implements RepositoryService {
private final PackageInfoRepository packageInfoRepository;
private final PackageAliasRepository packageAliasRepository;
private final PackageInstanceRepository packageInstanceRepository;
private final PackageInstanceArchiveRepository packageInstanceArchiveRepository;
private final String serverAddress = "the default address to start file downloading)";


@Override
public Optional<HeaderInfo> getHeaderInfo(HeaderRequest request) {
      Optional<PackageAlias> optionalAlias = packageAliasRepository.findByName(request.getPackageAlias());
      return optionalAlias
		 .flatMap(alias -> packageInfoRepository
				       .findWithAliasesById(alias.getPackageInfo().getId()))
		 .map(packageInfo -> {
		       List<String> aliases = packageInfo.getAliases().stream()
						  .map(PackageAlias::getName)
						  .toList();
		       return HeaderInfo.newBuilder()
				  .setPackageId((int) packageInfo.getId())
				  .setPackageType(packageInfo.getType().getName())
				  .setPackageName(packageInfo.getName())
				  .addAllAliases(aliases)
				  .build();
		 });
}

@Override
public List<ShortInstanceInfo> getShortInfo(InstanceRequest request) {
      assert request.getType().equals(InstanceInfoType.SHORT);
      int packageId = request.getPackageId();
      return packageInstanceRepository.findAllByPackageInfoId(packageId).stream()
		 .map(this::toShortInfo)
		 .collect(Collectors.toList());
}

@Override
public List<FullInstanceInfo> getFullInfo(InstanceRequest request) {
      assert request.getType().equals(InstanceInfoType.FULL);
      int packageId = request.getPackageId();
      String version = request.getVersion();
      Optional<PackageInstance> optionalInstance =
	  packageInstanceRepository.findWithDependenciesAndArchivesByPackageInfoIdAndVersion(packageId, version);
      return optionalInstance.stream()
		 .map(instance -> {
		       List<ShortInstanceInfo> dependencies = instance.getDependencies().stream()
								  .map(PackageDependency::getDependency)
								  .map(this::toShortInfo)
								  .toList();
		       Map<String, Long> archives = instance.getArchives().stream()
							.collect(Collectors.toMap(
							    archive -> archive.getArchiveType().getName(),
							    PackageInstanceArchive::getPayloadSize));
		       return FullInstanceInfo.newBuilder()
				  .setPackageId(packageId)
				  .setVersion(instance.getVersion())
				  .addAllDependencies(dependencies)
				  .putAllArchives(archives)
				  .build();
		 }).collect(Collectors.toList());
}

@Override
public Optional<PayloadInfo> getPayloadInfo(PayloadRequest request) {
      int packageId = request.getPackageId();
      String version = request.getVersion();
      short archiveTypeId = (short) request.getType().getNumber();
      Optional<PackageInstance> optionalInstance = packageInstanceRepository.findByPackageInfoIdAndVersion(packageId, version);
      return optionalInstance.flatMap(instance -> {
	    Optional<PackageInstanceArchive> optionalPayload = packageInstanceArchiveRepository.findByInstanceIdAndArchiveTypeId(instance.getId(), archiveTypeId);
	    return optionalPayload.map(payload -> {
		  byte[] secret = new byte[32];
		  ThreadLocalRandom.current().nextBytes(secret);
		  return PayloadInfo.newBuilder()
			     .setRepositoryIp(serverAddress)
			     .setSecret(ByteString.copyFrom(secret))
			     .build();
	    });
      });
}

private ShortInstanceInfo toShortInfo(PackageInstance instance) {
      return ShortInstanceInfo.newBuilder()
		 .setPackageId((int) instance.getPackageInfo().getId())
		 .setVersion(instance.getVersion())
		 .build();
}
}
