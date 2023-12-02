package org.petos.pum.repository.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Paval Shlyk
 * @since 02/12/2023
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class UserAccessViolationException extends RuntimeException {
public UserAccessViolationException(String message) {
      super(message);
}

public UserAccessViolationException(Throwable t) {
      super(t);
}
}
