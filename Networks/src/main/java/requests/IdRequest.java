package requests;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;

@AllArgsConstructor
public class IdRequest extends AbstractRequest{
@Getter
private String alias;
@Override
public String stringify() {
      return alias;
}
public static Optional<IdRequest> valueOf(String content) {
      return Optional.of(new IdRequest(content));
}
}
