package org.petos.pum.server.dto;

import org.mapstruct.*;
import org.petos.pum.server.repositories.PackageHatDao;
import org.petos.pum.server.repositories.entities.PackageAlias;
import org.petos.pum.server.repositories.entities.PackageHat;
import org.petos.pum.server.repositories.entities.PackageType;
import org.petos.pum.networks.grpc.HeaderInfo;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * @author Paval Shlyk
 * @since 09/09/2023
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE, unmappedSourcePolicy = ReportingPolicy.WARN)
public interface PackageHatMapper {
@Mapping(target = "packageId", source = "hat.id")
@Mapping(target = "packageType", source = "hat.type")
HeaderInfo toHeaderInfo(PackageHat hat);
default String mapType(PackageType type) {
      return type.getName();
}
@AfterMapping
default void addAllAliases(@MappingTarget HeaderInfo.Builder builder, PackageHat hat) {
      builder.addAllAliases(hat.getAliases().stream().map(PackageAlias::getName).toList());
}
}
