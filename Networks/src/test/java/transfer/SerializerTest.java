package transfer;

import dto.DependencyInfoDTO;
import dto.FullPackageInfoDTO;
import org.apache.logging.log4j.core.config.plugins.PluginLoggerContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class SerializerTest {
private Serializer serializer = new Serializer();
@Test
public void test() {
      var list = Integer.class.getDeclaredFields();
      serializer.register(7, DependencyInfoDTO.class);
      var dto = new FullPackageInfoDTO();
      dto.dependencies = new DependencyInfoDTO[]{new DependencyInfoDTO(111, "111"), new DependencyInfoDTO(222, "222")};
      dto.aliases = new String[]{"first", "second", "third"};
      dto.name = "Test";
      dto.version = "0.0.0";
      dto.payloadType = "Binary";
      dto.payloadSize = 777;
      dto.licenseType = "GNU";
      byte[] bytes = serializer.serialize(dto);
      FullPackageInfoDTO dto1 = serializer.construct(bytes, FullPackageInfoDTO.class);
      assertEquals(dto1, dto);
}
}