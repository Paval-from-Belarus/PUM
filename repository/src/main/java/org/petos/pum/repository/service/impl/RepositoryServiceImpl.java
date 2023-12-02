package org.petos.pum.repository.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.protobuf.ByteString;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import kafka.utils.Json;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.petos.pum.networks.dto.packages.*;
import org.petos.pum.networks.transfer.PackageAssembly;
import org.petos.pum.repository.dao.*;
import org.petos.pum.repository.exception.UserAccessViolationException;
import org.petos.pum.repository.model.*;
import org.petos.pum.repository.service.RepositoryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import scala.reflect.ClassTag;
import scala.util.Either;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
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
private final LicenseRepository licenseRepository;
private final PackageTypeRepository packageTypeRepository;
private RepositoryServiceImpl self;
@Setter
@Value("${server.port}")
private Integer port;
@Setter
@Value("${server.address}")
private String address;
@Setter
@Value("${storage.root.path}")
private Path storageRoot;
private final ArchiveTypeRepository archiveTypeRepository;
public void setSelfInjection(RepositoryServiceImpl self) {
      this.self	= self;
}

@PostConstruct
public void init() {
      System.out.println("The self bean is " + self);
}
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

private UserAccessViolationException newUserAccessViolationException(String params, Object... args) {
      String message = String.format(params, args);
      LOGGER.warn(message);
      return new UserAccessViolationException(message);
}

@Override
@Async("fileTaskExecutor")
public void download(byte[] token, OutputStream output) {
      Either<JsonProcessingException, DownloadSecret> either = Json.parseBytesAs(token, ClassTag.apply(DownloadSecret.class));
      String message;
      try {
	    if (either.isLeft()) {
		  output.close();
		  throw newUserAccessViolationException("Failed to share file with Invalid token:%s", Arrays.toString(token));
	    }
	    DownloadSecret secret = either.getOrElse(() -> null);
	    assert secret != null;
	    Path targetPath = storageRoot.resolve(secret.payloadPath());
	    try (InputStream input = PackageAssembly.decompress(Files.newInputStream(targetPath, StandardOpenOption.READ))) {
		  input.transferTo(output);
	    }
	    message = "File is fully sent";
      } catch (IOException e) {
	    LOGGER.warn("Failed to download file by client: {}", e.toString());
	    message = "Failed to sent file";
      }
      LOGGER.trace(message);
}


@Override
@Async("fileTaskExecutor")
public void upload(byte[] token, InputStream input) {
      Either<JsonProcessingException, PublicationSecret> either = Json.parseBytesAs(token, ClassTag.apply(PublicationSecret.class));
      Path targetPath = null;
      String message;
      try (input) {
	    if (either.isLeft()) {
		  input.close();
		  throw newUserAccessViolationException("Failed upload with invalid token:%s", Arrays.toString(token));
	    }
	    PublicationSecret secret = either.getOrElse(() -> null);
	    assert secret != null;
	    PackageInfo packageInfo = packageInfoRepository.findById(secret.packageId()).orElseThrow();
	    String uniqueFileName = packageInfo.getName() + secret.version();
	    targetPath = storageRoot.resolve(uniqueFileName);
	    try (OutputStream output = PackageAssembly.compress(Files.newOutputStream(targetPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE))) {
		  input.transferTo(output);
	    }
	    self.assignPackageAsAvailable(packageInfo.getId(), secret.version(), targetPath);
	    message = "File successfully stored";
      } catch (Throwable e) {
	    LOGGER.error(e);
	    try {
		  if (targetPath != null) {
			Files.deleteIfExists(targetPath);
		  }
	    } catch (IOException e2) {
		  LOGGER.error("Double fault while file uploading: {}", e2.toString());
		  //we can do nothing
	    }
	    message = "Failed to upload file";
      }
      LOGGER.trace(message);
}

@Transactional
public void assignPackageAsAvailable(long packageId, String version, Path targetPath) {
      File targetFile = targetPath.toFile();
      PackageInstance instance = packageInstanceRepository.findByPackageInfoIdAndVersion(packageId, version).orElseThrow();
      PackageInstanceArchive archive = PackageInstanceArchive.builder()
					   .instance(instance)
					   .payloadPath(targetPath.toAbsolutePath().toString())
					   .payloadSize(targetFile.length())
					   .archiveType(archiveTypeRepository.getReferenceById((short) 2))//the brotli algo
					   .build();
      instance.getArchives().add(archive);
      packageInfoRepository.setStatusById(packageId, PackageInfo.VALID);
}

private EndpointInfo buildEndpointInfo(@NotNull Object message) {
      String address = String.format("%s:%d", this.address, this.port);
      byte[] secret = Json.encodeAsBytes(message);
      return EndpointInfo.newBuilder()
		 .setRepositoryIp(address)
		 .setSecret(ByteString.copyFrom(secret))
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
