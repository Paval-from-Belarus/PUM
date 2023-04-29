package org.petos.packagemanager.client;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.petos.packagemanager.client.OutputProcessor.QuestionResponse;
import org.petos.packagemanager.packages.DependencyInfoDTO;
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
import java.util.*;

import static org.petos.packagemanager.client.ClientService.*;
import static org.petos.packagemanager.client.InputProcessor.*;
import static org.petos.packagemanager.client.OutputProcessor.*;
import static org.petos.packagemanager.transfer.NetworkExchange.*;

public class Client {
public static class PackageIntegrityException extends Exception {
      PackageIntegrityException(String msg) {
	    super(msg);
      }
}

public final int LATEST_VERSION = 0;
public final int OLDEST_VERSION = -1;
Logger logger = LogManager.getLogger(Client.class);
private final String serverUri;
private final int port;
private Thread listenThread;
private Thread userThread;
private Configuration config;
private InputProcessor input;
private OutputProcessor output;

public Client(int port, String domain) throws ConfigFormatException {
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
		  case List -> onListCommand(group.params());
		  case Install -> onInstallCommand(group.params());
		  case Remove -> onRemoveCommand(group.params());
		  case Repository -> onRepositoryCommand(group.params());
	    }
      } catch (Exception e) {
	    defaultErrorHandler(e);
      }
}

private ClientService defaultService() throws ServerAccessException {
      ClientService service = new ClientService(serverUri, port);
      service.setExceptionHandler(this::defaultErrorHandler);
      return service;
}


//Multithreading intrinsic)
private void dispatchService(ClientService service) {
      service.run();
}

//this method is supposed to be multithreading
private void dispatchTask(Runnable task) {
      task.run();
}

private void onRemoveCommand(ParameterMap params) {

}

private void onRepositoryCommand(ParameterMap params) {

}

private void onInstallCommand(ParameterMap params) {
      var rawParams = params.get(ParameterType.Raw);
      if (rawParams.isEmpty() || rawParams.size() > 1)
	    throw new IllegalArgumentException("Package is not specified");
      String packageName = rawParams.get(0).self();//raw parameter has only value
      dispatchTask(() -> installTask(packageName));
}

/**
 * By information in Main dto try to resolve all dependencies
 *
 * @return all dependencies that should be installed locally (without current info)
 */


private void printPackageInfo(@NotNull FullPackageInfoDTO dto) {
      String output = String.format("%-30s %-30s %-30s %-20s\nPayloadSize: %-20d\n",
	  dto.name, dto.version, dto.licenseType, dto.payloadType, dto.payloadSize);
      System.out.print(output);

}

private boolean hasUserAgreement(@NotNull FullPackageInfoDTO info, List<FullPackageInfoDTO> dependencies) {
      printPackageInfo(info);
      QuestionResponse userResponse = output.sendQuestion("Is it ok?", QuestionType.YesNo);
      if (!userResponse.value(Boolean.class)) {
	    output.sendMessage("Installation aborted", "");
      }
      return (Boolean) userResponse.value();
}

//packageName is fully qualified name
private boolean isInstalledPackage(String packageName) {
      return false;//tricky algo to check installation
}

private boolean isInstalledPackage(Integer id, String version) {
      return false;
}

/**
 * Store package in local file system
 * and add package's info to local registry
 */
private void storeLocal(FullPackageInfoDTO info, PackageAssembly assembly) {

}

private void installPackage( ){

}
private void installDependencies(List<DependencyInfoDTO> dependencies) throws PackageIntegrityException{

}
//the main thread is for gui
//the multiple thread for dependencies
//This method is final in sequence of installation
private void startInstallation(Integer id, FullPackageInfoDTO info, List<DependencyInfoDTO> dependencies) {
      output.sendMessage("", "Installation in progress...");
      var optional = getRawAssembly(id, info.version);//latest version
      output.sendMessage("", "Verification in progress...");
      try {
	    PackageAssembly assembly;
	    if (optional.isPresent()) {
		  assembly = PackageAssembly.deserialize(optional.get());
		  output.sendMessage("", "Installation locally...");
		  storeLocal(info, assembly);
	    } else {
		  output.sendMessage("", "Failed to load package");
		  logger.warn("Package is not installed");
	    }
      } catch (PackageAssembly.VerificationException e) {
	    output.sendMessage("Verification error", e.getMessage());
      }
}

private void installTask(String packageName) {
      try {
	    var packageId = getPackageId(packageName);
	    var fullInfo =
		packageId.flatMap(id -> getFullInfo(id, LATEST_VERSION));
	    if (fullInfo.isPresent()){
		  var dto = fullInfo.get();
		  List<FullPackageInfoDTO> dependencies = resolveDependencies(dto);
		  if (hasUserAgreement(dto, dependencies)){
			startInstallation(packageId.get(), dto, Arrays.asList(dto.dependencies));
		  } else {
			output.sendMessage("", "Installation is aborted by user");
		  }
	    } else {
		  output.sendMessage("", "Package is not found");
	    }
      }
      catch (PackageIntegrityException e) {
	    output.sendError("Package integrity Error", e.getMessage());
      }
}

private Optional<byte[]> getRawAssembly(Integer id, String label) {
      Wrapper<byte[]> payload = new Wrapper<>();
      try {
	    var request = new NetworkPacket(RequestType.GetPayload, RequestCode.STR_FORMAT);
	    request.setPayload(toBytes(id), label.getBytes(StandardCharsets.US_ASCII));
	    ClientService service = defaultService();
	    service.setRequest(request)
		.setResponseHandler((r, s) -> payload.set(onRawAssemblyResponse(r, s)));
	    service.run();
      } catch (ServerAccessException e) {
	    output.sendError("", e.getMessage());
      }
      return Optional.ofNullable(payload.get());
}

private byte[] onRawAssemblyResponse(NetworkPacket response, Socket socket) {
      if (response.type() != ResponseType.Approve)
	    throw new IllegalStateException("No valid package exists");
      return response.data();
}


private void onListCommand(ParameterMap params) throws IOException {
      //params are ignored
      ClientService service = defaultService();
      var request = new NetworkPacket(RequestType.GetAll, RequestCode.NO_CODE);
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

private List<FullPackageInfoDTO> resolveDependencies(FullPackageInfoDTO info) throws PackageIntegrityException {
      assert info.dependencies != null;
      List<DependencyInfoDTO> dependencies = Arrays.stream(info.dependencies)
						 .filter(d -> !isInstalledPackage(d.packageId(), d.label()))
						 .toList();
      List<FullPackageInfoDTO> list = new ArrayList<>();
      for (var dependency : dependencies) {
	    var packageId = getPackageId(dependency.label());
	    var fullInfo = packageId.flatMap(id -> getFullInfo(id, dependency.label()));
	    if (fullInfo.isPresent()) {
		  list.add(fullInfo.get());
	    } else {
		  logger.warn("Broken dependency:" +
				  dependency.label() + " id=" + dependency.packageId());
		  throw new PackageIntegrityException("Broken dependency");
	    }
      }
      return list;
}

private Optional<Integer> getPackageId(String packageName) {
      Wrapper<Integer> id = new Wrapper<>();
      try {
	    var request = new NetworkPacket(RequestType.GetId, RequestCode.NO_CODE);
	    request.setPayload(packageName.getBytes(StandardCharsets.US_ASCII));
	    ClientService service = defaultService();
	    service.setRequest(request)
		.setResponseHandler((p, s) -> id.set(onPackageIdResponse(p, s)));
	    service.run();
      } catch (ServerAccessException e) {
	    output.sendError("", e.getMessage());
      }
      return Optional.ofNullable(id.get());
}

private int onPackageIdResponse(NetworkPacket response, Socket socket) {
      Integer result = null;
      if (response.type() == ResponseType.Approve) {
	    ByteBuffer buffer = ByteBuffer.wrap(response.data());
	    if (buffer.capacity() == 4)
		  result = buffer.getInt();
      }
      if (result == null)
	    throw new IllegalArgumentException("Server declines");
      return result;
}

private Optional<FullPackageInfoDTO> getFullInfo(Integer id, String label) {
      var request = new NetworkPacket(RequestType.GetInfo, RequestCode.STR_FORMAT);
      request.setPayload(toBytes(id), label.getBytes(StandardCharsets.US_ASCII));
      return getFullInfo(request);
}

private Optional<FullPackageInfoDTO> getFullInfo(Integer id, Integer version) {
      var request = new NetworkPacket(RequestType.GetInfo, RequestCode.INT_FORMAT);
      request.setPayload(toBytes(id), toBytes(version)); //second param is version offset
      return getFullInfo(request);
}

private String onPackageInfoResponse(NetworkPacket response, Socket socket) {
      if (response.type() != ResponseType.Approve)
	    throw new IllegalStateException("No valid package info");
      return response.stringData();
}

private Optional<FullPackageInfoDTO> getFullInfo(NetworkPacket request) {
      FullPackageInfoDTO dto = null;
      try {
	    ClientService service = defaultService();
	    Wrapper<String> info = new Wrapper<>();
	    service.setRequest(request)
		.setResponseHandler((r, s) -> info.set(onPackageInfoResponse(r, s)));
	    service.run();
	    var stringInfo = Optional.ofNullable(info.get());
	    if (stringInfo.isPresent()) {
		  dto = new Gson().fromJson(stringInfo.get(), FullPackageInfoDTO.class);
	    }
      } catch (ServerAccessException e) {
	    output.sendError("", e.getMessage());
      }
      return Optional.ofNullable(dto);
}

private void defaultErrorHandler(Exception e) {
      e.printStackTrace();
      System.out.println(e.getMessage());
}

private static byte[] toBytes(Integer value) {
      return ByteBuffer.allocate(4).putInt(value).array();
}

public static void main(String[] args) {
      try {
	    Client client = new Client(3344, "self.ip");
	    client.start();
      } catch (ConfigFormatException e) {
	    System.out.println("Some files are not valid or absent. Impossible to start client"); //probably replace exception with recreation of program
      }
}

}
