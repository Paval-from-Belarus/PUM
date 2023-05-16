package requests;

import lombok.Getter;

import java.util.List;
import java.util.Optional;

import static transfer.PackageAssembly.ArchiveType;
import static transfer.PackageAssembly.decompress;

public class PayloadRequest extends AbstractRequest {

private PayloadRequest(Integer id, ArchiveType archive) {
      this.id = id;
      this.archive = archive;
      this.label = "";
}

public PayloadRequest(Integer id, String label, ArchiveType archive) {
      this(id, archive);
      this.label = label;

}

public PayloadRequest(Integer id, Integer offset, ArchiveType archive) {
      this(id, archive);
      this.offset = offset;
}


@Getter
private final Integer id;
@Getter
private final ArchiveType archive;
@Getter
private String label;
@Getter
private Integer offset;

public String stringify() {
      String result;
      if (label.isEmpty()) {
	    result = stringify(id, archive.ordinal(), offset);
      } else {
	    result = stringify(id, archive.ordinal());
	    result = join(result, label);
      }
      return result;
}

public static Optional<PayloadRequest> valueOf(String content, VersionFormat format) {
      assert content != null;
      Optional<PayloadRequest> optional = Optional.empty();
      List<byte[]> bytes = split(content);
      if (bytes.size() == 3) {
	    int id = toInteger(bytes.get(0));
	    int ordinal = toInteger(bytes.get(1));
	    if (ordinal < ArchiveType.values().length && ordinal >= 0) {
		  ArchiveType type = ArchiveType.values()[ordinal];
		  optional = construct(format, id, type, bytes.get(2));
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
}
