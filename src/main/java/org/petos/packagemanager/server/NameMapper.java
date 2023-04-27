package org.petos.packagemanager.server;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.petos.packagemanager.server.PackageStorage.*;

/**
 * This class store aliases for definite PackageId. Each alias can belong only for single PackageId
 */
public class NameMapper {
private final Map<String, PackageId> mapper;

public NameMapper() {
      mapper = new HashMap<>();
}

public boolean addAlias(PackageId id, String alias) {
      PackageId putted = mapper.putIfAbsent(alias, id);
      return putted == null;
}

/**
 * @return true if and only if all aliases are free
 * */
public boolean addAll(PackageId id, List<String> aliases) {
      boolean isBusyAlias = true;
      for (String alias : aliases){
            isBusyAlias = mapper.get(alias) != null;
            if (isBusyAlias)
                  break;
      }
      if(!isBusyAlias){
            aliases.forEach(alias -> mapper.put(alias, id));
      }
      return isBusyAlias;
}
}
