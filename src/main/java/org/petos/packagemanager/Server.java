package org.petos.packagemanager;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import org.jetbrains.annotations.NotNull;
import com.google.gson.Gson;
import org.petos.packagemanager.NetworkPacket.CommandType;

public class Server {
private int port;
private int clientCnt;
private ForkJoinPool threadPool;
private Map<Integer, PackageInfo> table;

Server() {
      threadPool = new ForkJoinPool();
      //todo: check available port and
}

public void start() {
      try (ServerSocket server = new ServerSocket(port, clientCnt)) {
	    while (!server.isClosed()) {
		  Socket client = server.accept();
		  ClientService service = new ClientService(client);
		  threadPool.submit(service);
	    }
      } catch (IOException e) {
	    throw new RuntimeException(e);
      }
}

private ConcurrentHashMap<String, Integer> nameMapper;
private ConcurrentHashMap<Integer, PackageInfo> packages;

private byte[] getPayload(Integer packageId){
      return new byte[0];
}
private @NotNull List<DataPackage> getPackageBranch(Integer id){
      PackageInfo info = packages.get(id);
      if(info == null)
	    return List.of();
      DataPackage data = new DataPackage();
      data.info = info;
      data.payload = new byte[0];
      return List.of(data);
}
private @NotNull PackageInfo getPackageInfo(String packageName) throws IllegalStateException {
      Integer id = nameMapper.get(packageName);
      if (id == null)
	    throw new IllegalStateException("Package not exists");
      PackageInfo info = packages.get(id);
      if (info == null)
	    throw new IllegalStateException("No package for corresponding id");
      return info;
}
/**
 * if version == 0: get latest version<br>
 * elsif version in [-1,-9]: get specific latter version<br>
 * else: get minimal available version<br>
 * */
private @NotNull PackageEntity getPackage(int id, int versionOffset){
	List<DataPackage> list = getPackageBranch(id);
	assert list.size() != 0;
	return null;
}
class ClientData {
      private CommandType lastCommand;
      private int repeatCnt; //the num of curr command
      private PackageEntity data;

      ClientData() {
	    lastCommand = CommandType.Unknown;
	    markRepeater();
      }
      public void setPackageEntity(PackageEntity entity){
	    this.data = entity;
      }
      public void initTransfer(){

      }

      public void markRepeater() {
	    repeatCnt = 0;
      }

      public void updateRepeater() {
	    repeatCnt += 1;
      }

      public int getRepeater() {
	    return repeatCnt;
      }

      public void setLast(CommandType type) {
	    this.lastCommand = type;
      }

      public CommandType getLast() {
	    return this.lastCommand;
      }
}

class ClientService implements Runnable {
      private final int DEFAULT_TIMEOUT = 1000;
      private Socket socket;
      private ClientData client;
      private NetworkPacket response;

      public ClientService(Socket socket) {
	    this.socket = socket;
	    client = new ClientData();
      }

      //each dispatch set response network packet

      private void dispatchPackageRequest(DataInputStream input) throws IOException {
	    if (client.getLast() != CommandType.GetId)
		  client.markRepeater();
	    switch (client.getRepeater()) {
		  case 0 -> {
			Integer id = Integer.parseInt(input.readUTF());
			Integer version = Integer.parseInt(input.readUTF());
		  }
	    }

      }

      private void dispatchInfoRequestAll() {
	    client.setLast(CommandType.GetAll);
      }

      private void dispatchInfoRequest(@NotNull DataInputStream input) throws IOException {
	    String name = input.readUTF();
	    PackageInfo info;
	    try {
		  info = Server.this.getPackageInfo(name);
		  String data = info.toJson();
		  response = new NetworkPacket(CommandType.Approve, data);
	    } catch (IllegalStateException e) {
		  response = new NetworkPacket(CommandType.Decline);
	    }
      }

      private void dispatchDefaultRequest() {
	    response = new NetworkPacket(CommandType.Decline);
      }

      private void dispatchInfoResponseAll(DataOutputStream output) throws IOException {
	    NetworkPacket response = new NetworkPacket(CommandType.GetAll);
	    for (var entry : packages.entrySet()) {
		  response.setData(entry.getValue().toJson());
		  output.write(response.code());
		  output.writeUTF(response.data());
	    }
	    output.flush();
	    client.setLast(CommandType.Unknown);
      }

      private void dispatchDefaultResponse(DataOutputStream output) throws IOException {
	    output.write(response.code());
	    output.writeUTF(response.data());
	    output.flush();
      }

      private void dispatchInput(byte[] command, DataInputStream input) throws IOException {
	    CommandType type = NetworkPacket.convertCommand(command);
	    response = null;
	    assert client != null;
	    try {
		  switch (type) {
			case Decline, Approve -> {
			}
			case GetAll -> dispatchInfoRequestAll();
			case GetName -> dispatchInfoRequest(input);
			case GetId -> dispatchPackageRequest(input);
			case GetVersion -> {
			}
			case Publish -> {}
			default -> dispatchDefaultRequest();
		  }
	    } catch (SocketTimeoutException e) {
		  dispatchDefaultRequest();
	    }

      }

      private void dispatchOutput(DataOutputStream output) throws IOException {
	    if (response == null)
		  return;
	    switch (client.getLast()) {
		  case GetAll -> dispatchInfoResponseAll(output);
		  default -> dispatchDefaultResponse(output);
	    }
      }

      private void updateCommand(byte[] command, DataInputStream input) throws IOException {
	    boolean hasRequest = false;
	    while (!hasRequest) {
		  try {
			hasRequest = input.read(command) == NetworkPacket.BytesPerCommand;
		  } catch (SocketTimeoutException ignored) {
		  }
	    }
      }

      @Override
      public void run() {
	    try (DataInputStream input = new DataInputStream(socket.getInputStream());
		 DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {
		  byte[] command = new byte[NetworkPacket.BytesPerCommand];
		  socket.setSoTimeout(DEFAULT_TIMEOUT);
		  while (!socket.isClosed()) {
			updateCommand(command, input);
			dispatchInput(command, input);
			dispatchOutput(output);
		  }
	    } catch (IOException e) {
		  throw new RuntimeException(e);
	    }

      }
}
public static void main(String[] args){
      DataPackage data = new DataPackage();
      data.info = new PackageInfo();
      data.info.name = "FarCommander";
      data.info.payloadType = "Binary";
      data.payload = new byte[]{0xF, 0xF, 0xA, 0xA};
      PackageEntity entity = PackageEntity.valueOf(data, 23, 42);
      String result = entity.serialize();
      PackageEntity.deserialize(result);
}
}
