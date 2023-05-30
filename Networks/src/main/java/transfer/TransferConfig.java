package transfer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import transfer.Serializer;

import javax.xml.parsers.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;

public class TransferConfig {
private Serializer origin;

public TransferConfig(Serializer origin) {
      this.origin = origin;
}

public void configure(URI resourceUri) throws ClassNotFoundException {
      Document document;
      try {
	    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    DocumentBuilder builder = factory.newDocumentBuilder();
	    document = builder.parse(resourceUri.toString());
      } catch (IOException e) {
	    throw new IllegalStateException("Impossible to configure the serializer");
      } catch (ParserConfigurationException e) {
	    throw new RuntimeException(e);
      } catch (SAXException e) {
	    throw new RuntimeException(e);
      }
      document.getDocumentElement().normalize();
      NodeList nodes = document.getElementsByTagName("registration");
      for (int i = 0; i < nodes.getLength(); i++) {
	    Node regNode = nodes.item(i);
	    String packageName = ((Element) regNode).getAttribute("package");
	    assert !packageName.isEmpty();
	    NodeList classes = ((Element) regNode).getElementsByTagName("class");
	    for (int j = 0; j < classes.getLength(); j++) {
		  Element element = (Element) classes.item(j);
		  int id = Integer.parseInt(element.getAttribute("id"));
		  String clazz = element.getTextContent();
		  origin.register(id, Class.forName(packageName + "." + clazz));
	    }
      }
}
}
