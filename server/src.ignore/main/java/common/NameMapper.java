package common;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static common.PackageStorage.*;


/**
 * This class store aliases for definite PackageId. Each alias can belong only for single PackageId
 */
public class NameMapper {
private final Map<String, PackageId> mapper;

public NameMapper() {
      mapper = new HashMap<>(); //thinking about multithreading
}
public synchronized Optional<PackageId> get(String alias){
      return Optional.ofNullable(mapper.get(alias));
}
public synchronized boolean add(PackageId id, String alias) {
      PackageId putted = mapper.putIfAbsent(alias, id);
      return putted == null;
}

/**
 * @return true if and only if all aliases are free
 * */
public synchronized boolean addAll(PackageId id, @NotNull Collection<String> aliases) {
      boolean isBusyAlias = contains(aliases);
      if(!isBusyAlias){
            aliases.forEach(alias -> mapper.put(alias, id));
      }
      return !isBusyAlias;
}
public boolean contains(@NotNull String alias){
      return mapper.containsKey(alias);
}
public boolean contains(@NotNull Collection<String> aliases){
      boolean isBusyAlias = true;
      for (String alias : aliases){
            isBusyAlias = mapper.get(alias) != null;
            if (isBusyAlias)
                  break;
      }
      return isBusyAlias;
}
}
