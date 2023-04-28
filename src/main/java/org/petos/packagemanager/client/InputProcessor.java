package org.petos.packagemanager.client;

import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InputProcessor {
public InputProcessor() {
      initCommands();
}

public InputGroup nextGroup() {
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

public record InputGroup(@NotNull UserInput type, @NotNull Map<ParameterType, List<InputParameter>> typeMap) {
      public @Nullable List<InputParameter> rawParams(){
	    return typeMap.get(ParameterType.Raw);
      }
      public @Nullable List<InputParameter> shortParams(){
	    return typeMap.get(ParameterType.Shorts);
      }
      public @Nullable List<InputParameter> verboseParams(){
	    return typeMap.get(ParameterType.Verbose);
      }
      public static InputGroup valueOf(UserInput type, Collection<InputParameter> params){
	    Map<ParameterType, List<InputParameter>> typeMap = new HashMap<>();
	    for (InputParameter parameter : params){
		  var list = typeMap.getOrDefault(parameter.type(), new ArrayList<>());
		  list.add(parameter);
		  typeMap.put(parameter.type(), list);
	    }
	    fillGaps(typeMap);
	    return new InputGroup(type, typeMap);
      }
      private static void fillGaps(Map<ParameterType, List<InputParameter>> typeMap){
	    for (ParameterType type : ParameterType.values()){
		  List<InputParameter> list = typeMap.get(type);
		  if (list == null){
			list = new ArrayList<>(0);
			typeMap.put(type, list);
		  }
	    }
      }
}

public enum ParameterType {Raw, Shorts, Verbose}

public record InputParameter(@NotNull ParameterType type, @NotNull String self, @NotNull String value) {
}

public enum UserInput {List, Install, Exit, Unknown}

public static class InputPattern {
      public UserInput type;
      public String[] aliases;
      public String[] shorts; //shorts are really single letters
      public String[] verbose;

      public Pattern pattern() {
	    assert aliases != null && aliases.length >= 1;
	    StringBuilder strText = new StringBuilder();
	    for (String alias : aliases) {
		  strText.append(alias).append("|");
	    }
	    strText.setLength(strText.length() - 1);
	    return Pattern.compile(strText.toString());
      }

      public @NotNull String[] verboseParams() {
	    return verbose;
      }

      public @NotNull String[] shortParams() {
	    return shorts;
      }

      public UserInput type() {
	    return type;
      }

}

private @NotNull List<InputParameter> collectParams(String source, InputPattern pattern) {
      List<InputParameter> params = new ArrayList<>();
      //search of short parameters
      final int RAW_PARAMETER = 0;
      final int SHORT_PARAMETER = 1;
      final int VERBOSE_PARAMETER = 2;
      char[] letters = source.toCharArray();
      for (int i = 0; i < letters.length; ) {
	    int paramIndex = RAW_PARAMETER; //none
	    if (letters[i] == '-') {
		  int offset = 1;
		  paramIndex = SHORT_PARAMETER;
		  if (i + 1 < letters.length && letters[i + 1] == '-') {
			paramIndex = VERBOSE_PARAMETER;
			offset += 1;
		  }
		  i += offset;
	    }
	    StringBuilder nameBuilder = new StringBuilder();
	    StringBuilder valueBuilder = new StringBuilder();
	    //skip spaces
	    while (i < letters.length && letters[i] == ' ') {
		  i += 1;
	    }
	    while (i < letters.length && letters[i] != ' ') { //attempt to extract self parameter name
		  nameBuilder.append(letters[i]);
		  i += 1;
	    }
	    while (i < letters.length && letters[i] == ' ') {
		  i += 1;
	    }
	    while (i < letters.length && letters[i] != '-') {
		  valueBuilder.append(letters[i]);
		  i += 1;
	    }
	    if (((paramIndex == VERBOSE_PARAMETER || paramIndex == SHORT_PARAMETER) && !nameBuilder.isEmpty()) || paramIndex == RAW_PARAMETER) {
		  String name = nameBuilder.toString();
		  String value = valueBuilder.toString();
		  ParameterType type = ParameterType.values()[paramIndex];
		  params.add(new InputParameter(type, name, value));
	    }
      }
      return params;
}

private @Nullable InputGroup nextCommand(@NotNull String line) {
      InputGroup result = null;
      String[] groups = line.split("( +)", 2); //split first occurene of params to different side
      for (InputPattern command : this.rules) {
	    Matcher matcher = command.pattern().matcher(groups[0]);
	    if (matcher.find()) {
		  List<InputParameter> params = List.of();
		  if (groups.length >= 2)
			 params = collectParams(groups[1], command);
		  result = InputGroup.valueOf(command.type(), params);
		  break;
	    }
      }
      return result;
}

private void initCommands() {
      try {
	    String config = Files.readString(Path.of("commands.json"));
	    this.rules = new Gson().fromJson(config, InputPattern[].class);
      } catch (IOException e) {
	    throw new RuntimeException(e);
      }
}

private InputPattern[] rules;
}
