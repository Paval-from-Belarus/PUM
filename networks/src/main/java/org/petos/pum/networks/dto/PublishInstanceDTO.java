package org.petos.pum.networks.dto;


import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.petos.pum.networks.transfer.TransferEntity;
import org.petos.pum.networks.transfer.TransferOrder;

@TransferEntity(ignoreNullable = true, code = 20)
@EqualsAndHashCode(callSuper = false)
public class PublishInstanceDTO {
public static final String DEFAULT_LICENCE = "GNU";
@Getter
@TransferOrder(value = 0)
private final int packageId;
@Getter
@NotNull
@TransferOrder(value = 1)
private final String version;
@Getter
@TransferOrder(value = 2)
private final int payloadSize;
@Setter @Getter
@NotNull
@TransferOrder(value = 3)
private String license;

@Setter @Getter
@Nullable
@TransferOrder(value = 4)
private DependencyInfoDTO[] dependencies;

/**
 * @param version      is unique String label for package Family
 * @param payloadSize size of payload in bytes
 */
public PublishInstanceDTO(int id, @NotNull String version, int payloadSize) {
      this.packageId = id;
      this.version = version;
      this.payloadSize = payloadSize;
      this.license = DEFAULT_LICENCE;
}

}
