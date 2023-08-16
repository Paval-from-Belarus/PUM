package common;

import com.google.gson.Gson;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LocalConfig {
@Getter @Setter
private String packages;
@Getter @Setter
private String repoName;
@Nullable
@Getter @Setter
private String[] mirrors;
@Getter @Setter
private Long timeout;
public static LocalConfig load(String path) {
      LocalConfig config;
      try {
	    String content = Files.readString(Path.of(path));
	    config = new Gson().fromJson(content, LocalConfig.class);
	    if (config.mirrors == null) {
		  config.mirrors = new String[0];
	    }
      } catch (IOException e) {
	    throw new RuntimeException(e);
      }
      return config;

}
}
