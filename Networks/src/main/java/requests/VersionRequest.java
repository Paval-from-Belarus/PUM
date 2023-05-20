package requests;

import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Optional;

@Accessors(fluent = true)
@MethodRequest(name="GetVersion")
public class VersionRequest extends AbstractRequest {
@Getter
@Accessors(fluent = false)
final private Integer packageId;
@Getter
private String label;
@Getter
private int offset;
private VersionRequest(Integer id) {
      this.packageId = id;
      label = "";
}
public VersionRequest(Integer id, String label) {
      this(id);
      this.label = label;
}
public VersionRequest(Integer id, int offset) {
      this(id);
      this.offset = offset;
}
@Override
public String stringify() {
      String result = stringify(packageId);
      if (label.isEmpty()) {
	    result = join(result, offset);
      } else {
	    result = join(result, label);
      }
      return result;
}

public static Optional<VersionRequest> valueOf(String content, VersionFormat format) {
      final int fieldCnt = 2;
      VersionRequest request = null;
      List<byte[]> bytes = split(content, fieldCnt);
      if (bytes.size() == fieldCnt) {
	    int id = toInteger(bytes.get(0));
	    request = switch(format) {
		  case String -> new VersionRequest(id, toString(bytes.get(1)));
		  case Int -> new VersionRequest(id, toInteger(bytes.get(1)));
		  case Unknown -> null;
	    };
      }
      return Optional.ofNullable(request);
}
}
