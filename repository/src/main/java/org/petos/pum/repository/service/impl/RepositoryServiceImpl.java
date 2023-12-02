package org.petos.pum.repository.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import org.springframework.stereotype.Service;
import scala.reflect.ClassTag;
import scala.util.Either;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
      this.self = self;
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
	    PackageInfo packageInfo;
	    packageInfo = optionalPackageInfo.orElseGet(() -> publishNewPackage(request));
	    Optional<PackageInstance> optionalInstance = packageInstanceRepository.findByPackageInfoIdAndVersion(packageInfo.getId(), request.getVersion());
	    if (optionalInstance.isPresent() && !optionalInstance.get().getArchives().isEmpty()) {
		  return Optional.empty();
	    }
	    if (optionalInstance.isEmpty()) {
		  PackageInstance instance = PackageInstance.builder()
						 .packageInfo(packageInfo)
						 .version(request.getVersion())
						 .dependencies(collectDependencies(request.getDependenciesList()))
						 .build();
		  packageInstanceRepository.save(instance);
	    }
	    info = buildEndpointInfo(new PublicationSecret(packageInfo.getId(), request.getVersion()));
      } catch (Exception e) {
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
public void download(String token, OutputStream output) {
      String message;
      try (output) {
	    Optional<DownloadSecret> optionalSecret = decodeSecret(token, DownloadSecret.class);
	    if (optionalSecret.isEmpty()) {
		  throw newUserAccessViolationException("Failed to share file with Invalid token:%s", token);
	    }
	    DownloadSecret secret = optionalSecret.get();
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
public void upload(String token, InputStream input) {
      Path targetPath = null;
      String message;
      try (input) {
	    Optional<PublicationSecret> optionalSecret = decodeSecret(token, PublicationSecret.class);
	    if (optionalSecret.isEmpty()) {
		  throw newUserAccessViolationException("Failed upload with invalid token:%s", token);
	    }
	    PublicationSecret secret = optionalSecret.get();
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
	    LOGGER.warn(message);
//	    throw new IllegalStateException(message);
      }
      LOGGER.trace(message);
}

@Transactional
public void assignPackageAsAvailable(long packageId, String version, Path targetPath) {
      File targetFile = targetPath.toFile();
      PackageInstance instance = packageInstanceRepository.findByPackageInfoIdAndVersion(packageId, version).orElseThrow();
      ArchiveType archiveType = archiveTypeRepository.findById((short) 2).orElseThrow();//brotli algo
      PackageArchiveId id = PackageArchiveId.builder()
				   .instanceId(instance.getId())
				   .archiveTypeId(archiveType.getId())
				   .build();
      PackageInstanceArchive archive = PackageInstanceArchive.builder()
					   .id(id)
					   .payloadPath(targetPath.toAbsolutePath().toString())
					   .payloadSize(targetFile.length())
					   .build();
      packageInstanceArchiveRepository.save(archive);
      packageInfoRepository.setStatusById(packageId, PackageInfo.VALID);
}

private static String encodeSecret(@NotNull Object message) {
      byte[] bytes = Json.encodeAsBytes(message);
      return Base64.getEncoder().encodeToString(bytes);
}

private static <T> Optional<T> decodeSecret(String secret, Class<T> clazz) {
      byte[] bytes = Base64.getDecoder().decode(secret);
      Either<JsonProcessingException, Object> either = Json.parseBytesAs(bytes, ClassTag.apply(clazz));
      if (either.isLeft()) {
	    return Optional.empty();
      } else {
	    return Optional.of(either.getOrElse(() -> null));
      }
}

private EndpointInfo buildEndpointInfo(@NotNull Object message) {
      String address = String.format("%s:%d", this.address, this.port);
      String secret = encodeSecret(message);
      return EndpointInfo.newBuilder()
		 .setRepositoryIp(address)
		 .setSecret(secret)
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
