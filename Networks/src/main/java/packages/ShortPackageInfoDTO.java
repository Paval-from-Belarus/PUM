package packages;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@AllArgsConstructor
@Accessors(prefix="", makeFinal = true, fluent = true)
public class ShortPackageInfoDTO extends AbstractDTO{
@NotNull
@Getter
private Integer id;
@NotNull
@Getter
private String name;
@NotNull
@Getter
private String version;
@NotNull
@Getter
private String[] aliases;
public boolean similar(String word) {
      if (word == null || word.isEmpty())
	    return false;
      if (name.equals(word))
	    return true;
      boolean isFound = false;
      for (String alias : aliases) {
	    isFound = alias.equals(word);
	    if (isFound)
		  break;
      }
      return isFound;
}

public static ShortPackageInfoDTO valueOf(Integer id, FullPackageInfoDTO dto) {
      return new ShortPackageInfoDTO(id, dto.name, dto.version, dto.aliases);
}
public static Optional<ShortPackageInfoDTO> valueOf(String content) {
      return valueOf(ShortPackageInfoDTO.class, content);
}
}
