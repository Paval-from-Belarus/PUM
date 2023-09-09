package org.petos.pum.networks.dto;

import lombok.Getter;
import lombok.Setter;
import org.petos.pum.networks.transfer.TransferEntity;
import org.petos.pum.networks.transfer.TransferOrder;

@TransferEntity(ignoreNullable = true, code = 21)
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
