package org.petos.packagemanager.packages;

import org.jetbrains.annotations.NotNull;

public record ShortPackageInfoDTO(Integer id, @NotNull String name, @NotNull String version,
				  @NotNull String[] aliases) {
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
}
