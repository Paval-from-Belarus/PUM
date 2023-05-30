package requests;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import transfer.TransferEntity;
import transfer.TransferOrder;

@Accessors(fluent = true)
@TransferEntity(selective = true, ignoreNullable = true)
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
