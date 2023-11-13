package org.petos.pum.networks.old_dto;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.petos.pum.networks.transfer.TransferEntity;
import org.petos.pum.networks.transfer.TransferOrder;

//generally, package info is supposed to transfer through network
//this class hasn't any real representation in database
//Client should only accept this entity
//This info soo verbose
@TransferEntity(code = 17)
@EqualsAndHashCode
public class FullPackageInfoDTO {
public FullPackageInfoDTO (@NotNull String name, @NotNull String version, int payloadSize) {
      this.name = name;
      this.version = version;
      this.payloadSize = payloadSize;
}
@Getter
@NotNull
@TransferOrder(value = 0)
public  String name;
@Getter
@NotNull
@TransferOrder(value = 1)
public String version;
@Getter
@TransferOrder(value = 2)
public int payloadSize;
@Getter @Setter
@TransferOrder(value = 3)
public String licenseType;
@Getter @Setter
@TransferOrder(value = 4)
public String payloadType;
@Getter @Setter
@Nullable
@TransferOrder(value = 5)
public DependencyInfoDTO[] dependencies;
@Getter @Setter
@Nullable
@TransferOrder(value = 6)
public String[] aliases;
}
