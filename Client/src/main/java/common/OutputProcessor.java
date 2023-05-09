package common;

import org.jetbrains.annotations.NotNull;

import java.util.Scanner;

public class OutputProcessor {
public OutputProcessor(){
      responseInput = new Scanner(System.in);
}
public enum QuestionType {YesNo, Input}
private Scanner input;
public record QuestionResponse(Object value) {
      public <T> T value(Class<T> classType){
	    return (classType.cast(value));
      }
}
public void sendError(String title, String text){
      sendMessage(title, text);
}

public void sendMessage(String title, String text){
      String msg = String.format("%-40s\n%s\n", title.toUpperCase(), text);
      System.out.print(msg);
}

public @NotNull QuestionResponse sendQuestion(String question, QuestionType type){
      QuestionResponse response;
      response = switch (type) {
	    case YesNo -> processYesNo(question);
	    case Input -> processInput(question);
      };
      return response;
}
private QuestionResponse processInput(String text) {
      System.out.format("%s:", text);
      return new QuestionResponse(getResponse());
}
private QuestionResponse processYesNo(String text){
      System.out.format("%s [y/n]:", text);
      String response;
      boolean isProcessed;
      boolean isApproved;
      do {
	    response = getResponse();
	    isProcessed = (isApproved = isAccept(response)) || isDecline(response);
	    if (!isProcessed){
		  System.out.println("Please, verify correctness[y/n]:");
	    }
      } while(!isProcessed);
      return new QuestionResponse(isApproved);
}
private boolean isAccept(String line){
      return line.equalsIgnoreCase("y");
}
private boolean isDecline(String line){
      return line.equalsIgnoreCase("n");
}
private String getResponse(){
      return responseInput.nextLine();
}
private final Scanner responseInput;
}
