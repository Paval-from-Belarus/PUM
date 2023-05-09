package common;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import packages.*;
import security.Author;
import transfer.NetworkExchange;
import transfer.NetworkExchange.RequestType;
import transfer.NetworkPacket;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static common.PackageStorage.*;
import static transfer.NetworkExchange.*;
import static transfer.NetworkExchange.PACKAGE_ID_RESPONSE;


public class ServerDispatcher implements ServerController {
private static final Logger logger = LogManager.getLogger(Server.class);
private final PackageStorage storage;

public ServerDispatcher(PackageStorage storage) {
      this.storage = storage;
}

@Override
public void accept(NetworkExchange exchange) throws Exception {
      RequestType request = (RequestType) exchange.request().type();
      switch (request) {
	    case GetAll -> onAllPackages(exchange);
	    case GetId -> onPackageId(exchange);
	    case GetInfo -> onPackageInfo(exchange);
	    case GetPayload -> onPayload(exchange);
	    case GetVersion -> onVersionInfo(exchange);
	    case PublishInfo -> onPublishInfo(exchange);
	    case PublishPayload -> onPublishPayload(exchange);
	    case Authorize -> onAuthorizeRequest(exchange);
	    case DeprecateVersion -> onDeprecateVersion(exchange);
	    default -> throw new IllegalStateException("Illegal command");
      }
}

@Override
public void error(NetworkExchange exchange) {
      ServerController.super.error(exchange);
}

private void onAllPackages(NetworkExchange exchange) throws IOException {
      var packages = storage.shortInfoList()
			 .toArray(ShortPackageInfoDTO[]::new);
      OutputStream output = exchange.getOutputStream();
      String payload = toJson(packages);
      output.write(payload.getBytes(StandardCharsets.US_ASCII));
      exchange.setResponse(ResponseType.Approve, ALL_PACKAGES_RESPONSE);
}

private void onPackageId(NetworkExchange exchange) {
      String alias = exchange.request().stringData();
      var optional = storage.getPackageId(alias);
      if (optional.isPresent()) {
	    ByteBuffer buffer = ByteBuffer.allocate(4);
	    buffer.putInt(optional.get().value());
	    exchange.setResponse(ResponseType.Approve, PACKAGE_ID_RESPONSE, buffer.array());
      } else {
	    exchange.setResponse(ResponseType.Decline, FORBIDDEN);
      }
}

/**
 * There method resolves three kinds of request:<br><ol>
 * <li>PackageId handle and Version label</li>
 * <li>PackageId handle and Version offset (user available format to choose version)</li>
 * </ol>
 * VersionId handle is forbidden (during future updates)
 * How this should be implemented? By request format.
 * If <code>request.code()</code> equals <code>INT_FORMAT</code>, that is VersionOffset. Otherwise,
 * (if code is <code>STR_FORMAT</code>) the VersionLabel is assumed
 */
private Optional<PackageRequest> onPackageRequest(NetworkExchange exchange) {
      ByteBuffer buffer = ByteBuffer.wrap(exchange.request().data());
      Optional<PackageRequest> result = Optional.empty();
      int id = buffer.getInt();
      var optional = storage.getPackageId(id);
      if (optional.isPresent()) {
	    var packageId = optional.get();
	    Optional<VersionId> versionId;
	    switch (exchange.request().code()) {
		  case RequestCode.INT_FORMAT -> {
			int offset = buffer.getInt();
			versionId = storage.mapVersion(packageId, offset);
		  }
		  case RequestCode.STR_FORMAT -> {
			String label = exchange.request().stringFrom(4);//int offset
			versionId = storage.mapVersion(packageId, label);
		  }
		  default -> versionId = Optional.empty();
	    }
	    //if version id is correct, return not empty optional
	    result = versionId.map(v -> new PackageRequest(packageId, v));
      }
      return result;
}

private void onPackageInfo(NetworkExchange exchange) {
      var optional = onPackageRequest(exchange);
      if (optional.isPresent()) {
	    PackageRequest request = optional.get();
	    var info = storage.getFullInfo(request.id(), request.version());
	    if (info.isPresent()) {
		  String response = toJson(info.get());
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


/**
 * Specific request to check the version integrity
 * The versionInfo can be replaced by GetInfo. But it's supposed to be more suitable and fast
 */
private void onVersionInfo(NetworkExchange exchange) {
      var optional = onPackageRequest(exchange);
      if (optional.isPresent()) {
	    var request = optional.get();
	    var fullInfo = storage.getFullInfo(request.id(), request.version());
	    if (fullInfo.isPresent()) {
		  VersionInfoDTO versionInfo = new VersionInfoDTO(
		      request.version().value(),
		      fullInfo.get().version
		  );
		  String response = toJson(versionInfo);
		  exchange.setResponse(ResponseType.Approve, VERSION_INFO_FORMAT,
		      response.getBytes(StandardCharsets.US_ASCII));
	    } else {
		  exchange.setResponse(ResponseType.Decline, NO_PAYLOAD);
	    }
      } else {
	    exchange.setResponse(ResponseType.Decline, FORBIDDEN);
      }
}

private void writeBytes(NetworkExchange exchange, @NotNull byte[] payload) throws IOException {
      DataOutputStream output = new DataOutputStream(exchange.getOutputStream());
      output.write(payload);
}

private byte[] readBytes(NetworkExchange exchange, Integer cbRead) throws IOException {
      DataInputStream input = new DataInputStream(exchange.getInputStream());
      return input.readNBytes(cbRead);
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
		  exchange.setResponse(ResponseType.Approve, PAYLOAD_FORMAT);
	    } else {
		  exchange.setResponse(ResponseType.Decline, NO_PAYLOAD);
	    }
      } else {
	    exchange.setResponse(ResponseType.Decline, FORBIDDEN);
      }
}

private void onPublishInfo(NetworkExchange exchange) {
      int rawAuthorId = exchange.request().intFrom(0);
      var authorId = storage.getAuthorId(rawAuthorId);
      if (authorId.isPresent()) {
	    String jsonInfo = exchange.request().stringFrom(4);
	    PublishInfoDTO info = fromJson(jsonInfo, PublishInfoDTO.class);
	    try {
		  var id = storage.storePackageInfo(authorId.get(), info);
		  exchange.setResponse(ResponseType.Approve, PUBLISH_INFO_RESPONSE,
		      NetworkPacket.toBytes(id.value()));
	    } catch (StorageException e) {
		  String error = e.getMessage();
		  exchange.setResponse(ResponseType.Decline, VERBOSE_FORMAT,
		      error.getBytes(StandardCharsets.US_ASCII));
	    }
      }
}

/**
 *
 */
private void onPublishPayload(NetworkExchange exchange) throws IOException {
      int rawAuthorId = exchange.request().intFrom(0);
      var authorId = storage.getAuthorId(rawAuthorId);
      if (authorId.isEmpty()) {
	    exchange.setResponse(ResponseType.Decline, FORBIDDEN);
	    return;
      }
      String jsonInfo = exchange.request().stringFrom(4);
      PackageInstanceDTO dto = fromJson(jsonInfo, PackageInstanceDTO.class);
      byte[] payload = null;
      if (dto != null) {
	    payload = readBytes(exchange, dto.getPayloadSize());
      }
      if (dto != null && payload != null) {
	    try {
		  var version = storage.storePayload(authorId.get(), dto, payload);
		  exchange.setResponse(ResponseType.Approve, PUBLISH_PAYLOAD_RESPONSE,
		      NetworkPacket.toBytes(version.value()));
	    } catch (StorageException e) {
		  exchange.setResponse(ResponseType.Decline, VERBOSE_FORMAT,
		      e.getMessage().getBytes(StandardCharsets.US_ASCII));
	    }
      } else {
	    exchange.setResponse(ResponseType.Decline, ILLEGAL_REQUEST);
      }
}

private void onAuthorizeRequest(NetworkExchange exchange) {
      String base64Line = exchange.request().stringData();
      Optional<Author> author = Author.valueOf(base64Line);
      if (author.isPresent()) {
	    Optional<AuthorId> id = storage.getAuthorId(author.get());
	    try {
		  int code = INT_FORMAT;
		  if (id.isEmpty()) { //attempt to register Author
			storage.authorize(author.get());
			id = storage.getAuthorId(author.get());
			code |= CREATED;
		  }
		  if (id.isPresent()) {
			exchange.setResponse(ResponseType.Approve, code,
			    NetworkPacket.toBytes(id.get().value()));
		  }
	    } catch (StorageException e) {
		  exchange.setResponse(ResponseType.Decline, FORBIDDEN);
	    }

      } else {
	    exchange.setResponse(ResponseType.Decline, ILLEGAL_REQUEST);
      }
}

private void onDeprecateVersion(NetworkExchange exchange) {
      // TODO: 29/04/2023
}

private static final Gson gson;

static {
      gson = new Gson();
}

private static <T> @Nullable T fromJson(String source, Class<T> classType) {
      T result = null;
      try {
	    result = gson.fromJson(source, classType);
      } catch (JsonSyntaxException e) {
	    logger.warn("Json syntax at " + source);
      }
      return result;
}

private static <T> @NotNull String toJson(T source) {
      return gson.toJson(source);
}
}