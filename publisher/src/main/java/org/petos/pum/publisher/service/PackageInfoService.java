package org.petos.pum.publisher.service;

import org.petos.pum.networks.dto.packages.HeaderInfo;
import reactor.core.publisher.Mono;

/**
 * @author Paval Shlyk
 * @since 29/11/2023
 */

public interface PackageInfoService {
      Mono<HeaderInfo> publishHeader();
}
