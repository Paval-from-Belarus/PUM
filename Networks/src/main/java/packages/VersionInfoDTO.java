package packages;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@AllArgsConstructor
@Accessors(prefix="", makeFinal = true, fluent = true)
public class VersionInfoDTO extends AbstractDTO{
@NotNull
@Getter
private Integer versionId;
@NotNull
@Getter
private String label;
public static Optional<VersionInfoDTO> valueOf(String content) {
      return valueOf(VersionInfoDTO.class, content);
}
}