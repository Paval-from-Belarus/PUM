package org.petos.pum.networks.requests;

import lombok.Getter;
import org.petos.pum.networks.transfer.TransferRequest;
import org.petos.pum.networks.transfer.NetworkExchange;
import org.petos.pum.networks.transfer.TransferEntity;
import org.petos.pum.networks.transfer.TransferOrder;

@TransferRequest(NetworkExchange.RequestType.GetInfo)
@TransferEntity(ignoreNullable = true, code = 11)
public class InfoRequest {
@Getter
@TransferOrder(value = 0)
private final Integer packageId;
@Getter
@TransferOrder(value = 1)
private Integer offset;
@Getter
@TransferOrder(value = 1)
private String label;

public InfoRequest(Integer packageId) {
      this.packageId = packageId;
      this.label = "";
}

public InfoRequest(Integer packageId, Integer offset) {
      this(packageId);
      this.offset = offset;
}

public InfoRequest(Integer packageId, String label) {
      this(packageId);
      this.label = label;
}
}
