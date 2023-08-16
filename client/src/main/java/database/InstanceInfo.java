package database;

import lombok.AccessLevel;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

//conventionally, the first name in aliases is correct name
public class InstanceInfo {
public enum LinkState {Add, Remove}
private final static int MIN_LETTERS_PER_INSTANCE = 19;
private final Integer packageId;
private final String[] aliases; //the first
private final String path; //where the full info is stored
@Setter(AccessLevel.PRIVATE)
private int linksCnt;//by default no
public InstanceInfo(Integer id, String[] aliases, String path) {
      this.packageId = id;
      this.aliases = aliases;
      this.path = path;
}
public int getLinksCnt(){
      return linksCnt;
}
public void updateLinksCnt(LinkState link){
      int delta = 1;
      if (link == LinkState.Remove)
	    delta = -1;
      linksCnt = Math.max(linksCnt + delta, 0);
}

public String[] getAliases() {
      return aliases;
}
public boolean similar(@NotNull String name){
      boolean response = false;
      for (String alias : aliases) {
	    response = alias.equals(name);
	    if (response)
		  break;
      }
      return response;
}
public String getStringPath() {
      return path;
}

public Integer getId() {
      return packageId;
}

@Override
public String toString() {
      StringBuilder strText = new StringBuilder();
      strText.append("id=[").append(packageId).append("]");
      strText.append("aliases=[");
      for (String alias : aliases)
	    strText.append(alias).append(",");
      strText.setLength(strText.length() - 1);
      strText.append("]");
      strText.append("path=[").append(path).append("]");
      strText.append("links=[").append(linksCnt).append("]\r\n");
      return strText.toString();
}

@Override
public boolean equals(Object other) {
      boolean result = false;
      if (other instanceof InstanceInfo info) {
	    result = Objects.equals(info.packageId, this.packageId);
      }
      return result;
}


// TODO: rewrite to finite state machine
//this method should never lie
public static List<InstanceInfo> valueOf(@NotNull String source) {
      if (source.length() < MIN_LETTERS_PER_INSTANCE) //magic_number
	    return List.of();
      String[] separated = source.split("\r\n");
      List<InstanceInfo> list = new ArrayList<>();
      for (String line : separated) {
	    try {
		  String[] parts = line.split("]");
		  if (parts.length == 4) {
			String[] id = collectParams(parts[0]);
			String[] aliases = collectParams(parts[1] + "]");
			String[] path = collectParams(parts[2] + "]");
			String[] links = collectParams(parts[3] + "]");
			if (links.length == 1 && path.length == 1 && id.length == 1 && aliases.length != 0) {
			      var instance = new InstanceInfo(Integer.parseInt(id[0]), aliases, path[0]);
			      instance.setLinksCnt(Integer.parseInt(links[0]));
			      list.add(instance);
			}

		  }
	    } catch (NumberFormatException ignored) {
	    }
      }
      return list;
}

//collect params upon the next the first occurrence of `]` letter
//parameter is any sequence of letter besides space and coma
public static String[] collectParams(@NotNull String line) {
      String[] params = line.split("( *, *)|(])");
      int index = 0;
      for (char letter : params[0].toCharArray()) {
	    if (letter == '[') {
		  break;
	    }
	    index += 1;
      }
      if (index + 1 >= params[0].length())
	    params = new String[0];
      else
	    params[0] = params[0].substring(index + 1);
      return params;
}

}
