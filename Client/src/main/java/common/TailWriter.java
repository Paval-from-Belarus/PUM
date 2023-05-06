package common;

import java.io.IOException;
import java.io.OutputStream;

@FunctionalInterface
public interface TailWriter {
      void write(OutputStream output) throws IOException;
}
