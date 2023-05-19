package storage;

import database.InstanceInfo;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JournalTransaction {
public enum Type {Remove, Install}

JournalTransaction(Type type, InstanceInfo instance) {
      this.type = type;
      this.instance = instance;
}

public boolean isDeletion() {
      return type == Type.Remove;
}

public boolean isInstallation() {
      return type == Type.Install;
}

public String stringify() {
      String instanceString = instance.toString();
      instanceString = instanceString.substring(0, instanceString.length() - 2);
      return "${" + type.toString() + "#" + instanceString + "}";
}

public static List<JournalTransaction> listOf(String lines) {
      Matcher matcher = pattern.matcher(lines);
      List<JournalTransaction> transactions = new ArrayList<>();
      while (matcher.find()) {
	    String[] parts = matcher.group(1).split("#");
	    assert parts.length == 2;
	    Type type = Type.valueOf(parts[0]);
	    List<InstanceInfo> instance = InstanceInfo.valueOf(parts[1]);
	    if (instance.size() == 1) //only single instance per transaction
		  transactions.add(new JournalTransaction(type, instance.get(0)));
      }
      return transactions;
}

private static final Pattern pattern;

static {
      pattern = Pattern.compile("\\$\\{([^$\n\r]+)}");
}

public String getStringPath() {
      return getInstance().getStringPath();
}

@Getter
private final Type type;
@Getter
private final InstanceInfo instance;
}
