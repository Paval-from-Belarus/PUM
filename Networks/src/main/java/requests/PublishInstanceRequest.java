package requests;

import lombok.*;
import org.jetbrains.annotations.NotNull;
import dto.PublishInstanceDTO;
import transfer.TransferEntity;
import transfer.TransferOrder;

import java.util.List;
import java.util.Optional;


@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@TransferEntity(selective = true)
public class PublishInstanceRequest  {
@Getter
@NotNull
@TransferOrder(value = 0)
private Integer authorId;
@Getter
@NotNull
@TransferOrder(value = 1)
private PublishInstanceDTO dto;
}
