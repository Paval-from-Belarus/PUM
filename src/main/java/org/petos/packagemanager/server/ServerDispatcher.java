package org.petos.packagemanager.server;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.petos.packagemanager.PackageAssembly;
import org.petos.packagemanager.PackageHeader;
import org.petos.packagemanager.PackageInfo;
import org.petos.packagemanager.Server;
import org.petos.packagemanager.transfer.NetworkExchange;
import org.petos.packagemanager.transfer.PackageRequest;
import org.petos.packagemanager.transfer.ShortPackageInfo;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.petos.packagemanager.transfer.NetworkExchange.*;

public class ServerDispatcher implements ServerController {
private static Logger logger = LogManager.getLogger(Server.class);
private final PackageStorage storage;

public ServerDispatcher(PackageStorage storage) {
      this.storage = storage;
}

private byte[] getPayload(Integer packageId) {
      return new byte[0];
}

@Override
public void accept(NetworkExchange exchange) throws Exception {
      RequestType request = (RequestType) exchange.request().type();
      switch (request) {
	    case GetAll -> onAllPackages(exchange);
	    case GetId -> onPackageId(exchange);
	    case GetInfo -> onPackageInfo(exchange);
	    case GetPayload -> onPayload(exchange);
	    case PublishInfo -> onPublishInfo(exchange);
	    case PublishPayload -> onPublishPayload(exchange);
	    case UpgradeVersion -> onUpgradeVersion(exchange);
	    default -> throw new IllegalStateException("Illegal command");
      }
}

@Override
public void error(NetworkExchange exchange) {
      ServerController.super.error(exchange);
}

private void onAllPackages(NetworkExchange exchange) throws IOException {
      var keys = storage.keyList();
      var packages = keys.stream()
			 .map(storage::getShortInfo)
			 .filter(Optional::isPresent)
			 .map(Optional::get)
			 .toArray(ShortPackageInfo[]::new);
      try (DataOutputStream output = new DataOutputStream(exchange.getOutputStream())) {
	    String response = new Gson().toJson(packages);
	    output.writeUTF(response);
      }
      exchange.setResponse(ResponseType.Approve, ALL_PACKAGES_RESPONSE);

}

private void onPackageId(NetworkExchange exchange) {
      String alias = exchange.request().stringPacket();
      var optional = storage.getPackageId(alias);
      if (optional.isPresent()) {
	    ByteBuffer buffer = ByteBuffer.allocate(4);
	    buffer.putInt(optional.get().value());
	    exchange.setResponse(ResponseType.Approve, PACKAGE_ID_RESPONSE, buffer.array());
      } else {
	    exchange.setResponse(ResponseType.Decline, FORBIDDEN);
      }
}

private Optional<PackageRequest> onPackageRequest(NetworkExchange exchange) {
      ByteBuffer buffer = ByteBuffer.wrap(exchange.request().data());
      Optional<PackageRequest> result = Optional.empty();
      int id = buffer.getInt();
      int version = buffer.getInt();
      var optional = storage.getPackageId(id);
      if (optional.isPresent()) {
	    var packageId = optional.get();
	    var versionId = storage.mapVersion(packageId, version);
	    result = Optional.of(new PackageRequest(packageId, versionId));
      }
      return result;
}

private void onPackageInfo(NetworkExchange exchange) {
      var optional = onPackageRequest(exchange);
      if (optional.isPresent()) {
	    PackageRequest request = optional.get();
	    var info = storage.getFullInfo(request.id(), request.version());
	    if (info.isPresent()) {
		  String response = info.get().toJson();
		  exchange.setResponse(
		      ResponseType.Approve, PACKAGE_INFO_FORMAT,
		      response.getBytes(StandardCharsets.US_ASCII));
	    } else {
		  exchange.setResponse(ResponseType.Decline, NO_PAYLOAD);
	    }
      } else {
	    exchange.setResponse(ResponseType.Decline, FORBIDDEN);
      }
}

private void writeBytes(NetworkExchange exchange, @NotNull byte[] payload) throws IOException {
      try (DataOutputStream output = new DataOutputStream(exchange.getOutputStream())) {
	    output.write(payload);
      }
}

private void onPayload(NetworkExchange exchange) throws IOException {
      var optional = onPackageRequest(exchange);
      if (optional.isPresent()) {
	    var request = optional.get();
	    Optional<byte[]> payload = storage.getPayload(request.id(), request.version());
	    PackageHeader header = new PackageHeader(request.id().value(), request.version().value());
	    if (payload.isPresent()) {
		  var assembly = PackageAssembly.valueOf(header, payload.get());
		  writeBytes(exchange, assembly.serialize());
		  exchange.setResponse(ResponseType.Approve, PACKAGE_PAYLOAD_FORMAT);
	    } else {
		  exchange.setResponse(ResponseType.Decline, NO_PAYLOAD);
	    }
      } else {
	    exchange.setResponse(ResponseType.Decline, FORBIDDEN);
      }
}

private void onPublishInfo(NetworkExchange exchange) {
      String jsonInfo = exchange.request().stringPacket();
      PackageInfo info = PackageInfo.fromJson(jsonInfo);
      try {
	    var id = storage.storePackage(info);
	    var buffer = ByteBuffer.allocate(4);
	    buffer.putInt(id.value());
	    exchange.setResponse(ResponseType.Approve, PUBLISH_INFO_RESPONSE,
		buffer.array());
      } catch (PackageStorage.StorageException e) {
	    String error = e.getMessage();
	    exchange.setResponse(ResponseType.Decline, VERBOSE_FORMAT,
		error.getBytes(StandardCharsets.US_ASCII));
      }
}

private void onPublishPayload(NetworkExchange exchange) {

}

private void onUpgradeVersion(NetworkExchange exchange) {

}
}