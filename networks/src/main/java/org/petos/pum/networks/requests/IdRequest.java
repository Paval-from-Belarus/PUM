package org.petos.pum.networks.requests;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.petos.pum.networks.transfer.TransferRequest;
import org.petos.pum.networks.transfer.NetworkExchange;
import org.petos.pum.networks.transfer.TransferEntity;

@TransferRequest(NetworkExchange.RequestType.GetId)
@TransferEntity(selective = false, code = 10)//the order of one field is not interesting
@AllArgsConstructor
public final class IdRequest {
@Getter
private final String alias;


}
