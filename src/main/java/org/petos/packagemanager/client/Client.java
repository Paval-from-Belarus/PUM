package org.petos.packagemanager.client;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.petos.packagemanager.client.OutputProcessor.QuestionResponse;
import org.petos.packagemanager.packages.PackageAssembly;
import org.petos.packagemanager.packages.FullPackageInfoDTO;
import org.petos.packagemanager.packages.ShortPackageInfoDTO;
import org.petos.packagemanager.transfer.NetworkPacket;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.petos.packagemanager.client.InputProcessor.*;
import static org.petos.packagemanager.transfer.NetworkExchange.*;

public class Client {
Logger logger = LogManager.getLogger(Client.class);
private final String serverUri;
private final int port;
private Thread listenThread;
private Thread userThread;
private Configuration config;
private InputProcessor input;
private OutputProcessor output;

public Client(int port, String domain) {
      this.serverUri = domain;
      this.port = port;
      input = new InputProcessor();
      output = new OutputProcessor();
      initConfig();
}

private static final String CONFIG_PATH = "config.json";

private void initConfig() {
      try {
	    Path configPath = Path.of(CONFIG_PATH);
	    String content = Files.readString(configPath);
	    this.config = new Gson().fromJson(content, Configuration.class);
      } catch (IOException e) {
	    throw new RuntimeException("Impossible to proceed client without config file");
      }
}

private void checkCache() {

}

public void start() {
      InputGroup input;
      while ((input = this.input.nextGroup()).type() != UserInput.Exit) {
	    dispatchInputGroup(input);
      }
      System.out.println("Thanks for working with us!");
}


private void dispatchInputGroup(@NotNull InputGroup group) {
      try {
	    switch (group.type()) {
		  case List -> onListCommand(group.typeMap());
		  case Install -> onInstallCommand(group.typeMap());
	    }
      } catch (Exception e) {
	    defaultErrorHandler(e);
      }
}

private ClientService defaultService() throws IOException {
      ClientService service = new ClientService(serverUri, port);
      service.setExceptionHandler(this::defaultErrorHandler);
      return service;
}


//Multithreading intrinsic)
private void dispatchService(ClientService service) {
      service.run();
}

private void dispatchTask(Runnable task) {
      task.run();
}


private void onInstallCommand(Map<ParameterType, List<InputParameter>> typeMap) {
      var rawParams = typeMap.get(ParameterType.Raw);
      if (rawParams.isEmpty() || rawParams.size() > 1)
	    throw new IllegalArgumentException("Package is not specified");
      String packageName = rawParams.get(0).self();//raw parameter has only value
      dispatchTask(() -> installTask(packageName));
}

private void resolveDependencies(FullPackageInfoDTO info) {

}
private boolean isAcceptableInstallation(@NotNull FullPackageInfoDTO info){
      System.out.format("%30s | %d", info.name, info.payloadSize);
      QuestionResponse userResponse = output.sendQuestion("Is it ok?", OutputProcessor.YES_NO);
      if(!((Boolean) userResponse.value()))
	    System.out.println("Installation aborted");
      return (Boolean) userResponse.value();
}
//packageName is fully qualified name
private boolean isInstalledPackage(String packageName){
      return false;//tricky algo to check installation
}
/**Store package in local file system
 * and add package's info to local registry
 * */
private void storeLocal(FullPackageInfoDTO info, PackageAssembly assembly){
      resolveDependencies(info);

}
private void acceptInstallation(FullPackageInfoDTO info, Integer id) throws IOException{
      var optional = getPackage(id, 0);//latest version
      PackageAssembly assembly;
      //todo: if it's impossible to assembly package ― try request several times to server (many times)
      if(optional.isPresent() && (assembly = PackageAssembly.deserialize(optional.get())) != null){
	    storeLocal(info, assembly);
      } else {
	    logger.warn("Package is not installed");
      }
}

private void installTask(String packageName) {
      try {
	    var id = getPackageId(packageName);
	    int version = 0; //latest version
	    if(id.isPresent()){
		  String stringInfo = getPackageInfo(id.get(), version).orElse("");
		  FullPackageInfoDTO info = new Gson().fromJson(stringInfo, FullPackageInfoDTO.class);
		  if(info != null && !isInstalledPackage(info.name) && isAcceptableInstallation(info)){
			acceptInstallation(info, id.get());
		  } else {
			logger.info("Installation is aborted");
		  }
	    }
      } catch (IOException e) {
	    logger.warn("Error during package installation: " + e.getMessage());
	    throw new IllegalStateException("Impossible to install package");
      }
}
private Optional<byte[]> getPackage(Integer id, Integer version) throws IOException {
      var request = new NetworkPacket(RequestType.GetPayload);
      Wrapper<byte[]> payload = new Wrapper<>();
      request.setPayload(id, version);
      ClientService service = defaultService();
      service.setRequest(request)
	  .setResponseHandler((r, s) -> payload.set(onPackageResponse(r, s)));
      service.run();
      return Optional.ofNullable(payload.get());
}
private byte[] onPackageResponse(NetworkPacket response, Socket socket){
      if(response.type() != ResponseType.Approve)
	    throw new IllegalStateException("No valid package exists");
      return response.rawPacket();
}
private Optional<Integer> getPackageId(String packageName) throws IOException {
      Wrapper<Integer> id = new Wrapper<>();
      var request = new NetworkPacket(RequestType.GetId);
      request.setPayload(packageName.getBytes(StandardCharsets.US_ASCII));
      ClientService service = defaultService();
      service.setRequest(request)
	  .setResponseHandler((p, s) -> id.set(onPackageIdResponse(p, s)));
      service.run();
      return Optional.ofNullable(id.get());
}


private int onPackageIdResponse(NetworkPacket response, Socket socket) {
      Integer result = null;
      if (response.type() == ResponseType.Approve) {
	    ByteBuffer buffer = ByteBuffer.wrap(response.data());
	    if (buffer.capacity() == 4)
		  result = buffer.getInt();
      }
      if(result == null)
	    throw new IllegalArgumentException("Server declines");
      return result;
}

private Optional<String> getPackageInfo(Integer id, Integer version) throws IOException {
      ClientService service = defaultService();
      Wrapper<String> info = new Wrapper<>();
      var request = new NetworkPacket(RequestType.GetInfo);
      request.setPayload(id, version); //second param is version offset
      service.setRequest(request)
	  .setResponseHandler((r, s) -> info.set(onPackageInfoResponse(r, s)));
      System.out.println(request);
      service.run();
      return Optional.ofNullable(info.get());
}

private String onPackageInfoResponse(NetworkPacket response, Socket socket) {
      if (response.type() != ResponseType.Approve)
	    throw new IllegalStateException("No valid package info");
      return response.stringData();
}

private void onListCommand(Map<ParameterType, List<InputParameter>> typeMap) throws IOException {
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
      ShortPackageInfoDTO[] packages = new Gson().fromJson(jsonInfo, ShortPackageInfoDTO[].class);
      printShortInfo(packages);
}

private void printShortInfo(ShortPackageInfoDTO[] packages) {
      System.out.format("Available packages\n | %40s | %40s | %40s\n",
	  "Package name", "Payload type", "Repository");
      for (ShortPackageInfoDTO info : packages) {
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
