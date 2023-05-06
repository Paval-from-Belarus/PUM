package packages;

//generally, package info is supposed to transfer through network
//this class hasn't any real representation in database
//Client should only accept this entity
//This info soo verbose
public class FullPackageInfoDTO {
public  String name;
public String[] aliases;
public String payloadType;
public String version;
public String licenseType;
public DependencyInfoDTO[] dependencies;
public Integer payloadSize;
}
