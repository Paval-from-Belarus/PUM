package org.petos.pum.http.service.impl;

import com.google.protobuf.Message;
import jakarta.annotation.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.petos.pum.http.exception.ResourceNotFoundException;
import org.petos.pum.networks.dto.transfer.ResponseStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * @author Paval Shlyk
 * @since 01/12/2023
 */
public abstract class AbstractReactiveKafkaService {
private static final Logger LOGGER = LogManager.getLogger(AbstractReactiveKafkaService.class);
private final Map<UUID, BiConsumer<? super Message, ResponseStatus>> packageInfoMap = new ConcurrentHashMap<>();

protected UUID nextUniqueSessionId() {
      UUID uuid;
      do {
	    uuid = UUID.randomUUID();
      } while ((packageInfoMap.putIfAbsent(uuid, (value, status) -> {
      })) != null);
      return uuid;
}

protected <T extends Message> void consumeResponse(UUID id, @Nullable T info, ResponseStatus status) {
      BiConsumer<? super Message, ResponseStatus> consumer = packageInfoMap.get(id);
      if (consumer == null) {
	    final String msg = String.format("Attempt to fetch non existing consumer by id=%s", id.toString());
	    LOGGER.warn(msg);
	    return;
      }
      consumer.accept(info, status);
}

protected <T> Mono<T> nextMonoResponse(UUID sessionId) {
      return Mono.<T>create(sink -> {
	    packageInfoMap.put(sessionId, (info, status) -> {
		  if (status == ResponseStatus.NOT_FOUND) {
			sink.error(newMonoNotFoundException(sessionId));
		  } else {
			LOGGER.trace("Mono is committed by sessionId {}", sessionId);
			sink.success((T) info);
		  }
		  packageInfoMap.remove(sessionId);
	    });
	    sink.onDispose(() -> {
		  packageInfoMap.remove(sessionId);
		  LOGGER.trace("Mono is canceled by sessionId {}", sessionId.toString());
	    });
      });
}

protected <T> Flux<T> nextFluxResponse(UUID sessionId) {
      return Flux.create(sink -> {
	    packageInfoMap.put(sessionId, (info, status) -> {
		  if (info == null || status == ResponseStatus.NOT_FOUND) {
			sink.error(newMonoNotFoundException(sessionId));
			packageInfoMap.remove(sessionId);
		  } else {
			sink.next((T) info);
			LOGGER.trace("Flux is appended with sessionId {}", sessionId);
			if (status == ResponseStatus.LAST) {
			      sink.complete();
			}
		  }
	    });
	    sink.onDispose(() -> {
		  packageInfoMap.remove(sessionId);
		  LOGGER.trace("Flux is canceled by sessionId {}", sessionId);
	    });
      });
}

private ResourceNotFoundException newMonoNotFoundException(UUID sessionId) {
      final String msg = String.format("Failed to find mono for given session %s", sessionId.toString());
      LOGGER.trace(msg);
      return new ResourceNotFoundException(msg);

}
}
