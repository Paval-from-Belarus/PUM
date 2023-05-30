package dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import transfer.TransferEntity;
import transfer.TransferOrder;

import java.util.Optional;

@AllArgsConstructor
@TransferEntity(selective = true)
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