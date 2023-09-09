package org.petos.pum.networks.requests;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.petos.pum.networks.transfer.*;

import static org.petos.pum.networks.security.Encryptor.*;
import static org.petos.pum.networks.transfer.PackageAssembly.ArchiveType;

@TransferRequest(NetworkExchange.RequestType.GetPayload)
@TransferEntity(ignoreNullable = true, code = 12)
@EqualsAndHashCode
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
