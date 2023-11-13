package org.petos.pum.networks.requests;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.petos.pum.networks.old_dto.PublishInfoDTO;
import org.petos.pum.networks.transfer.TransferRequest;
import org.jetbrains.annotations.NotNull;
import org.petos.pum.networks.transfer.NetworkExchange;
import org.petos.pum.networks.transfer.TransferEntity;
import org.petos.pum.networks.transfer.TransferOrder;

@TransferRequest(NetworkExchange.RequestType.PublishInfo)
@TransferEntity(code = 13)
@AllArgsConstructor
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
