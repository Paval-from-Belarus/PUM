package requests;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;

@AllArgsConstructor
@MethodRequest(name="GetId")
public class IdRequest {
@Getter
private String alias;
}
