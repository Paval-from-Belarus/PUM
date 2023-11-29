package org.petos.pum.publisher.service;

import org.petos.pum.networks.dto.credentials.*;

import java.util.Optional;

/**
 * @author Paval Shlyk
 * @since 29/11/2023
 */
public interface PublisherService {
boolean register(RegistrationRequest request);

Optional<AuthorizationInfo> authorize(AuthorizationRequest request);

Optional<PublicationInfo> publish(PublicationRequest request);

}
