package database;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public record CachedInfo(Integer id, String name, @NotNull String[] aliases){
      public boolean similar(String name) {
	    if (this.name.equals(name)) {
		  return true;
	    }
	    return Arrays.asList(aliases).contains(name);
      }
      @Override
      public boolean equals(Object object) {
	    boolean result = false;
	    if (object instanceof CachedInfo other) {
		  result = id.equals(other.id) || name.equals(other.name);
	    }
	    return result;
      }
}

