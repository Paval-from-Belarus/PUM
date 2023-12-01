package org.petos.pum.publisher.service.impl;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.petos.pum.networks.dto.credentials.*;
import org.petos.pum.publisher.dao.PublisherRepository;
import org.petos.pum.publisher.model.Publisher;
import org.petos.pum.publisher.service.PublisherService;
import org.petos.pum.publisher.utils.AuthorizationUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Optional;

/**
 * @author Paval Shlyk
 * @since 29/11/2023
 */
@Service
@RequiredArgsConstructor
public class PublisherServiceImpl implements PublisherService {
private static final Logger LOGGER = LogManager.getLogger(PublisherServiceImpl.class);
private final PublisherRepository publisherRepository;

@Override
@Transactional
public Optional<RegistrationErrorInfo> register(RegistrationRequest request) {
      RegistrationErrorInfo info = null;
      byte[] salt = AuthorizationUtils.newRandomSalt();
      byte[] hashedPassword = AuthorizationUtils.generateHash(request.getPassword(), salt);
      try {
	    Publisher publisher = Publisher.builder()
				      .salt(salt)
				      .password(hashedPassword)
				      .name(request.getName())
				      .email(request.getEmail())
				      .build();
	    if (publisherRepository.existsByName(request.getName())) {
		  info = RegistrationErrorInfo.newBuilder()
			     .setError("Username is not unique")
			     .build();
		  return Optional.of(info);
	    }
	    publisherRepository.save(publisher);
      } catch (DataAccessException e) {
	    LOGGER.error(e);
	    info = RegistrationErrorInfo.newBuilder()
		       .setError("The server is busy")
		       .build();
      }
      return Optional.ofNullable(info);
}

@Override
public Optional<AuthorizationInfo> authorize(AuthorizationRequest request) {
      AuthorizationInfo info = null;
      try {
	    Optional<Publisher> optionalPublisher = publisherRepository.findByName(request.getName());
	    if (optionalPublisher.isPresent()) {
		  Publisher publisher = optionalPublisher.get();
		  byte[] hashedPassword = AuthorizationUtils.generateHash(request.getPassword(), publisher.getSalt());
		  if (Arrays.equals(hashedPassword, publisher.getPassword())) {
			info = AuthorizationInfo.newBuilder()
				   .setToken(String.valueOf(publisher.getId()))
				   .build();
		  }
	    }
      } catch (DataAccessException e) {
	    LOGGER.error(e);
      }
      return Optional.ofNullable(info);
}

@Override
public ValidationInfo validate(ValidationRequest request) {
      String token = request.getToken();
      ValidationInfo.Builder builder = ValidationInfo.newBuilder();
      try {
	    long publisherId = Long.parseLong(token);
	    if (!publisherRepository.existsById(publisherId)) {
		  builder.setError("Token for non existing publisher");
	    } else {
		  builder.setPublisherId(publisherId);
	    }
      } catch (NumberFormatException e) {
	    builder.setError("Invalid token format");
      } catch (DataAccessException e) {
	    LOGGER.error(e);
	    builder.setError("Server is busy");
      }
      assert builder.isInitialized();
      return builder.build();
}

}
