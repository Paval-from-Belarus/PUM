package org.petos.packagemanager.server;

import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;
import org.petos.packagemanager.*;
import org.petos.packagemanager.transfer.NetworkExchange;
import org.petos.packagemanager.transfer.NetworkPacket;

import java.io.*;
import java.util.Optional;

public class ServerDispatcher implements ServerController {
private final PackageStorage storage;
public ServerDispatcher(PackageStorage storage){
      this.storage = storage;
}
private byte[] getPayload(Integer packageId) {
      return new byte[0];
}

@Override
public void accept(NetworkExchange exchange) throws Exception {
      NetworkExchange.RequestType request = (NetworkExchange.RequestType) exchange.request().type();
      switch(request){
	    case GetAll -> onAllPackagesHandler(exchange);
	    case GetId -> {
	    }
	    case Publish -> {
	    }
	    case GetInfo -> {
	    }
	    case GetPayload -> {
	    }
	    default ->
		  throw new IllegalStateException("Illegal command");
      }
}

@Override
public void error(NetworkExchange exchange) {
      ServerController.super.error(exchange);
}
class ClientData {
      private CommandType lastCommand;
      private int repeatCnt; //the num of curr command
      private PackageAssembly data;
      private boolean isBlocked;

      ClientData() {
	    lastCommand = CommandType.Unknown;
	    markRepeater();
	    isBlocked = true;
      }

      public void setPackageEntity(PackageAssembly entity) {
	    this.data = entity;
      }

      public PackageAssembly getPackageEntity() {
	    return data;
      }

      public void block() {
	    isBlocked = true;
      }

      public void unlock() {
	    isBlocked = false;
      }

      public boolean isBlocked() {
	    return isBlocked;
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

private void onAllPackagesHandler(NetworkExchange exchange) throws IOException {
      var keys = storage.keyList();
      var packages = keys.stream()
		     .map(storage::getShortInfo)
		     .filter(Optional::isPresent)
		     .map(Optional::get)
		     .toArray(ShortPackageInfo[]::new);
      try(DataOutputStream output = new DataOutputStream(exchange.getOutputStream())){
	    String response = new Gson().toJson(packages);
	    output.writeUTF(response);
      }
      exchange.setResponse(NetworkExchange.ResponseType.Approve, NetworkExchange.ALL_PACKAGES_RESPONSE);

}
private void onPackageIdHandler(NetworkExchange exchange){
      String alias = exchange.request().
      var optional = storage.getPackageId
}
private void dispatchPackageRequest(NetworkExchange exchange) throws IOException {
      if (client.getLast() != CommandType.GetId) {
	    client.setLast(CommandType.GetId);
	    client.markRepeater();
      }
      response = new NetworkPacket(CommandType.Approve);//if something go wrong -> Decline response
      switch (client.getRepeater()) {
	    case 0 -> {
		  int id = Integer.parseInt(input.readUTF());
		  int version = Integer.parseInt(input.readUTF());
		  PackageAssembly entity = getPackage(id, version);
		  client.setPackageEntity(entity);
		  client.block();
	    }
	    case 1 -> client.unlock();
      }
      client.updateRepeater();

}

private void dispatchPackageResponse(DataOutputStream output) throws IOException {
      output.write(response.rawPacket());
      if (!client.isBlocked()) {
	    String entity = client.getPackageEntity().serialize();
	    output.writeUTF(entity);
	    client.setLast(CommandType.Unknown);
      }
      output.flush();
}

private void dispatchInfoRequestAll() {
      client.setLast(CommandType.GetAll);
}

private void dispatchInfoRequest(@NotNull DataInputStream input) throws IOException {
      String name = input.readUTF();
      PackageInfo info = Server.this.getPackageInfo(name);
      String data = info.toJson();
      response = new NetworkPacket(CommandType.Approve, data);
}

private void dispatchInfoResponseAll(DataOutputStream output) throws IOException {
      NetworkPacket response = new NetworkPacket(CommandType.GetAll);
      for (var entry : packages.entrySet()) {
	    response.setPayload(entry.getValue().toJson());
	    output.write(response.rawPacket());
	    output.writeUTF(response.data());
      }
      output.flush();
      client.setLast(CommandType.Unknown);
}

private void dispatchDefaultRequest(NetworkExchange exchange) {
}

private void dispatchDefaultResponse(DataOutputStream output) throws IOException {
      output.write(response.rawPacket());
      if (response.hasData())
	    output.writeUTF(response.data());
      output.flush();
}
}