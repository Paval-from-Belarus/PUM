package org.petos.packagemanager.client;

import com.google.gson.Gson;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InputProcessor {
public InputProcessor(){
      initCommands();
}
public InputGroup nextGroup(){
      Scanner input = new Scanner(System.in);
      InputGroup inputGroup;
      do {
	    String line = input.nextLine();
	    inputGroup = nextCommand(line);
	    if (inputGroup == null)
		  System.out.println("Incorrect params. Try again!");
      }
      while (inputGroup == null);
      return inputGroup;
}
public  record InputGroup(UserInput type, String[] params) { }
public enum UserInput {ListAll, Install, Exit, Unknown}
public static class InputCommand {
      public String pattern;
      public UserInput type;
      public boolean hasParams;
      public Pattern pattern() {
	    return Pattern.compile(pattern);
      }

      public boolean hasParams() {
	    return hasParams;
      }

      public UserInput type() {
	    return type;
      }

}
private String[] collectParams(Matcher matcher) {
      List<String> params = new ArrayList<>();
      for (int i = 1; i <= matcher.groupCount(); i++) {
	    if (matcher.group(i) != null) {
		  params.add(matcher.group(i));
	    }
      }
      return params.toArray(new String[0]);
}
private @Nullable InputGroup nextCommand(String line) {
      InputGroup result = null;
      for (InputCommand command : this.rules) {
	    Matcher matcher = command.pattern().matcher(line);
	    if (matcher.find()) {
		  String[] params = collectParams(matcher);
		  result = new InputGroup(command.type(), params);
		  break;
	    }
      }
      return result;
}
private void initCommands() {
      try {
	    String config = Files.readString(Path.of("commands.json"));
	    this.rules = new Gson().fromJson(config, InputCommand[].class);
      } catch (IOException e) {
	    throw new RuntimeException(e);
      }
}
private InputCommand[] rules;
}
