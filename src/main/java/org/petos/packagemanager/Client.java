package org.petos.packagemanager;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;
import java.util.regex.Pattern;

public class Client {
public enum UserInput {ListAll, Install, Exit}
private final String domain;
private final int port;
private Thread listenThread;
private Thread userThread;

public Client(int port, String domain) {
      this.domain = domain;
      this.port = port;
      initCommands();
}

public void start() {

}


private void dispatchInput() {
      Scanner input = new Scanner(System.in);
      String line = input.nextLine();

}
public static void main(String[] args) {
      Client client = new Client(3344, "self.ip");
      client.start();
}
private CommandRule[] commandRules;
private void initCommands() {
      try (BufferedReader reader = new BufferedReader(new FileReader("commands.json"))) {
	    StringBuilder strText = new StringBuilder();
	    String line;
	    while ((line = reader.readLine()) != null)
		  strText.append(line);
	    Gson gson = new Gson();
	    InputCommand[] commands = gson.fromJson(strText.toString(), InputCommand[].class);
	    this.commandRules = Arrays.stream(commands)
				    .map(CommandRule::new)
				    .toArray(CommandRule[]::new);

      } catch (IOException e) {
	    throw new RuntimeException(e);
      }
}
private class CommandRule extends InputCommand {
      public CommandRule(InputCommand command) {
	    this.pattern = command.pattern;
	    this.type = command.type;
	    this.hasParams = command.hasParams;
      }

      public Pattern pattern() {
	    return Pattern.compile(this.pattern);
      }
}
private class InputCommand {
      public enum CommandType {ListAll, Install}
      public String pattern;
      public CommandType type;
      public boolean hasParams;
}
}
