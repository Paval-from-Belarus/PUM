package org.petos.pum.repository.service.impl;

import com.google.protobuf.ByteString;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.avro.data.Json;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.petos.pum.networks.dto.packages.*;
import org.petos.pum.repository.dao.*;
import org.petos.pum.repository.model.*;
import org.petos.pum.repository.service.RepositoryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Paval Shlyk
 * @since 14/11/2023
 */
@Service
@RequiredArgsConstructor
public class RepositoryServiceImpl implements RepositoryService {
private static final Logger LOGGER = LogManager.getLogger(RepositoryServiceImpl.class);
private final PackageInfoRepository packageInfoRepository;
private final PackageAliasRepository packageAliasRepository;
private final PackageInstanceRepository packageInstanceRepository;
private final PackageDependencyRepository packageDependencyRepository;
private final PackageInstanceArchiveRepository packageInstanceArchiveRepository;
private final String serverAddress = "the default address to start file downloading)";
private final LicenseRepository licenseRepository;
private final PackageTypeRepository packageTypeRepository;
@Setter
@Value("server.port")
private Integer port;
@Setter
@Value("server.address")
private String address;

@Override
public Optional<HeaderInfo> getHeaderInfo(HeaderRequest request) {
      Optional<PackageAlias> optionalAlias = packageAliasRepository.findByName(request.getPackageAlias());
      return optionalAlias
		 .flatMap(alias -> packageInfoRepository
				       .findWithAliasesByIdAndStatus(alias.getPackageInfo().getId(), PackageInfo.VALID))
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
	  packageInstanceRepository.findByPackageInfoIdAndVersion(packageId, version);
      return optionalInstance.stream()
		 .map(instance -> {
		       List<ShortInstanceInfo> dependencies = packageDependencyRepository.findByTargetId(instance.getId()).stream()
								  .map(PackageDependency::getDependency)
								  .map(this::toShortInfo)
								  .toList();
		       Map<String, Long> archives = packageInstanceArchiveRepository.findByInstanceId(instance.getId()).stream()
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
public Optional<EndpointInfo> getPayloadInfo(PayloadRequest request) {
      int packageId = request.getPackageId();
      String version = request.getVersion();
      short archiveTypeId = (short) request.getType().getNumber();
      Optional<PackageInstance> optionalInstance = packageInstanceRepository.findByPackageInfoIdAndVersion(packageId, version);
      return optionalInstance.flatMap(instance -> {
	    Optional<PackageInstanceArchive> optionalPayload = packageInstanceArchiveRepository.findByInstanceIdAndArchiveTypeId(instance.getId(), archiveTypeId);
	    return optionalPayload.map(payload -> buildEndpointInfo(new DownloadSecret(payload.getPayloadPath())));
      });
}

@Override
@Transactional
public Optional<EndpointInfo> publish(PublishingRequest request) {
      EndpointInfo info = null;
      try {
	    Optional<PackageInfo> optionalPackageInfo = packageInfoRepository.findByPublisherIdAndName(request.getPublisherId(), request.getPackageName());
	    PackageInfo packageInfo = optionalPackageInfo.orElse(publishNewPackage(request));
	    PackageInstance instance = PackageInstance.builder()
					   .packageInfo(packageInfo)
					   .version(request.getVersion())
					   .dependencies(collectDependencies(request.getDependenciesList()))
					   .build();
	    packageInstanceRepository.save(instance);
	    info = buildEndpointInfo(new PublicationSecret(packageInfo.getId(), instance.getVersion()));
      } catch (DataAccessException e) {
	    LOGGER.error(e);
      }
      return Optional.ofNullable(info);
}

@Override
public OutputStream download(byte[] secret) {
      return null;
}

@Override
public void upload(byte[] secret, InputStream input) {

}

private EndpointInfo buildEndpointInfo(@NotNull Object message) {
      String address = String.format("%s:%d", this.address, this.port);
      String secret = Json.toString(message);
      return EndpointInfo.newBuilder()
		 .setRepositoryIp(address)
		 .setSecret(ByteString.copyFrom(secret.getBytes()))
		 .build();
}

private PackageInfo publishNewPackage(PublishingRequest request) throws DataAccessException {
      List<PackageAlias> aliases = request.getAliasesList().stream()
				       .map(alias -> PackageAlias.builder()
							 .name(alias).build())
				       .toList();
      aliases = packageAliasRepository.saveAll(aliases);
      License license = licenseRepository.getByName(request.getLicense());
      PackageType type = packageTypeRepository.getByName(request.getPackageType());
      PackageInfo info = PackageInfo.builder()
			     .name(request.getPackageName())
			     .type(type)
			     .license(license)
			     .status(PackageInfo.NO_INSTANCE)
			     .publisherId(request.getPublisherId())
			     .aliases(aliases)
			     .build();
      return packageInfoRepository.save(info);
}

private List<PackageDependency> collectDependencies(List<ShortInstanceInfo> dependencies) throws DataAccessException {
      List<PackageDependency> list = new ArrayList<>();
      for (ShortInstanceInfo shortInfo : dependencies) {
	    //else throws DataAccessException
	    PackageInstance instance = packageInstanceRepository.getByPackageInfoIdAndVersion(shortInfo.getPackageId(), shortInfo.getVersion());
	    PackageDependency dependency = PackageDependency.builder()
					       .dependencyId(instance.getId())
					       .build();
	    list.add(dependency);
      }
      return list;
}

private record PublicationSecret(long packageId, String version) {
}

private record DownloadSecret(String payloadPath) {
}

private ShortInstanceInfo toShortInfo(PackageInstance instance) {
      return ShortInstanceInfo.newBuilder()
		 .setPackageId((int) instance.getPackageInfo().getId())
		 .setVersion(instance.getVersion())
		 .build();
}

}
