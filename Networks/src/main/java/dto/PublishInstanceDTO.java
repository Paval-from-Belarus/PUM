package dto;


import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import transfer.TransferEntity;
import transfer.TransferOrder;


import java.util.Optional;

@EqualsAndHashCode(callSuper = false)
@TransferEntity(selective = true, ignoreNullable = true)
public class PublishInstanceDTO {
public static final String DEFAULT_LICENCE = "GNU";
@Getter
@TransferOrder(value = 0)
private final int packageId;
@Getter
@NotNull
@TransferOrder(value = 1)
private final String version;
@Getter
@TransferOrder(value = 2)
private final int payloadSize;
@Setter @Getter
@NotNull
@TransferOrder(value = 3)
private String license;

@Setter @Getter
@Nullable
@TransferOrder(value = 4)
private DependencyInfoDTO[] dependencies;

/**
 * @param dependencies is any alias (or package name) of dependency that should be used to install package.
 *                     Each dependency should be verified and exists other package will no publish.
 * @param version      is unique String label for package Family
 */
public PublishInstanceDTO(int id, @NotNull String version, int payloadSize) {
      this.packageId = id;
      this.version = version;
      this.payloadSize = payloadSize;
      this.license = DEFAULT_LICENCE;
}

}
