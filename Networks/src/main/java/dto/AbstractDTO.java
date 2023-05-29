package dto;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import transfer.TransferEntity;

import java.util.Optional;
@TransferEntity
abstract class AbstractDTO implements SimpleDTO {
private static final Gson gson = new Gson();

@Override
public String stringify() {
      return gson.toJson(this);
}

protected static <T> Optional<T> valueOf(Class<T> clazz, String content) {
      T dto = null;
      try {
	    dto = gson.fromJson(content, clazz);
      } catch (JsonSyntaxException ignored) {
      }
      return Optional.ofNullable(dto);
}
}
