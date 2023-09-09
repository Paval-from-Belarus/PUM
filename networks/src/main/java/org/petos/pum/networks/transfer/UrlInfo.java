package org.petos.pum.networks.transfer;

import org.jetbrains.annotations.NotNull;

public record UrlInfo(String url, int port, @NotNull UrlInfo[] mirrors) {
private static final UrlInfo[] DUMMY_MIRRORS = new UrlInfo[0];
public static UrlInfo valueOf(String url, int port) {
      return new UrlInfo(url, port, DUMMY_MIRRORS);
}
}
