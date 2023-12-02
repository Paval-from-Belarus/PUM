package org.petos.pum.networks.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * @author Paval Shlyk
 * @since 01/12/2023
 */
public record PublicationDto(
    @NotNull String packageName,
    @NotNull String packageType,
    @NotNull String version,
    @NotNull String license,
    List<String> aliases,
    @NotNull List<DependencyDto> dependencies
) {
}
