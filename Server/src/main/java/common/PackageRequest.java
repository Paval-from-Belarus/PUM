package common;

import static common.PackageStorage.*;

public record PackageRequest(PackageId id, VersionId version) {
}
