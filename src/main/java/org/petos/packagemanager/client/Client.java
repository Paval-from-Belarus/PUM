package org.petos.packagemanager.client;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.petos.packagemanager.client.OutputProcessor.QuestionResponse;
import org.petos.packagemanager.client.storage.InstallerSession;
import org.petos.packagemanager.client.storage.PackageStorage;
import org.petos.packagemanager.client.storage.RemovableSession;
import org.petos.packagemanager.packages.DependencyInfoDTO;
import org.petos.packagemanager.packages.FullPackageInfoDTO;
import org.petos.packagemanager.packages.PackageAssembly;
import org.petos.packagemanager.packages.ShortPackageInfoDTO;
import org.petos.packagemanager.transfer.NetworkPacket;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.petos.packagemanager.client.ClientService.ServerAccessException;
import static org.petos.packagemanager.client.InputProcessor.*;
import static org.petos.packagemanager.client.OutputProcessor.QuestionType;
import static org.petos.packagemanager.client.StorageSession.*;
import static org.petos.packagemanager.client.storage.PackageStorage.*;
import static org.petos.packagemanager.transfer.NetworkExchange.*;

public class Client {
/**
 * <code>Selective</code> mode is used to install package (select dependencies that are not installed)<br>
 * <code>Local</code> mode is used to collect all available local dependencies<br>
 * <code>Complete</code> mode is used to complete all dependencies whereas installed or not
 */
public enum ResolutionMode {
      Remote, Local, Removable;
      private PackageStorage storage = null;
      public static ResolutionMode valueOf(ResolutionMode mode, PackageStorage storage){
	    mode.storage = storage;
	    return mode;
      }
      public boolean isSuitable(DependencyInfoDTO dto) {
	    if (storage == null)
		  return false;
	    InstallationState state = storage.getPackageState(dto.packageId(), dto.label());
	    boolean response = false;
	    switch (this) {
		  case Remote -> response = state.isRemote();
		  case Local -> response = state.isLocal();
		  case Removable -> response = state.isRemovable();
	    }
	    return response;
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
private PackageStorage storage;
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
	    this.config.init();
	    storage = new PackageStorage(this.config);//throws
//	    Class<?> storageClass = storage.getClass();
//	    var methods = storageClass.getDeclaredMethods();
//	    for (var method : methods){
//		  System.out.println(method.getName() + ":" + method.getReturnType());
//		  System.out.println(method.isSynthetic());
//	    }
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
      List<InputParameter> removables = params.get(ParameterType.Raw);
      if (removables.isEmpty())
	    throw new IllegalStateException("The package is not specified");
      for (var removable : removables){
	    removePackage(removable.self());
      }
}

private void removePackage(String name) {
      InstallationState state = storage.getPackageState(name);
      if (state.isRemovable()) {
	    try (var session = storage.initSession(RemovableSession.class)) {
		  var fullInfo = storage.getFullInfo(name);
		  if (fullInfo.isPresent()) {
			//the dependency resotion is obligatory only to determine the integrity of package
			ResolutionMode mode = ResolutionMode.valueOf(ResolutionMode.Removable, storage);
			Map<Integer, FullPackageInfoDTO> dependencies = resolveDependencies(fullInfo.get(), mode);//only existing dependencies
			session.removeLocally(fullInfo.get());
			for (var dto : dependencies.values()) {
			      session.removeLocally(dto);
			}
			session.commit(CommitState.Success);
		  } else {
			output.sendError("", String.format("The package %s is damaged", name));
		  }
	    } catch (PackageIntegrityException e) {
		  output.sendError("Broken dependencies", e.getMessage());
	    } catch (ConfigurationException e) {
		  output.sendError("", "Package configuration error");
	    }
      } else {
	    output.sendError("", String.format("The package %s is not installed or cannot be removed", name));
      }

}

private void onRepositoryCommand(ParameterMap params) {

}

private void onListCommand(ParameterMap params) throws IOException {
      boolean isVerboseMode = params.hasParameter(ParameterType.Shorts, "v") ||
				  params.hasParameter(ParameterType.Verbose, "verbose");
      if (params.hasParameter(ParameterType.Verbose, "installed")) {
	    showPackagesLocally(isVerboseMode);
      } else {
	    showPackagesAll();
      }

}

private void showPackagesAll() {
      try {
	    ClientService service = defaultService();
	    var request = new NetworkPacket(RequestType.GetAll, RequestCode.NO_CODE);
	    service.setRequest(request)
		.setResponseHandler(this::onListAllResponse);
	    service.run();
      } catch (ServerAccessException e) {
	    output.sendError("Network error", "Server is busy");
      }
}

private void showPackagesLocally(boolean isVerbose) {
      System.out.println("Installed packages");
      final int DUMMY_ID = 0;
      var packages = storage.localPackages();
      if (isVerbose) {
	    packages.forEach(this::printPackageInfo);
      } else {
	    packages.stream()
		.map(full -> ShortPackageInfoDTO.valueOf(DUMMY_ID, full))
		.forEach(this::printShortInfo);
      }
      if (storage.getLastStatus() != OperationStatus.Success) {
	    output.sendMessage("Integrity warning", "some packages are broken");
      }
}

private void onListAllResponse(NetworkPacket response, Socket socket) throws IOException {
      if (response.type() != ResponseType.Approve)
	    throw new IllegalStateException("No payload to print");
      String jsonInfo = response.stringData();
      ShortPackageInfoDTO[] packages = new Gson().fromJson(jsonInfo, ShortPackageInfoDTO[].class);
      System.out.format("Available packages:\n");
      for (ShortPackageInfoDTO info : packages) {
	    printShortInfo(info);
      }
}

private void onInstallCommand(ParameterMap params) {
      var rawParams = params.get(ParameterType.Raw);
      if (rawParams.isEmpty() || rawParams.size() > 1)
	    throw new IllegalArgumentException("Package is not specified");

      String packageName = rawParams.get(0).self();//raw parameter has only value
      var state = storage.getPackageState(packageName);
      if (state.isRemote())
	    installPackage(packageName);
      else {
	    output.sendMessage("", "Package is already installed");
      }
}

/**
 * By information in Main dto try to resolve all dependencies
 *
 * @return all dependencies that should be installed locally (without current info)
 */

private void printPackageInfo(@NotNull FullPackageInfoDTO dto) {
      String output = String.format("%-40s\t%-15s\t%-10s\t%-10s\t%-30d\n",
	  dto.name, dto.version, dto.payloadType, dto.licenseType, dto.payloadSize);
      System.out.print(output);

}

private boolean hasUserAgreement(@NotNull FullPackageInfoDTO info, @NotNull Collection<FullPackageInfoDTO> dependencies) {
      printPackageInfo(info);
      output.sendMessage("The additional dependencies", "");
      long totalSize = info.payloadSize;
      for (var dto : dependencies) {
	    printPackageInfo(dto);
	    totalSize += dto.payloadSize;
      }
      output.sendMessage("The total size", String.format("%d", totalSize));
      QuestionResponse userResponse = output.sendQuestion("Is it ok?", QuestionType.YesNo);
      return (Boolean) userResponse.value();
}

//this method install dependencies according the common conventional rules
private void storeDependencies(InstallerSession session, Map<Integer, FullPackageInfoDTO> dependencies) throws PackageIntegrityException {
      //todo: rewrite onto parallel stream or dispatchTask methods
      PackageAssembly assembly;
      try {
	    for (var entry : dependencies.entrySet()) {
		  var payload = getRawAssembly(entry.getKey(), entry.getValue().version);
		  if (payload.isPresent()) {
			assembly = PackageAssembly.deserialize(payload.get());
			session.storeLocally(entry.getValue(), assembly);
		  }
	    }
      } catch (PackageIntegrityException | PackageAssembly.VerificationException e) {
	    throw new PackageIntegrityException("Cannot install package: " + e.getMessage());
      }
}

//the main thread is for gui
//the multiple thread for dependencies
//This method is final in sequence of installation
private void startInstallation(Integer id, FullPackageInfoDTO info, Map<Integer, FullPackageInfoDTO> dependencies) {
      output.sendMessage("", "Installation in progress...");
      PackageAssembly assembly;
      CommitState commitState = CommitState.Failed;
      try (var session = storage.initSession(InstallerSession.class)) {
	    output.sendMessage("", "Dependency installation...");
	    storeDependencies(session, dependencies);
	    var optional = getRawAssembly(id, info.version);//latest version
	    output.sendMessage("", "Verification in progress...");
	    if (optional.isPresent()) {
		  assembly = PackageAssembly.deserialize(optional.get());
		  output.sendMessage("", "Installation locally...");
		  session.storeLocally(info, assembly);
		  output.sendMessage("", "Local transactions are running...");
		  commitState = CommitState.Success;
	    } else {
		  output.sendMessage("", "Failed to load package");
		  logger.warn("Package is not installed");
	    }
	    session.commit(CommitState.Success);
	    if (commitState == CommitState.Success)
		  output.sendMessage("Congratulations!", "Installed successfully!");
      } catch (PackageAssembly.VerificationException e) {
	    output.sendMessage("Verification error", e.getMessage());
      } catch (PackageIntegrityException e) {
	    logger.warn("Broken dependencies: " + e.getMessage());
	    output.sendError("Broken dependencies", e.getMessage());
      } catch (ConfigurationException e) {
	    output.sendError("Configuration error", "Impossible to start installation");
      }
}

private void installPackage(String packageName) {
      Optional<Integer> packageId = Optional.empty();
      if (storage.getPackageState(packageName) == InstallationState.Cached) {
	    packageId = storage.getCachedInfo(packageName)
			    .map(ShortPackageInfoDTO::id);
      }
      if (packageId.isEmpty())
	    packageId = getPackageId(packageName);
      Optional<FullPackageInfoDTO> fullInfo = packageId.flatMap(id -> getFullInfo(id, LATEST_VERSION));
      try {
	    if (fullInfo.isPresent()) {
		  FullPackageInfoDTO dto = fullInfo.get();
		  ResolutionMode mode = ResolutionMode.valueOf(ResolutionMode.Remote, storage);
		  Map<Integer, FullPackageInfoDTO> dependencies = resolveDependencies(dto, mode);
		  if (hasUserAgreement(dto, dependencies.values())) {
			startInstallation(packageId.get(), dto, dependencies);
		  } else {
			output.sendMessage("", "Installation is aborted by user");
		  }
	    } else {
		  output.sendMessage("", "Package is not found");
	    }
      } catch (PackageIntegrityException e) {
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


private void printShortInfo(ShortPackageInfoDTO info) {
      System.out.format("%-40s\t%-10s\t%-30s\n",
	  info.name(), info.version(), "PetOS Central");
}

/**
 * convert Entries from FullPackageInfoDTO to Dependencies Map
 */
private Map<Integer, FullPackageInfoDTO> resolveDependencies(FullPackageInfoDTO info, ResolutionMode mode) throws PackageIntegrityException {
      assert info.dependencies != null;
      List<DependencyInfoDTO> dependencies = Arrays.stream(info.dependencies)
						 .filter(mode::isSuitable)
						 .toList();
      Map<Integer, FullPackageInfoDTO> dependencyMap = new HashMap<>();
      for (var dependency : dependencies) {
	    var fullInfo = storage.getFullInfo(dependency.packageId(), dependency.label()); //attempt to fetch data from local database
	    if (fullInfo.isEmpty())
		  fullInfo = getFullInfo(dependency.packageId(), dependency.label()); //fetch the info from remote server
	    if (fullInfo.isPresent()) {
		  dependencyMap.put(dependency.packageId(), fullInfo.get());
		  dependencyMap.putAll(resolveDependencies(fullInfo.get(), mode));
	    } else {
		  logger.warn("Broken dependency:" +
				  dependency.label() + " id=" + dependency.packageId());
		  throw new PackageIntegrityException("Broken dependency");
	    }
      }
      return dependencyMap;
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
	    if (dto != null && dto.aliases == null)
		  dto.aliases = new String[0];
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
