package packages;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PACKAGE)
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