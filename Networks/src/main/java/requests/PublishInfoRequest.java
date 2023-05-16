package requests;

import lombok.AllArgsConstructor;
import lombok.Getter;
import packages.PublishInfoDTO;

import java.util.List;
import java.util.Optional;

@AllArgsConstructor
public class PublishInfoRequest extends AbstractRequest {
@Getter
private Integer authorId;
@Getter
private PublishInfoDTO dto;

@Override
public String stringify() {
      String result = stringify(authorId);
      return join(result, dto.stringify());
}

public static Optional<PublishInfoRequest> valueOf(String content) {
      PublishInfoRequest request = null;
      List<byte[]> bytes = split(content);
      if (bytes.size() == 2) {
	    int id = toInteger(bytes.get(0));
	    String stringDto = toString(bytes.get(1));
	    var dto = PublishInfoDTO.valueOf(stringDto);
	    if (dto.isPresent()) {
		  request = new PublishInfoRequest(id, dto.get());
	    }
      }
      return Optional.ofNullable(request);
}
}
