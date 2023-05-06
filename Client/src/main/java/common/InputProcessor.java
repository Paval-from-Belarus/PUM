package common;

import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InputProcessor {
public static class InputFormatException extends Exception {
      InputFormatException(String msg) {
	    super(msg);
      }
}

public static class ConfigFormatException extends Exception {
      ConfigFormatException(String msg) {
	    super(msg);
      }
}

public InputProcessor() throws ConfigFormatException {
      initCommands();
}

public @NotNull InputGroup nextGroup() {
      Scanner input = new Scanner(System.in);
      Optional<InputGroup> promise;
      do {
	    String line = input.nextLine();
	    promise = nextCommand(line);
	    if (promise.isEmpty())
		  System.out.println("Incorrect params. Try again!");
      }
      while (promise.isEmpty());
      return promise.get();
}

public record InputGroup(@NotNull UserInput type, @NotNull Map<ParameterType, List<InputParameter>> typeMap) {
      public @NotNull List<InputParameter> rawParams() {
	    return typeMap.get(ParameterType.Raw);
      }

      public @NotNull List<InputParameter> shortParams() {
	    return typeMap.get(ParameterType.Shorts);
      }

      public @NotNull List<InputParameter> verboseParams() {
	    return typeMap.get(ParameterType.Verbose);
      }

      public ParameterMap params() {
	    return new ParameterMap(typeMap());
      }

      @Override
      public String toString() {
	    StringBuilder output = new StringBuilder("(type=" + type + ", map=\n");
	    for (var entry : typeMap().entrySet()) {
		  output.append("param=").append(entry.getKey()).append(", values=");
		  output.append(Arrays.toString(entry.getValue().toArray(new InputParameter[0])));
		  output.append("\n");
	    }
	    output.append(")");
	    return output.toString();
      }

      public static InputGroup valueOf(UserInput type, Collection<InputParameter> params) {
	    Map<ParameterType, List<InputParameter>> typeMap = new HashMap<>();
	    for (InputParameter parameter : params) {
		  var list = typeMap.getOrDefault(parameter.type(), new ArrayList<>());
		  List<InputParameter> bufferList = new ArrayList<>();
		  if (parameter.type() != ParameterType.Shorts) {
			bufferList.add(parameter);
		  } else {
			bufferList.addAll(splitShorts(parameter));
		  }
		  bufferList.forEach(p -> {
			if (!list.contains(p))
			      list.add(p);
		  });
		  typeMap.put(parameter.type(), list);
	    }
	    fillGaps(typeMap);
	    return new InputGroup(type, typeMap);
      }

      private static void fillGaps(Map<ParameterType, List<InputParameter>> typeMap) {
	    for (ParameterType type : ParameterType.values()) {
		  typeMap.computeIfAbsent(type, k -> new ArrayList<>(0));
	    }
      }

      //finally, the order of parameters doesn't have any sense
      private static @NotNull List<InputParameter> splitShorts(InputParameter shorts) {
	    assert shorts.type() == ParameterType.Shorts && !shorts.self().isEmpty();
	    var params = new ArrayList<InputParameter>();
	    if (!shorts.value().isEmpty()) {
		  final int LAST_INDEX = shorts.self().length() - 1;
		  String self = String.valueOf(shorts.self().charAt(LAST_INDEX));
		  params.add(new InputParameter(ParameterType.Shorts, self, shorts.value()));
		  shorts = new InputParameter(ParameterType.Shorts, shorts.self().substring(0, LAST_INDEX), "");
	    }
	    for (char letter : shorts.self().toCharArray()) {
		  String self = String.valueOf(letter);
		  params.add(new InputParameter(ParameterType.Shorts, self, ""));
	    }
	    return params;
      }

}

public enum ParameterType {Raw, Shorts, Verbose}

public record InputParameter(@NotNull ParameterType type, @NotNull String self, @NotNull String value) {
      @Override
      public boolean equals(Object other) {
	    boolean result = false;
	    if (other instanceof InputParameter param) {
		  result = param.type.equals(this.type) &&
			       param.self.equals(this.self); //only name and the type
	    }
	    return result;
      }
}

public enum UserInput {List, Install, Repository, Remove, Publish, Exit, Unknown}

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

private boolean isValidName(char letter, boolean strictMode) {
      boolean response = (strictMode && letter != '"') ||
			 (!strictMode && letter != ' ' && letter != '-' && letter != '=');
      return response;
}
private String strictSlice(char[] letters, int offset){
      if (letters[offset] != '"')
	    return "";
      StringBuilder slice = new StringBuilder();
      int i = offset + 1;
      while (i < letters.length && letters[i] != '"'){
	    slice.append(letters[i]);
	    i += 1;
      }
      return slice.toString();
}
private boolean isValidValue(char letter) {
      return letter != ' ' && letter != '-';
}

private @NotNull List<InputParameter> collectParams(String source) {
      List<InputParameter> params = new ArrayList<>();
      //search of short parameters
      final int RAW_PARAMETER = 0;
      final int SHORT_PARAMETER = 1;
      final int VERBOSE_PARAMETER = 2;
      char[] letters = source.toCharArray();
      for (int i = 0; i < letters.length; ) {
	    int paramIndex = RAW_PARAMETER; //none
	    boolean strictMode = false;
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
	    if (letters[i] == '"' && paramIndex == RAW_PARAMETER) {
		  String slice = strictSlice(letters, i);
		  i += slice.length() + 2;
		  nameBuilder.append(slice);
		  strictMode = true;
	    }
	    while (!strictMode && i < letters.length && isValidName(letters[i], false)) { //attempt to extract self parameter name
		  nameBuilder.append(letters[i]);
		  i += 1;
	    }
	    //the beginning of the parameter's value
	    //it's possible only in non strict mode
	    if (!strictMode && i < letters.length && letters[i] == '=') {
		  i += 1;
		  if (letters[i] == '"'){
			String slice = strictSlice(letters, i);
			valueBuilder.append(slice);
			i += slice.length() + 2;
		  }
		  while (i < letters.length && isValidValue(letters[i])) {
			valueBuilder.append(letters[i]);
			i += 1;
		  }
	    }
	    if (!nameBuilder.isEmpty()) {
		  String name = nameBuilder.toString();
		  String value = valueBuilder.toString();
		  ParameterType type = ParameterType.values()[paramIndex];
		  params.add(new InputParameter(type, name, value));
	    }
	    //skip spaces between params
	    while (i < letters.length && letters[i] == ' ') {
		  i += 1;
	    }
      }
      return params;
}

private boolean belongsGroup(List<InputParameter> params, String[] patterns) {
      boolean isValid = true; //no params means valid
      for (InputParameter param : params) {
	    isValid = false;
	    for (String label : patterns) {
		  if (label.equals(param.self())) {
			isValid = true;
			break;
		  }
	    }
	    if (!isValid) {
		  break;
	    }
      }
      return isValid;
}

private Optional<InputGroup> verifyGroup(@NotNull InputGroup group) {
      Optional<InputGroup> result = Optional.empty();
      InputPattern pattern = patternMap.get(group.type());
      assert pattern != null;
      var labels = List.of(pattern.shortParams(), pattern.verboseParams());
      var params = List.of(group.shortParams(), group.verboseParams());
      final int VALIDATION_LENGTH = 2;
      int index = 0;
      boolean isValid = true;
      while (isValid && index < VALIDATION_LENGTH) {
	    isValid = belongsGroup(params.get(index), labels.get(index));
	    index += 1;
      }
      if (isValid)
	    result = Optional.of(group);
      return result;
}

private Optional<InputGroup> nextCommand(@NotNull String line) {
      Optional<InputGroup> promise = Optional.empty();
      InputGroup result = null;
      String[] groups = line.split("( +)", 2); //split first occurrence of params to different side
      for (InputPattern command : patternMap.values()) {
	    Matcher matcher = command.pattern().matcher(groups[0]);
	    if (matcher.find()) {
		  List<InputParameter> params = List.of();
		  if (groups.length >= 2)
			params = collectParams(groups[1]);
		  result = InputGroup.valueOf(command.type(), params);
		  break;
	    }
      }
      if (result != null)
	    promise = verifyGroup(result);
      return promise;
}

private void initCommands() throws ConfigFormatException {
      try {
	    ClassLoader classLoader = InputProcessor.class.getClassLoader();
	    Path commandsPath = Path.of(classLoader.getResource("commands.json").getPath());
	    String config = Files.readString(commandsPath);
	    InputPattern[] patterns = new Gson().fromJson(config, InputPattern[].class);
	    patternMap = new HashMap<>();
	    for (InputPattern pattern : patterns) {
		  assert pattern.type != null;
		  patternMap.put(pattern.type, pattern);
	    }
      } catch (IOException | AssertionError e) {
	    throw new ConfigFormatException("The config file is not exists or set not properly");
      }
}

private Map<UserInput, InputPattern> patternMap;
}
