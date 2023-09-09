package org.petos.pum.networks.requests;

import lombok.*;
import org.jetbrains.annotations.NotNull;
import org.petos.pum.networks.dto.PublishInstanceDTO;
import org.petos.pum.networks.transfer.NetworkExchange;
import org.petos.pum.networks.transfer.TransferEntity;
import org.petos.pum.networks.transfer.TransferOrder;
import org.petos.pum.networks.transfer.TransferRequest;

@TransferRequest(NetworkExchange.RequestType.PublishPayload)
@TransferEntity(code = 14)
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class PublishInstanceRequest {
@Getter
@NotNull
@TransferOrder(value = 0)
private Integer authorId;
@Getter
@NotNull
@TransferOrder(value = 1)
private PublishInstanceDTO dto;
}
