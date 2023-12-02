package common;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Wrapper<T> {
public Wrapper() {
}

public Wrapper(T value) {
      this.value = value;
}

private T value = null;

public void set(T value) {
      this.value = value;
}

public @Nullable T get() {
      return value;
}

public boolean isEmpty() {
      return value == null;
}
}
