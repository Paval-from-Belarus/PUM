package dto;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import transfer.TransferEntity;
import transfer.TransferOrder;

@EqualsAndHashCode
@TransferEntity(selective = true)
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
