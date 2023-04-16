package org.petos.packagemanager.client;

import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.petos.packagemanager.packages.ShortPackageInfo;
import org.petos.packagemanager.transfer.NetworkPacket;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.petos.packagemanager.transfer.NetworkExchange.*;

public class Client {
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

private static record InputGroup(UserInput type, String[] params) {
}

private final String serverUri;
private final int port;
private Thread listenThread;
private Thread userThread;

public Client(int port, String domain) {
      this.serverUri = domain;
      this.port = port;
      initCommands();
}

public void start() {
      InputGroup input;
      while ((input = dispatchInput()).type() != UserInput.Exit) {
	    dispatchInputGroup(input);
      }
      System.out.println("Thanks for working with us!");
}

private InputGroup dispatchInput() {
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

private void dispatchInputGroup(@NotNull InputGroup group) {
      try {
	    switch (group.type()) {
		  case ListAll -> onListAllCommand(group.params);
		  case Install -> onInstallCommand(group.params);
	    }
      } catch (Exception e) {
	    defaultErrorHandler(e);
      }
}
private ClientService defaultService() throws IOException{
      ClientService service = new ClientService(serverUri, port);
      service.setExceptionHandler(this::defaultErrorHandler);
      return service;
}
private InputCommand[] rules;

private void initCommands() {
      try {
	    String config = Files.readString(Path.of("commands.json"));
	    this.rules = new Gson().fromJson(config, InputCommand[].class);
      } catch (IOException e) {
	    throw new RuntimeException(e);
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

private void dispatchService(ClientService service) {
      service.run();
}

private void onInstallCommand(String[] params) throws IOException {
      if (params.length < 1)
	    throw new IllegalArgumentException("Package is not specified");
      String packageName = params[0];
      int id = getPackageId(packageName);
      String info = getPackageInfo(id);
      System.out.println(info);
}

private int getPackageId(String packageName) throws IOException {
      ClientService service = defaultService();
      var request = new NetworkPacket(RequestType.GetId);
      AtomicInteger id = new AtomicInteger(0);
      request.setPayload(packageName.getBytes(StandardCharsets.US_ASCII));
      service.setRequest(request)
	  .setExceptionHandler(this::defaultErrorHandler)
	  .setResponseHandler((p, s) -> id.set(onPackageIdResponse(p, s)));
      service.run();
      return id.get();
}

private Integer onPackageIdResponse(NetworkPacket response, Socket socket) {
      assert response.payloadSize() == 4;
      ByteBuffer buffer = ByteBuffer.wrap(response.data());
      return buffer.getInt();

}
private String getPackageInfo(Integer id) throws IOException {
      ClientService service = defaultService();
      String[] info = new String[1];
      var request = new NetworkPacket(RequestType.GetInfo);
      byte[] bytes = ByteBuffer.allocate(8)
			 .putInt(id).putInt(0).array();//second param is version offset
      request.setPayload(bytes);
      service.setRequest(request)
	  .setResponseHandler((r, s) -> info[0] = onPackageInfoResponse(r, s));
      System.out.println(request);
      service.run();
      return info[0];
}
private String onPackageInfoResponse(NetworkPacket response, Socket socket){
      if(response.type() != ResponseType.Approve)
	    throw new IllegalStateException("No valid package info");
	return response.stringData();
}
private void onListAllCommand(String[] params) throws IOException {
      //params are ignored
      ClientService service = defaultService();
      var request = new NetworkPacket(RequestType.GetAll);
      service.setRequest(request)
	  .setResponseHandler(this::onListAllResponse);
      dispatchService(service);
}

private void onListAllResponse(NetworkPacket response, Socket socket) throws IOException {
      if (response.type() != ResponseType.Approve)
	    throw new IllegalStateException("No payload to print");
      String jsonInfo = response.stringData();
      ShortPackageInfo[] packages = new Gson().fromJson(jsonInfo, ShortPackageInfo[].class);
      printShortInfo(packages);
}

private void printShortInfo(ShortPackageInfo[] packages) {
      System.out.format("Available packages\n | %40s | %40s | %40s\n",
	  "Package name", "Payload type", "Repository");
      for (ShortPackageInfo info : packages) {
	    System.out.format("| %40s | %40s | %40s", info.name(), info.payloadType(),
		"PetOS Central");
      }

}

private void defaultErrorHandler(Exception e) {
      e.printStackTrace();
      System.out.println(e.getMessage());
}

public static void main(String[] args) {
      Client client = new Client(3344, "self.ip");
      client.start();
}

}
