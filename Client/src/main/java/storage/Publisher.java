package storage;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.Optional;

import static storage.PackageStorage.*;

public class Publisher {
public final String name;
public final PayloadType type;
public String[] aliases;
public String version;
public String exePath;
public LicenceType licence;
public PublisherDependency[] dependencies;
public Publisher(String name, PayloadType type) {
	this.name = name;
	this.type = type;
}
private boolean hasProperties(){
      boolean response;
      if (licence == null)
	    licence = LicenceType.GNU;
      if (aliases == null)
	    aliases = new String[0];
      if (dependencies == null)
	    dependencies = new PublisherDependency[0];
      response = name != null && type != null && exePath != null && version != null;
      return response;
}
public static Optional<Publisher> valueOf (String content) {
      Publisher sender = null;
      try {
	    sender = new Gson().fromJson(content, Publisher.class);
	    if (!sender.hasProperties())
		  sender = null;
      } catch (JsonSyntaxException e) {
	    System.out.println(e.getMessage());
      }
      return Optional.ofNullable(sender);
}
}
