package requests;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import transfer.TransferFormat;

import java.util.List;
import java.util.Optional;

import static security.Encryptor.*;
import static transfer.PackageAssembly.ArchiveType;

public class PayloadRequest extends AbstractRequest {

private PayloadRequest(Integer packageId, ArchiveType archive) {
      this.packageId = packageId;
      this.archive = archive;
      this.encryption = Encryption.None;
      this.label = "";
}

public PayloadRequest(Integer packageId, String label, ArchiveType archive) {
      this(packageId, archive);
      this.label = label;

}

public PayloadRequest(Integer packageId, Integer offset, ArchiveType archive) {
      this(packageId, archive);
      this.offset = offset;
}


@Getter
private final Integer packageId;
private final ArchiveType archive;
@Getter
private String label;
@Getter
private Integer offset;
@NotNull
@Setter
private Encryption encryption;

public TransferFormat getTransfer() {
      return new TransferFormat(archive, encryption);
}

public String stringify() {
      String result;
      if (label.isEmpty()) {
	    result = stringify(packageId, archive.ordinal(), offset);
      } else {
	    result = stringify(packageId, archive.ordinal());
	    result = join(result, label);
      }
      result = join(result, encryption.getEncoded());
      return result;
}

public static Optional<PayloadRequest> valueOf(String content, VersionFormat format) {
      assert content != null;
      Optional<PayloadRequest> optional = Optional.empty();
      List<byte[]> bytes = split(content, 4);
      if (bytes.size() == 4) {
	    int id = toInteger(bytes.get(0));
	    int ordinal = toInteger(bytes.get(1));
	    if (ordinal < ArchiveType.values().length && ordinal >= 0) {
		  ArchiveType type = ArchiveType.values()[ordinal];
		  optional = construct(format, id, type, bytes.get(2));
		  optional = optional.flatMap(request -> detachKey(request, bytes.get(3)));
	    }
      }
      return optional;
}

private static Optional<PayloadRequest> construct(VersionFormat format, Integer id, ArchiveType archive, byte[] version) {
      PayloadRequest dto = switch (format) {
	    case String -> new PayloadRequest(id, toString(version), archive);
	    case Int -> new PayloadRequest(id, toInteger(version), archive);
	    default -> null;
      };
      return Optional.ofNullable(dto);
}
private static Optional<PayloadRequest> detachKey(@NotNull PayloadRequest request, byte[] bytes) {
      Encryption cipher = Encryption.restore(bytes);
      if (cipher != null) {
	    request.encryption = cipher;
      } else {
	    request = null;
      }
      return Optional.ofNullable(request);
}
}
