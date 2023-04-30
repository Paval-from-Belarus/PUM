package org.petos.packagemanager.client;

import org.petos.packagemanager.packages.DependencyInfoDTO;

public class PackageInfo {
	public Integer id;
	public Integer version;
	public String license;
	public String payload;
	public String label;
	public DependencyInfoDTO[] dependencies;
}
