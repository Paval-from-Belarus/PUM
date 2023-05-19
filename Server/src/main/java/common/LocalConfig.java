package common;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class LocalConfig {
@Getter
@Setter
private String packages;

public static LocalConfig load(String path) {
      LocalConfig config;
      try {
	    String content = Files.readString(Path.of(path));
	    config = new Gson().fromJson(content, LocalConfig.class);
      } catch (IOException e) {
	    throw new RuntimeException(e);
      }
      return config;

}
}
