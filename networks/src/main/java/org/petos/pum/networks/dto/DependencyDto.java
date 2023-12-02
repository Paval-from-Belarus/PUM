package org.petos.pum.networks.dto;

import jakarta.validation.constraints.NotNull;

/**
 * @author Paval Shlyk
 * @since 01/12/2023
 */
public record DependencyDto(
    @NotNull Integer packageId,
    @NotNull String version
) {
}
