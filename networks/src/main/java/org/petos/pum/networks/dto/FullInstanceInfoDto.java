package org.petos.pum.networks.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

/**
 * @author Paval Shlyk
 * @since 02/12/2023
 */
public record FullInstanceInfoDto(
    @NotNull Integer packageId,
    @NotNull String version,
    @NotNull Map<String, Long> archives,
    @NotNull List<ShortInstanceInfoDto> dependencies
) {
}
