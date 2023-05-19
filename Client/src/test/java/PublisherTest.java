import org.junit.jupiter.api.Test;
import storage.Publisher;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PublisherTest {
@Test
void testPublisher() {
      String properties = "{name=kernel,type=\"Binary\",version=\"0.0.1\",exePath=\"/home\"}";
      Optional<Publisher> publisher = Publisher.valueOf(properties);
      assertTrue(publisher.isPresent());
}
}
