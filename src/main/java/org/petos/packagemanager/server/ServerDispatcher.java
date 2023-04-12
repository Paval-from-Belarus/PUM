package org.petos.packagemanager.server;

import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;
import org.petos.packagemanager.*;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerDispatcher implements ServerController {

private byte[] getPayload(Integer packageId) {
      return new byte[0];
}

@Override
public void accept(NetworkExchange exchange) {

}

@Override
public void error(NetworkExchange exchange) {
      ServerController.super.error(exchange);
}

class ClientData {
      private CommandType lastCommand;
      private int repeatCnt; //the num of curr command
      private PackageEntity data;
      private boolean isBlocked;

      ClientData() {
	    lastCommand = CommandType.Unknown;
	    markRepeater();
	    isBlocked = true;
      }

      public void setPackageEntity(PackageEntity entity) {
	    this.data = entity;
      }

      public PackageEntity getPackageEntity() {
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
private void dispatchPackageRequest(DataInputStream input) throws IOException {
      if (client.getLast() != CommandType.GetId) {
	    client.setLast(CommandType.GetId);
	    client.markRepeater();
      }
      response = new NetworkPacket(CommandType.Approve);//if something go wrong -> Decline response
      switch (client.getRepeater()) {
	    case 0 -> {
		  int id = Integer.parseInt(input.readUTF());
		  int version = Integer.parseInt(input.readUTF());
		  PackageEntity entity = getPackage(id, version);
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
	    response.setHeaders(entry.getValue().toJson());
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

public ServerDispatcher(){}
}
