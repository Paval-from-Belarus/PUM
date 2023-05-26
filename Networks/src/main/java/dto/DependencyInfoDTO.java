package dto;

import lombok.*;
import lombok.experimental.Accessors;

import java.util.Optional;

@Accessors(prefix = "", makeFinal = true, fluent = true)
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@EqualsAndHashCode
public class DependencyInfoDTO extends AbstractDTO {
@Getter
private Integer packageId;
@Getter
private String label;

public static Optional<DependencyInfoDTO> valueOf(String content) {
      return valueOf(DependencyInfoDTO.class, content);
}
}
