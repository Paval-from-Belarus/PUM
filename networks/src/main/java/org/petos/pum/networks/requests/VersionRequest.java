package org.petos.pum.networks.requests;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.petos.pum.networks.transfer.NetworkExchange;
import org.petos.pum.networks.transfer.TransferEntity;
import org.petos.pum.networks.transfer.TransferOrder;
import org.petos.pum.networks.transfer.TransferRequest;

@TransferRequest(NetworkExchange.RequestType.GetVersion)
@TransferEntity(ignoreNullable = true, code = 15)
@Accessors(fluent = true)
@EqualsAndHashCode
public class VersionRequest {
@Getter
@Accessors(fluent = false)
@TransferOrder(value = 0)
final private Integer packageId;
@Nullable
@Getter
@TransferOrder(value = 1)
private String label;
@Nullable
@Getter
@TransferOrder(value = 1)
private int offset;
private VersionRequest(Integer id) {
      this.packageId = id;
}
public VersionRequest(Integer id, @NotNull String label) {
      this(id);
      this.label = label;
}
public VersionRequest(Integer id, int offset) {
      this(id);
      this.offset = offset;
}
}
