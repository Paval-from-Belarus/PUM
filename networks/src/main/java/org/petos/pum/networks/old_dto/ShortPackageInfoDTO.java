package org.petos.pum.networks.old_dto;

import lombok.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.petos.pum.networks.transfer.TransferEntity;
import org.petos.pum.networks.transfer.TransferOrder;

@EqualsAndHashCode
@TransferEntity(ignoreNullable = true, code = 16)
public class ShortPackageInfoDTO {
public ShortPackageInfoDTO(@NotNull String name, int id, @NotNull String version) {
      this.name = name;
      this.packageId = id;
      this.version = version;
}
@NotNull
@Getter
@TransferOrder(value = 0)
private final Integer packageId;
@NotNull
@Getter
@TransferOrder(value = 1)
private final String name;
@NotNull
@Getter
@TransferOrder(value = 2)
private final String version;
@Nullable
@Getter @Setter
@TransferOrder(value = 3)
private String[] aliases;
public static ShortPackageInfoDTO mapDTO(FullPackageInfoDTO fullInfo, Integer id) {
      return new ShortPackageInfoDTO(fullInfo.name, id, fullInfo.version);
}
}