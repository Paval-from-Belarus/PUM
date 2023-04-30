package org.petos.packagemanager.client;

import org.hibernate.query.criteria.internal.expression.function.AggregationFunction;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class InstanceInfo {
private final static int MIN_LETTERS_PER_INSTANCE = 19;
private final String[] aliases;
private final String path;

public InstanceInfo(String[] aliases, String path) {
      this.aliases = aliases;
      this.path = path;
}

public Path getPath() {
      return Path.of(path);
}

public String[] getAliases() {
      return aliases;
}

@Override
public String toString() {
      StringBuilder strText = new StringBuilder();
      strText.append("aliases=[");
      for (String alias : aliases)
	    strText.append(alias).append(",");
      strText.setLength(strText.length() - 1);
      strText.append("]");
      strText.append("path=[").append(path).append("]\n");
      return strText.toString();
}

public static List<InstanceInfo> valueOf(@NotNull String source) {
      if (source.length() < MIN_LETTERS_PER_INSTANCE) //magic_number
	    return List.of();
      String[] separated = source.split("\n");
      List<InstanceInfo> list = new ArrayList<>();
      for (String line : separated) {
	    String[] parts = line.split("]");
	    if (parts.length == 2) {
		  String[] aliases = collectParams(parts[0] + "]");
		  String[] path = collectParams(parts[1] + "]");
		  if (path.length == 1)
			list.add(new InstanceInfo(aliases, path[0]));

	    }
      }
      return list;
}

//collect params upon the next the first occurrence of `]` letter
//parameter is any sequence of letter besides space and coma
private static String[] collectParams(String line) {
      String[] params = line.split("( *, *)|(])");
      int index = 0;
      for (char letter : params[0].toCharArray()) {
	    if (letter == '['){
		  break;
	    }
	    index += 1;
      }
      params[0] = params[0].substring(index + 1);
      return params;
}
}
