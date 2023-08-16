package database;

import dto.DependencyInfoDTO;

import java.util.Objects;

public class PackageInfo {
	public Integer packageId;
	public Integer versionId;
	public String license;
	public String payload;
	public String version;
	public String repoUrl; //the url address of address for which should be
	public DependencyInfoDTO[] dependencies;
	public boolean equals(Integer id, String version){
	      return Objects.equals(id, packageId) && Objects.equals(version, this.version);
	}
}
