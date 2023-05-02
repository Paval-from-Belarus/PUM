package org.petos.packagemanager.client;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

import static org.petos.packagemanager.client.InputProcessor.*;

public class ParameterMap {
private final Map<ParameterType, List<InputParameter>> typeMap;
public ParameterMap(Map<ParameterType, List<InputParameter>> typeMap) {
      this.typeMap = typeMap;
}
public @NotNull List<InputParameter> get(ParameterType type){
      return typeMap.get(type);
}
public boolean hasParameter(ParameterType type, String self){
      var list = this.get(type);
      return list.stream()
          .anyMatch(p -> p.self().equals(self));
}

}
