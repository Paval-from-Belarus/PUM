package org.petos.pum.networks.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.petos.pum.networks.transfer.TransferEntity;
import org.petos.pum.networks.transfer.TransferOrder;

@TransferEntity(ignoreNullable = true, code = 19)
@AllArgsConstructor
public class PublishInfoDTO {
@NotNull
@Getter
@TransferOrder(value = 0)
private final String name;
@NotNull
@Getter
@TransferOrder(value = 1)
private final String payloadType;
@Nullable
@Getter
@TransferOrder(value = 2)
private final String[] aliases;
}
