package org.petos.packagemanager.server;

import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;
import org.petos.packagemanager.DataPackage;
import org.petos.packagemanager.PackageEntity;
import org.petos.packagemanager.PackageInfo;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class PackageStorage {
private ConcurrentHashMap<String, Integer> nameMapper;
private ConcurrentHashMap<Integer, PackageInfo> packages;
private static class NameMapEntry {
      public String name;
      public Integer id;
}

private void initNameMapper() {
      nameMapper = new ConcurrentHashMap<>();
      try (BufferedReader reader = new BufferedReader(new FileReader("mapper.json"))) {
	    StringBuilder strText = new StringBuilder();
	    String line;
	    Gson gson = new Gson();
	    while ((line = reader.readLine()) != null)
		  strText.append(line);
	    NameMapEntry[] entries = gson.fromJson(strText.toString(), NameMapEntry[].class);
	    Arrays.stream(entries).forEach(entry -> nameMapper.put(entry.name, entry.id));
      } catch (IOException e) {
	    throw new RuntimeException(e);
      }
}

private void initPackages() {
      packages = new ConcurrentHashMap<>();
      try (BufferedReader reader = new BufferedReader(new FileReader("packages.json"))) {
	    StringBuilder strText = new StringBuilder();
	    String line;
	    Gson gson = new Gson();
	    while ((line = reader.readLine()) != null)
		  strText.append(line);
	    PackageInfo[] packages = gson.fromJson(strText.toString(), PackageInfo[].class);
	    AtomicInteger counter = new AtomicInteger(0);
	    Arrays.stream(packages).forEach(info -> {
		  this.packages.put(counter.get(), info);
		  counter.set(counter.get() + 1);
	    });
      } catch (IOException e) {
	    throw new RuntimeException(e);
      }

}

/**
 * @return sorted DataPackage according time that had been added <br>
 * More fresh Data stores closer to first item. <br>
 * IE. The first item is most fresh
 */
private @NotNull List<DataPackage> getPackageBranch(Integer id) {
      PackageInfo info = packages.get(id);
      if (info == null)
	    return List.of();
      DataPackage data = new DataPackage();
      data.info = info;
      data.payload = new byte[0];
      return List.of(data);
}

private @NotNull PackageInfo getPackageInfo(String packageName) throws IllegalStateException {
      Integer id = nameMapper.get(packageName);
      if (id == null)
	    throw new IllegalStateException("Package not exists");
      PackageInfo info = packages.get(id);
      if (info == null)
	    throw new IllegalStateException("No package for corresponding id");
      return info;
}

/**
 * if version == 0: get latest version<br>
 * elsif version in [-1,-9]: get specific latter version<br>
 * else: get minimal available version<br>
 */
private @NotNull PackageEntity getPackage(int id, int versionOffset) {
      List<DataPackage> list = getPackageBranch(id);
      assert list.size() != 0;
      DataPackage chosen;
      switch (versionOffset) {
	    case 0 -> chosen = list.get(0);
	    case -1, -2, -3, -4,
		     -5, -6, -7, -8, -9 -> chosen = list.get(Math.min(list.size() - 1, -versionOffset));
	    default -> chosen = list.get(list.size() - 1);
      }
      Integer versionId = chosen.getVersionId();
      PackageEntity entity = PackageEntity.valueOf(chosen, id, versionId);
      return entity;
}
}
