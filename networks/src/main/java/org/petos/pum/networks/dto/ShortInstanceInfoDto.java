package org.petos.pum.networks.dto;

import jakarta.validation.constraints.NotNull;

/**
 * @author Paval Shlyk
 * @since 02/12/2023
 */
public record ShortInstanceInfoDto(
    @NotNull Integer packageId,
    @NotNull String version
) {
}
