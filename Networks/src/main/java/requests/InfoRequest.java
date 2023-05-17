package requests;

import lombok.Getter;

import java.util.List;
import java.util.Optional;


public class InfoRequest extends AbstractRequest {
@Getter
private final Integer id;
@Getter
private Integer offset;
@Getter
private String label;

public InfoRequest(Integer id) {
      this.id = id;
      this.label = "";
}

public InfoRequest(Integer id, Integer offset) {
      this(id);
      this.offset = offset;
}

public InfoRequest(Integer id, String label) {
      this(id);
      this.label = label;
}

public String stringify() {
      String result;
      if (label.isEmpty()) {
	    result = stringify(id, offset);
      } else {
	    result = stringify(id);
	    result = join(result, label);
      }
      return result;
}

public static Optional<InfoRequest> valueOf(String content, VersionFormat format) {
      final int fieldCnt = 2;
      InfoRequest request = null;
      List<byte[]> bytes = split(content, fieldCnt);
      if (bytes.size() == fieldCnt) { //if something go wrong, the size is reduced
	    int id = toInteger(bytes.get(0));
	    request = switch (format) {
		  case String -> new InfoRequest(id, toString(bytes.get(1)));
		  case Int -> new InfoRequest(id, toInteger(bytes.get(1)));
		  default -> null;
	    };
      }
      return Optional.ofNullable(request);
}
}
