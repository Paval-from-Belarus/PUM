package requests;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import dto.PublishInstanceDTO;

import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@MethodRequest(name="PublishPayload")
public class PublishInstanceRequest extends AbstractRequest {
@Getter
@NotNull
private Integer authorId;
@Getter
@NotNull
private PublishInstanceDTO dto;

@Override
public String stringify() {
      String result = stringify(authorId);
      return join(result, dto.stringify());
}

public static Optional<PublishInstanceRequest> valueOf(String content) {
      Optional<PublishInstanceRequest> optional = Optional.empty();
      List<byte[]> bytes = split(content, 2);
      if (bytes.size() == 2) {
	    Integer author = toInteger(bytes.get(0));
	    var dto = PublishInstanceDTO.valueOf(toString(bytes.get(1)));
	    optional = dto.map(info -> new PublishInstanceRequest(author, info));
      }
      return optional;
}
}
