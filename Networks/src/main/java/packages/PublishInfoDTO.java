package packages;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@AllArgsConstructor
@Accessors(prefix = "", makeFinal = true, fluent = true)
public class PublishInfoDTO extends AbstractDTO{
@NotNull
@Getter
private String name;
@NotNull
@Getter
private String[] aliases;
@Getter
private String payloadType;
public static Optional<PublishInfoDTO> valueOf(String content) {
      return valueOf(PublishInfoDTO.class, content);
}
}
