package dto;

import lombok.Getter;
import lombok.Setter;
import transfer.TransferEntity;
import transfer.TransferOrder;

@TransferEntity(selective = true, ignoreNullable = true)
public class RepoInfoDTO {
public RepoInfoDTO(String name, long timeout) {
      this.name = name;
      this.timeout = timeout;
}
@Getter
@TransferOrder(value = 0)
private final String name;
@Getter
@TransferOrder(value = 1)
private final long timeout;
@Getter @Setter
@TransferOrder(value = 2)
private String[] mirrors;
@Getter @Setter
@TransferOrder(value = 3)
private byte[] publicKey;

}
