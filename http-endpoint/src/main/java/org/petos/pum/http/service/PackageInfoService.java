package org.petos.pum.http.service;

import org.petos.pum.networks.dto.packages.EndpointInfo;
import org.petos.pum.networks.dto.packages.FullInstanceInfo;
import org.petos.pum.networks.dto.packages.HeaderInfo;
import org.petos.pum.networks.dto.packages.ShortInstanceInfo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Paval Shlyk
 * @since 14/11/2023
 */
public interface PackageInfoService {
Mono<HeaderInfo> getHeaderInfo(String headerInfo);

Flux<ShortInstanceInfo> getAllInstanceInfo(int packageId);

Mono<FullInstanceInfo> getFullInfo(int packageId, String version);

/**
 * prepare server side to file downloading
 *
 * @param packageId
 * @param version
 * @return
 */
Mono<EndpointInfo> getPayloadInfo(int packageId, String version, String archive);
}
