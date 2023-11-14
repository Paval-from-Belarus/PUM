package org.petos.pum.repository.service;

import org.petos.pum.networks.dto.packages.*;

import java.util.List;
import java.util.Optional;

/**
 * @author Paval Shlyk
 * @since 14/11/2023
 */
public interface RepositoryService {
Optional<HeaderInfo> getHeaderInfo(HeaderRequest request);
List<ShortInstanceInfo> getShortInfo(InstanceRequest request);
List<FullInstanceInfo> getFullInfo(InstanceRequest request);
Optional<PayloadInfo> getPayloadInfo(PayloadRequest request);
}
