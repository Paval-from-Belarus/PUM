package org.petos.packagemanager.transfer;

import org.petos.packagemanager.server.PackageStorage;

import java.util.Optional;

public record PackageRequest(PackageStorage.PackageId id, PackageStorage.VersionId version) {
}
