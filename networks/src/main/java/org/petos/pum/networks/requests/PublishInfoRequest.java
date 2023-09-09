package org.petos.pum.networks.requests;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.petos.pum.networks.dto.PublishInfoDTO;
import org.jetbrains.annotations.NotNull;
import org.petos.pum.networks.transfer.NetworkExchange;
import org.petos.pum.networks.transfer.TransferEntity;
import org.petos.pum.networks.transfer.TransferOrder;
import org.petos.pum.networks.transfer.TransferRequest;

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
