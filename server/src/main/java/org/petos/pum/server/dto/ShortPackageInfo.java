package org.petos.pum.server.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Paval Shlyk
 * @since 24/08/2023
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShortPackageInfo {
private String domainName;
private String type;
private String[] aliases;
}
