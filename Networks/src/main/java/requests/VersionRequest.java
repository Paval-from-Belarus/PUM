package requests;

import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Optional;

@AllArgsConstructor
public class VersionRequest extends AbstractRequest {
private Integer packageId;
private String version;

@Override
public String stringify() {
      String result = stringify(packageId);
      return join(result, version);
}

public static Optional<VersionRequest> valueOf(String content) {
      VersionRequest request = null;
      List<byte[]> bytes = split(content);
      if (bytes.size() == 2) {
	    int id = toInteger(bytes.get(0));
	    String version = toString(bytes.get(1));
	    request = new VersionRequest(id, version);
      }
      return Optional.ofNullable(request);
}
}
