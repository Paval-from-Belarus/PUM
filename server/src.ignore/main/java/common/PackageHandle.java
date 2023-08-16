package common;

import static common.PackageStorage.*;

public record PackageHandle(PackageId id, VersionId version) {
}
