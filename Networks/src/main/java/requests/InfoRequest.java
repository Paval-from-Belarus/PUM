package requests;

import lombok.Getter;
import lombok.experimental.Accessors;
import transfer.TransferEntity;
import transfer.TransferOrder;

@TransferEntity(selective = true, ignoreNullable = true)
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
