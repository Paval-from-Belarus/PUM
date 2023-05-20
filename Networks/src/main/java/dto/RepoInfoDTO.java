package dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@NoArgsConstructor
public class RepoInfoDTO extends AbstractDTO {
@Getter @Setter
private String name;
@Getter @Setter
private String[] mirrors;
@Getter @Setter
private Long timeout;
@Getter @Setter
private byte[] publicKey;
public static Optional<RepoInfoDTO> valueOf(String content) {
      return valueOf(RepoInfoDTO.class, content);
}
}
