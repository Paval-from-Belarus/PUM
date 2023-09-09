package org.petos.pum.networks.dto;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.petos.pum.networks.transfer.TransferEntity;
import org.petos.pum.networks.transfer.TransferOrder;

@EqualsAndHashCode
@TransferEntity(code = 18)
@AllArgsConstructor
public final class DependencyInfoDTO {
@Getter
@NotNull
@TransferOrder(value = 0)
private final Integer packageId;
@Getter
@NotNull
@TransferOrder(value = 1)
private final String label;

}
