package org.petos.pum.http.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Paval Shlyk
 * @since 15/11/2023
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {
public ResourceNotFoundException(String message) {
      super(message);
}

public ResourceNotFoundException(Throwable t) {
      super(t);
}
}
