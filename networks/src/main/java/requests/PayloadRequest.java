package requests;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import transfer.TransferEntity;
import transfer.TransferFormat;
import transfer.TransferOrder;

import java.util.List;
import java.util.Optional;

import static security.Encryptor.*;
import static transfer.PackageAssembly.ArchiveType;

@EqualsAndHashCode
@TransferEntity(selective = true, ignoreNullable = true)
public class PayloadRequest {

public PayloadRequest(Integer packageId, String label, ArchiveType archive) {
      this(packageId, archive);
      this.label = label;

}

public PayloadRequest(Integer packageId, Integer offset, ArchiveType archive) {
      this(packageId, archive);
      this.offset = offset;
}

private PayloadRequest(Integer packageId, ArchiveType archive) {
      this.packageId = packageId;
      this.archive = archive;
      this.encryption = Encryption.None;
}
@Getter
@TransferOrder(value = 0)
private final Integer packageId;

@Getter
@TransferOrder(value = 1)
private String label;
@Getter
@TransferOrder(value = 1)
private Integer offset;
@NotNull
@TransferOrder(value = 2)
private final ArchiveType archive;
@NotNull
@Setter
@TransferOrder(value = 3)
private Encryption encryption;

public TransferFormat getTransfer() {
      return new TransferFormat(archive, encryption);
}
}
