package org.petos.pum.publisher.service.impl;

import lombok.RequiredArgsConstructor;
import org.petos.pum.networks.dto.credentials.*;
import org.petos.pum.publisher.service.PublisherService;
import org.petos.pum.publisher.utils.AuthorizationUtils;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * @author Paval Shlyk
 * @since 29/11/2023
 */
@Service
@RequiredArgsConstructor
public class PublisherServiceImpl implements PublisherService {
@Override
public boolean register(RegistrationRequest request) {
      byte[] salt = AuthorizationUtils.newRandomSalt();
      byte[] hashedPassword = AuthorizationUtils.generateHash(request.getPassword(), salt);

}

@Override
public Optional<AuthorizationInfo> authorize(AuthorizationRequest request) {
      return Optional.empty();
}

@Override
public Optional<PublicationInfo> publish(PublicationRequest request) {
      return Optional.empty();
}
}
