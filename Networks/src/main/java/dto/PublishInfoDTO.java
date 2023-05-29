package dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import transfer.TransferEntity;

import java.util.Optional;
@TransferEntity
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@Accessors(prefix = "", makeFinal = true, fluent = true)
public class PublishInfoDTO extends AbstractDTO{
@NotNull
@Getter
private String name;
@NotNull
@Getter
private String[] aliases;
@Getter
private String payloadType;
public static Optional<PublishInfoDTO> valueOf(String content) {
      return valueOf(PublishInfoDTO.class, content);
}
}
