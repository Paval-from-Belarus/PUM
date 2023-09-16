package org.petos.pum.server.dto;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.petos.pum.server.repositories.entities.ArchiveType;
import org.petos.pum.server.repositories.entities.PayloadInstance;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.AbstractMap;
import java.util.Map;

/**
 * @author Paval Shlyk
 * @since 17/09/2023
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PayloadInstanceMapper {
default Map.Entry<String, Long> toMapEntry(PayloadInstance instance) {
      String archive = instance.getArchiveType().getType();
      Long payloadSize = instance.getPayloadSize();
      return new AbstractMap.SimpleImmutableEntry<>(archive, payloadSize);
}
default OutputStream toStream(PayloadInstance instance) throws IOException {
      Path path = Path.of(instance.getPath());
      return Files.newOutputStream(path, StandardOpenOption.READ);
}
}
