package org.petos.pum.networks.transfer;

import java.io.IOException;
import java.io.OutputStream;

@FunctionalInterface
public interface TailWriter {
      void write(OutputStream output) throws IOException;
}
