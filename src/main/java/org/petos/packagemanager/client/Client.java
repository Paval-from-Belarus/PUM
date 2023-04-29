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
import java.util.*;

import static org.petos.packagemanager.client.InputProcessor.*;
import static org.petos.packagemanager.client.OutputProcessor.*;
import static org.petos.packagemanager.transfer.NetworkExchange.*;

public class Client {
public static class PackageIntegrityException extends Exception {
      PackageIntegrityException(String msg) {
	    super(msg);
      }
}

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
private List<FullPackageInfoDTO> resolveDependencies(FullPackageInfoDTO info) {
      if (isInstalledPackage(info.name) || info.dependencies == null)
	    return List.of();
      var dependencies = Arrays.stream(info.dependencies)
			     .filter(d -> !isInstalledPackage(d.label()))
			     .toList();
      try {
	    for (var dependency : dependencies) {
		  var optional = getPackageId(dependency.label());
		  if (optional.isPresent()) {
			var id = optional.get();
		  } else {
//			throw new PackageIntegrityException
		  }
	    }
      } catch (IOException e) {

      }
      return null;
}

private void printPackageInfo(@NotNull FullPackageInfoDTO dto) {
      String output = String.format("%-30s %-30s %-30s %-20s\nPayloadSize: %-20d\n",
	  dto.name, dto.version, dto.licenseType, dto.payloadType, dto.payloadSize);
      System.out.print(output);

}

private boolean hasUserAgreement(@NotNull FullPackageInfoDTO info) {
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

/**
 * Store package in local file system
 * and add package's info to local registry
 */
private void storeLocal(FullPackageInfoDTO info, PackageAssembly assembly) {

}


//the main thread is for gui
//the multiple thread for dependencies
private void acceptInstallation(FullPackageInfoDTO info, Integer id) throws IOException {
      resolveDependencies(info);
      output.sendMessage("", "Installation in progress...");
      var optional = getPackage(id, 0);//latest version
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
	    var id = getPackageId(packageName);
	    int version = 0; //latest version
	    if (id.isPresent()) {
		  String stringInfo = getPackageInfo(id.get(), version).orElse("");
		  FullPackageInfoDTO info = new Gson().fromJson(stringInfo, FullPackageInfoDTO.class);
		  if (info != null && !isInstalledPackage(info.name)) {
			List<FullPackageInfoDTO> dependencies = resolveDependencies(info);
			acceptInstallation(info, id.get());
		  } else {
			logger.info("Installation is aborted");
			if (info != null && isInstalledPackage(info.name)) {
			      output.sendMessage("Package is already installed", "");
			}
		  }
	    }
      } catch (IOException e) {
	    logger.warn("Error during package installation: " + e.getMessage());
	    throw new IllegalStateException("Impossible to install package");
      }
}

private Optional<byte[]> getPackage(Integer id, Integer version) throws IOException {
      var request = new NetworkPacket(RequestType.GetPayload, RequestCode.INT_FORMAT);
      Wrapper<byte[]> payload = new Wrapper<>();
      request.setPayload(id, version);
      ClientService service = defaultService();
      service.setRequest(request)
	  .setResponseHandler((r, s) -> payload.set(onPackageResponse(r, s)));
      service.run();
      return Optional.ofNullable(payload.get());
}

private byte[] onPackageResponse(NetworkPacket response, Socket socket) {
      if (response.type() != ResponseType.Approve)
	    throw new IllegalStateException("No valid package exists");
      return response.data();
}

private Optional<Integer> getPackageId(String packageName) throws IOException {
      Wrapper<Integer> id = new Wrapper<>();
      var request = new NetworkPacket(RequestType.GetId, RequestCode.NO_CODE);
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
      if (result == null)
	    throw new IllegalArgumentException("Server declines");
      return result;
}

private Optional<String> getPackageInfo(Integer id, Integer version) throws IOException {
      ClientService service = defaultService();
      Wrapper<String> info = new Wrapper<>();
      var request = new NetworkPacket(RequestType.GetInfo, RequestCode.INT_FORMAT);
      request.setPayload(id, version); //second param is version offset
      service.setRequest(request)
	  .setResponseHandler((r, s) -> info.set(onPackageInfoResponse(r, s)));
      service.run();
      return Optional.ofNullable(info.get());
}

private String onPackageInfoResponse(NetworkPacket response, Socket socket) {
      if (response.type() != ResponseType.Approve)
	    throw new IllegalStateException("No valid package info");
      return response.stringData();
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

private void defaultErrorHandler(Exception e) {
      e.printStackTrace();
      System.out.println(e.getMessage());
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
