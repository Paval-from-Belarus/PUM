package org.petos.pum.server.dto;

import lombok.NoArgsConstructor;
import lombok.Setter;
import org.mapstruct.*;
import org.petos.pum.networks.grpc.FullInstanceInfo;
import org.petos.pum.networks.grpc.ShortInstanceInfo;
import org.petos.pum.server.repositories.entities.DependencyId;
import org.petos.pum.server.repositories.entities.Licence;
import org.petos.pum.server.repositories.entities.PackageInfo;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Paval Shlyk
 * @since 16/09/2023
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    unmappedSourcePolicy = ReportingPolicy.WARN)
public abstract class PackageInfoMapper {
private PayloadInstanceMapper instanceMapper;

@Autowired
public void setInstanceMapper(PayloadInstanceMapper instanceMapper) {
      this.instanceMapper = instanceMapper;
}

public abstract ShortInstanceInfo toShortInfo(PackageInfo info);

public abstract ShortInstanceInfo toShortInfo(DependencyId dependencyId);

@Mapping(target = "licence", source = "info.licence")
public abstract FullInstanceInfo toFullInfo(PackageInfo info);

protected String mapLicense(Licence licence) {
      return licence.getName();
}

@AfterMapping
protected void addAllDependencies(@MappingTarget FullInstanceInfo.Builder builder, PackageInfo info) {
      builder.addAllDependencies(info.getDependencies().stream().map(this::toShortInfo).toList());
}

@AfterMapping
protected void addAllPayloads(@MappingTarget FullInstanceInfo.Builder builder, PackageInfo info) {
      info.getPayloads().stream()
	  .map(instanceMapper::toMapEntry)
	  .forEach(entry -> builder.putArchives(entry.getKey(), entry.getValue()));
}
}
