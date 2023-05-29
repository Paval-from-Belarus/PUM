package dto;

import lombok.*;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@Accessors(prefix="", makeFinal = true, fluent = true)
@EqualsAndHashCode
public class ShortPackageInfoDTO {
@NotNull
@Getter
private Integer id;
@NotNull
@Getter
private String name;
@NotNull
@Getter
private String version;
@Nullable
@Getter
private String[] aliases;
//public boolean similar(String word) {
//      if (word == null || word.isEmpty())
//	    return false;
//      if (name.equals(word))
//	    return true;
//      boolean isFound = false;
//      for (String alias : aliases) {
//	    isFound = alias.equals(word);
//	    if (isFound)
//		  break;
//      }
//      return isFound;
//}

//public static ShortPackageInfoDTO valueOf(Integer id, FullPackageInfoDTO dto) {
//      return new ShortPackageInfoDTO(id, dto.name, dto.version, dto.aliases);
//}
//public static Optional<ShortPackageInfoDTO> valueOf(String content) {
//      return valueOf(ShortPackageInfoDTO.class, content);
//}
}
