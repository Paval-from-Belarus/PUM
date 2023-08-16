package dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import transfer.TransferEntity;
import transfer.TransferOrder;

import java.util.Optional;
@TransferEntity(selective = true, ignoreNullable = true)
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
