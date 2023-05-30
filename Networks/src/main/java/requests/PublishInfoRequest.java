package requests;

import lombok.AllArgsConstructor;
import lombok.Getter;
import dto.PublishInfoDTO;
import org.jetbrains.annotations.NotNull;
import transfer.TransferEntity;
import transfer.TransferOrder;

import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@TransferEntity(selective = true)
public class PublishInfoRequest {
@Getter
@NotNull
@TransferOrder(value = 0)
private Integer authorId;
@Getter
@NotNull
@TransferOrder(value = 1)
private PublishInfoDTO dto;
}
