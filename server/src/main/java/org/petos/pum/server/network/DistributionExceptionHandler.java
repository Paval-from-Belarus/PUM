package org.petos.pum.server.network;

import io.grpc.Status;
import net.devh.boot.grpc.server.advice.GrpcAdvice;
import net.devh.boot.grpc.server.advice.GrpcExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Paval Shlyk
 * @since 16/09/2023
 */
@GrpcAdvice
public class DistributionExceptionHandler {
private static final Logger logger = LoggerFactory.getLogger(DistributionExceptionHandler.class);
@GrpcExceptionHandler
public Status handleInvalidArgument(IllegalArgumentException e) {
      return Status.INVALID_ARGUMENT.withDescription("Dummy description").withCause(e);
}


}
