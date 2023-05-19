package common;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import packages.*;
import requests.*;
import transfer.TransferFormat;
import security.Author;
import transfer.*;
import transfer.NetworkExchange.RequestType;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static common.PackageStorage.*;
import static transfer.NetworkExchange.*;

import static transfer.NetworkPacket.toBytes;


public class ServerDispatcher implements ServerController {
public static class UnsupportedRequestException extends Exception {
      UnsupportedRequestException(String msg) {
	    super(msg);
      }
}

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

/**
 * No request is specified on this method type
 */
private void onAllPackages(NetworkExchange exchange) throws IOException {
      var packages = storage.shortInfoList()
			 .toArray(ShortPackageInfoDTO[]::new);
      OutputStream output = exchange.getOutputStream();
      String payload = toJson(packages); //replace with concatenation of string)
      output.write(payload.getBytes(StandardCharsets.US_ASCII));
      exchange.setResponse(ResponseType.Approve, ResponseCode.ALL_PACKAGES_RESPONSE);
}

private void onPackageId(NetworkExchange exchange) {
      String alias = exchange.request().stringData();
      var optional = storage.toPackageId(alias);
      if (optional.isPresent()) {
	    ByteBuffer buffer = ByteBuffer.allocate(4);
	    buffer.putInt(optional.get().value());
	    exchange.setResponse(ResponseType.Approve, ResponseCode.PACKAGE_ID_RESPONSE, buffer.array());
      } else {
	    exchange.setResponse(ResponseType.Decline, ResponseCode.FORBIDDEN);
      }
}
//todo: asymmetric to symmetric handshake

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
private Optional<PackageHandle> toPackageHandle(NetworkExchange exchange) {
      String stringRequest = exchange.request().stringData();
      ByteBuffer buffer = ByteBuffer.wrap(exchange.request().data());
      Optional<PackageHandle> result = Optional.empty();
      int id = buffer.getInt();
      var optional = storage.getPackageId(id);
      if (optional.isPresent()) {
	    var packageId = optional.get();
	    Optional<VersionId> versionId;
	    switch (exchange.request().code(RequestCode.PAYLOAD_FORMAT_MASK)) {
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
	    result = versionId.map(v -> new PackageHandle(packageId, v));
      }
      return result;
}

private Optional<PackageHandle> toPackageHandle(Integer value, String label) {
      var packageId = storage.getPackageId(value);
      var version = packageId.flatMap(id -> storage.mapVersion(id, label));
      return version.map(v -> new PackageHandle(packageId.get(), v));
}

private Optional<PackageHandle> toPackageHandle(Integer value, int offset) {
      var packageId = storage.getPackageId(value);
      var version = packageId.flatMap(id -> storage.mapVersion(id, offset));
      return version.map(v -> new PackageHandle(packageId.get(), v));
}

private VersionFormat getVersionFormat(NetworkPacket request) {
      return switch (request.code(RequestCode.PAYLOAD_FORMAT_MASK)) {
	    case RequestCode.INT_FORMAT -> VersionFormat.Int;
	    case RequestCode.STR_FORMAT -> VersionFormat.String;
	    default -> VersionFormat.Unknown;
      };
}

private void onPackageInfo(NetworkExchange exchange) {
      String data = exchange.request().stringData();
      VersionFormat format = getVersionFormat(exchange.request());
      Optional<InfoRequest> clientRequest = InfoRequest.valueOf(data, format);
      Optional<PackageHandle> serverRequest = clientRequest.flatMap(request -> switch (format) {
	    case String -> toPackageHandle(request.getPackageId(), request.label());
	    case Int -> toPackageHandle(request.getPackageId(), request.offset());
	    case Unknown -> Optional.empty();
      });
      if (serverRequest.isPresent()) {
	    PackageHandle request = serverRequest.get();
	    var info = storage.getFullInfo(request.id(), request.version());
	    if (info.isPresent()) {
		  exchange.setResponse(
		      ResponseType.Approve, ResponseCode.PACKAGE_INFO_FORMAT, toBytes(info.get().stringify()));
	    } else {
		  exchange.setResponse(ResponseType.Decline, ResponseCode.NO_PAYLOAD);
	    }
      } else {
	    exchange.setResponse(ResponseType.Decline, ResponseCode.FORBIDDEN);
      }
}

/**
 * Specific request to check the version integrity
 * The versionInfo can be replaced by GetInfo. But it's supposed to be more suitable and fast
 */
private void onVersionInfo(NetworkExchange exchange) {
      String data = exchange.request().stringData();
      VersionFormat format = getVersionFormat(exchange.request());
      Optional<VersionRequest> userRequest = VersionRequest.valueOf(data,format);
      Optional<PackageHandle> serverRequest = userRequest.flatMap(request -> switch(format) {
		case String -> toPackageHandle(request.getPackageId(), request.label());
		case Int -> toPackageHandle(request.getPackageId(), request.offset());
		case Unknown -> Optional.empty();
	  }
      );
      if (serverRequest.isPresent()) {
	    var request = serverRequest.get();
	    var fullInfo = storage.getFullInfo(request.id(), request.version());
	    if (fullInfo.isPresent()) {
		  VersionInfoDTO versionInfo = new VersionInfoDTO(
		      request.version().value(),
		      fullInfo.get().version
		  );
		  exchange.setResponse(ResponseType.Approve, ResponseCode.VERSION_INFO_FORMAT, toBytes(versionInfo.stringify()));
	    } else {
		  exchange.setResponse(ResponseType.Decline, ResponseCode.NO_PAYLOAD);
	    }
      } else {
	    exchange.setResponse(ResponseType.Decline, ResponseCode.FORBIDDEN);
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
      String data = exchange.request().stringData();
      VersionFormat format = getVersionFormat(exchange.request());
      Optional<PayloadRequest> clientRequest = PayloadRequest.valueOf(data, format);
      Optional<PackageHandle> packageHandle = clientRequest.flatMap(request -> switch (format) {
	    case String -> toPackageHandle(request.getPackageId(), request.label());
	    case Int -> toPackageHandle(request.getPackageId(), request.offset());
	    case Unknown -> Optional.empty();
      });
      if (packageHandle.isPresent()) {
	    PackageHandle handle = packageHandle.get();
	    Optional<byte[]> payload = storage.getPayload(handle.id(), handle.version()); //todo: add archive as parameter
	    PackageAssembly assembly;
	    TransferFormat transfer;
	    if (payload.isPresent()) {
		  PackageHeader header = new PackageHeader(handle.id().value(), handle.version().value());
		  transfer = clientRequest.get().getTransfer();
		  assembly = PackageAssembly.valueOf(header, payload.get());
		  transfer.apply(assembly);
		  writeBytes(exchange, assembly.serialize());
		  exchange.setResponse(ResponseType.Approve, ResponseCode.PAYLOAD_FORMAT);
	    } else {
		  exchange.setResponse(ResponseType.Decline, ResponseCode.NO_PAYLOAD);
	    }
      } else {
	    exchange.setResponse(ResponseType.Decline, ResponseCode.ILLEGAL_REQUEST);
      }
}

private void onPublishInfo(NetworkExchange exchange) {
      String data = exchange.request().stringData();
      Optional<PublishInfoRequest> userRequest = PublishInfoRequest.valueOf(data);
      Optional<AuthorId> author = userRequest.flatMap(request -> storage.getAuthorId(request.getAuthorId()));
      if (author.isPresent()) {
	    AuthorId authorId = author.get();
	    PublishInfoDTO dto = userRequest.get().getDto();
	    Optional<PackageId> packageId = storage.toPackageId(dto.name());
	    PackageId id;
	    try {
		  if (packageId.isPresent()) {
			storage.updatePackageInfo(authorId, packageId.get(), dto);
			id = packageId.get();
		  } else {
			id = storage.storePackageInfo(authorId, dto);
		  }
		  exchange.setResponse(ResponseType.Approve, ResponseCode.PUBLISH_INFO_RESPONSE,
		      toBytes(id.value()));
	    } catch (StorageException e) {
		  exchange.setResponse(ResponseType.Decline, ResponseCode.VERBOSE_FORMAT,
		      toBytes(e.getMessage()));
	    }
      } else {
	    exchange.setResponse(ResponseType.Decline, ResponseCode.ILLEGAL_REQUEST);
      }
}

private void onPublishPayload(NetworkExchange exchange) throws IOException {
      String data = exchange.request().stringData();
      Optional<PublishInstanceRequest> userRequest = PublishInstanceRequest.valueOf(data);
      Optional<AuthorId> author = userRequest.flatMap(request -> storage.getAuthorId(request.getAuthorId()));
      if (author.isPresent()) {
	    AuthorId authorId = author.get();
	    PublishInstanceDTO dto = userRequest.get().getDto();
	    byte[] payload = readBytes(exchange, dto.getPayloadSize());
	    try {
		  if (payload != null) {
			VersionId version = storage.storePayload(authorId, dto, payload);
			exchange.setResponse(ResponseType.Approve, ResponseCode.PUBLISH_PAYLOAD_RESPONSE,
			    toBytes(version.value()));
		  } else {
			exchange.setResponse(ResponseType.Decline, ResponseCode.ILLEGAL_REQUEST);
		  }
	    } catch (StorageException e) {
		  exchange.setResponse(ResponseType.Decline, ResponseCode.VERBOSE_FORMAT,
		      toBytes(e.getMessage()));
	    }
      } else {
	    exchange.setResponse(ResponseType.Decline, ResponseCode.FORBIDDEN);
      }
}

private void onAuthorizeRequest(NetworkExchange exchange) {
      String base64Line = exchange.request().stringData();
      Optional<Author> author = Author.valueOf(base64Line);
      if (author.isPresent()) {
	    Optional<AuthorId> id = storage.getAuthorId(author.get());
	    try {
		  int code = ResponseCode.INT_FORMAT;
		  if (id.isEmpty()) { //attempt to register Author
			storage.authorize(author.get());
			id = storage.getAuthorId(author.get());
			code |= ResponseCode.CREATED;
		  }
		  if (id.isPresent()) {
			exchange.setResponse(ResponseType.Approve, code,
			    toBytes(id.get().value()));
		  }
	    } catch (StorageException e) {
		  exchange.setResponse(ResponseType.Decline, ResponseCode.FORBIDDEN);
	    }

      } else {
	    exchange.setResponse(ResponseType.Decline, ResponseCode.ILLEGAL_REQUEST);
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