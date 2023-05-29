package requests;

import lombok.Getter;
import lombok.experimental.Accessors;
import transfer.TransferEntity;
import transfer.TransferOrder;

@Accessors(fluent = true)
@TransferEntity(selective = true, ignoreNullable = true)
public class InfoRequest {
@Getter
@Accessors(fluent = false)
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

//public String stringify() {
//      String result;
//      if (label.isEmpty()) {
//	    result = stringify(packageId, offset);
//      } else {
//	    result = stringify(packageId);
//	    result = join(result, label);
//      }
//      return result;
//}
//
//public static Optional<InfoRequest> valueOf(String content, VersionFormat format) {
//      final int fieldCnt = 2;
//      InfoRequest request = null;
//      List<byte[]> bytes = split(content, fieldCnt);
//      if (bytes.size() == fieldCnt) { //if something go wrong, the size is reduced
//	    int id = toInteger(bytes.get(0));
//	    request = switch (format) {
//		  case String -> new InfoRequest(id, toString(bytes.get(1)));
//		  case Int -> new InfoRequest(id, toInteger(bytes.get(1)));
//		  default -> null;
//	    };
//      }
//      return Optional.ofNullable(request);
//}
}
