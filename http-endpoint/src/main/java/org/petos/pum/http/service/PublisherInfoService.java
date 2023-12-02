package org.petos.pum.http.service;

import org.petos.pum.networks.dto.AuthorizationDto;
import org.petos.pum.networks.dto.RegistrationDto;
import org.petos.pum.networks.dto.credentials.AuthorizationInfo;
import org.petos.pum.networks.dto.credentials.RegistrationErrorInfo;
import org.petos.pum.networks.dto.credentials.ValidationInfo;
import reactor.core.publisher.Mono;

/**
 * @author Paval Shlyk
 * @since 01/12/2023
 */

public interface PublisherInfoService {
Mono<ValidationInfo> validate(String token);

Mono<RegistrationErrorInfo> register(RegistrationDto dto);

Mono<AuthorizationInfo> authorize(AuthorizationDto dto);
}
