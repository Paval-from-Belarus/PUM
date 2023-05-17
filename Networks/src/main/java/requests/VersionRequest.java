package requests;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@Accessors(fluent = true)
public class VersionRequest extends AbstractRequest {
@Getter
private Integer packageId;
@Getter
private String label;

@Override
public String stringify() {
      String result = stringify(packageId);
      return join(result, label);
}

public static Optional<VersionRequest> valueOf(String content) {
      final int fieldCnt = 2;
      VersionRequest request = null;
      List<byte[]> bytes = split(content, fieldCnt);
      if (bytes.size() == fieldCnt) {
	    int id = toInteger(bytes.get(0));
	    String version = toString(bytes.get(1));
	    request = new VersionRequest(id, version);
      }
      return Optional.ofNullable(request);
}
}
