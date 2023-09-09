package org.petos.pum.server.dto;

import org.mapstruct.*;
import org.petos.pum.server.repositories.entities.PackageAlias;
import org.petos.pum.server.repositories.entities.PackageHat;
import org.petos.pum.server.repositories.entities.PackageType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * @author Paval Shlyk
 * @since 09/09/2023
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)//this annotation allows to inject fields via spring
public interface PackageHatMapper {
@Mapping(target = "domainName", source = "hat.name")
ShortPackageInfo toShortInfo(PackageHat hat);

default String mapType(PackageType type) {
      return type.getName();
}

default String[] mapAliases(List<PackageAlias> aliases) {
      return aliases.stream()
		 .map(PackageAlias::getAlias)
		 .toArray(String[]::new);
}

default List<PackageAlias> unmapAliases(String[] aliases) {
      var set = new HashSet<>(Arrays.asList(aliases));
      return set.stream()
		 .map(alias -> PackageAlias.builder().alias(alias).build())
		 .toList();
}
}
