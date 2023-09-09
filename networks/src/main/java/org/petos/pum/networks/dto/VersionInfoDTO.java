package org.petos.pum.networks.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.petos.pum.networks.transfer.TransferEntity;
import org.petos.pum.networks.transfer.TransferOrder;

@AllArgsConstructor
@TransferEntity(code = 22)
public class VersionInfoDTO {
@NotNull
@Getter
@TransferOrder(value = 0)
private final Integer versionId;
@NotNull
@Getter
@TransferOrder(value = 1)
private final String label;
}