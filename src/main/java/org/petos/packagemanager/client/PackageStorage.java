package org.petos.packagemanager.client;

import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;
import org.petos.packagemanager.packages.DependencyInfoDTO;
import org.petos.packagemanager.packages.FullPackageInfoDTO;
import org.petos.packagemanager.packages.PackageAssembly;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PackageStorage {
public static class PackageIntegrityException extends Exception {
      PackageIntegrityException(String msg) {
	    super(msg);
      }
}

public PackageStorage(Configuration config) throws IOException {
      PackageStorage.config = config;
      String content = Files.readString(Path.of(config.infoPath));
      PackageStorage.installed =
	  InstanceInfo.valueOf(content).parallelStream()
	      .collect(Collectors.toMap(InstanceInfo::getId, info -> info));
}

public boolean isInstalledPackage(String name) {
      assert installed != null;
      return installed.values().stream()
		 .anyMatch(i -> i.similar(name));
}

public boolean isInstalledPackage(@NotNull Integer id, @NotNull String label) {
      assert installed != null;
      boolean response = false;
      var instance = installed.get(id);
      if (instance != null) {
	    try {
		  Path infoPath = Path.of(instance.getStringPath());
		  String jsonInfo = Files.readString(infoPath);
		  PackageInfo info = fromJson(jsonInfo, PackageInfo.class);
		  response = id.equals(info.id) && label.equals(info.label);
	    } catch (IOException ignored) {
	    }
      }
      return response;
}

public static void storePackageInfo(Path configDir, PackageInfo info) throws IOException {
      String jsonInfo = toJson(info);
      Files.writeString(configDir.resolve("conf.pum"), jsonInfo);
}

public static PackageInfo toLocalFormat(PackageAssembly assembly, FullPackageInfoDTO dto) {
      var info = new PackageInfo();
      info.id = assembly.getId();
      info.version = assembly.getVersion();
      info.license = dto.licenseType;
      info.payload = dto.payloadType;
      info.label = dto.version;
      info.dependencies = dto.dependencies;
      return info;
}

//The great assumption of this method... Never use application during its installation
public static void rebuildConfig(List<InstanceInfo> instances) throws IOException {
      assert config != null && installed != null;
      Path packagesInfo = Path.of(config.infoPath);
      instances.forEach(i -> installed.put(i.getId(), i));
      StringBuilder output = new StringBuilder();
      for (var instance : installed.values()) {
	    output.append(instance);
      }
      Files.writeString(packagesInfo, output.toString());
}

//At this moment, in central path stores configaration file
//The configuration file has <code>Local format</code>
//The result of thi function is OS relative form of ddl
public static @NotNull Path linkLibraries(Path central, @NotNull List<InstanceInfo> instances) throws IOException {
      assert installed != null;
      Path configPath = central.resolve("conf.pum");
      Path linkPath = Path.of(central.toString(), "bin", "ddl.conf");
      if (instances.size() != 0) {
	    try (BufferedWriter writer = new BufferedWriter(new FileWriter(linkPath.toFile()))) {
		  String jsonInfo = Files.readString(configPath);
		  PackageInfo info = fromJson(jsonInfo, PackageInfo.class);
		  for (DependencyInfoDTO depLink : info.dependencies) {
			InstanceInfo depInfo = installed.get(depLink.packageId());
			if (depInfo != null && !instances.contains(depInfo))
			      instances.add(depInfo);
		  }

		  for (var path : instances) {
			writer.write(path.getStringPath() + "\r\n");
		  }
	    }
      }//also store linkPath in executable
      return linkPath;
}

public static <T> T fromJson(String source, Class<T> classType) {
      return gson.fromJson(source, classType);
}

public static <T> String toJson(T source) {
      return gson.toJson(source);
}

private static Configuration config;
private static Map<Integer, InstanceInfo> installed;
private static Gson gson = new Gson();
}
