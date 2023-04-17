package org.petos.packagemanager.client;

import org.jetbrains.annotations.NotNull;

public class OutputProcessor {
public static final int YES_NO = 1;
public static final int INPUT = 2;

public record QuestionResponse(Object value) {}
public void sendMessage(){

}
public @NotNull QuestionResponse sendQuestion(String question, int type){
 	return new QuestionResponse(false);
}
}
