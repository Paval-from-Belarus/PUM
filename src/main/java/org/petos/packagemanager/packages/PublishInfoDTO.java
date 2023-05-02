package org.petos.packagemanager.packages;

import org.jetbrains.annotations.NotNull;

public record PublishInfoDTO(@NotNull String name, @NotNull String[] aliases, String payloadType) {
}
