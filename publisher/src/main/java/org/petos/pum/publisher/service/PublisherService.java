package org.petos.pum.publisher.service;

import org.petos.pum.networks.dto.credentials.*;

import java.util.Optional;

/**
 * @author Paval Shlyk
 * @since 29/11/2023
 */
public interface PublisherService {
Optional<RegistrationErrorInfo> register(RegistrationRequest request);

/**
 * This method returns a non-empty result only in case when the authorization process can be succeeded.
 * Otherwise, return {@link Optional#empty()}
 *
 * @param request the corresponding authorization request from client side
 * @return Authorization info
 */
Optional<AuthorizationInfo> authorize(AuthorizationRequest request);

/**
 * Verify that proposed {@link ValidationRequest} is valid.
 * Currently {@link ValidationRequest} and {@link AuthorizationInfo} are same.
 * But in future both can involve independently
 *
 * @param request holds validation information required for validation
 * @return {@link Optional#empty()} in case when all is fine. Otherwise, return not empty {@link Optional}
 */
ValidationInfo validate(ValidationRequest request);

}
