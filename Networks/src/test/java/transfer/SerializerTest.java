package transfer;

import dto.DependencyInfoDTO;
import dto.FullPackageInfoDTO;
import lombok.EqualsAndHashCode;
import org.apache.logging.log4j.core.config.plugins.PluginLoggerContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class SerializerTest {
@EqualsAndHashCode
public static class ArrayOrigin {
      ArrayOrigin(){}
      ArrayOrigin(int count) {
            Random random = new Random(132);
            numbers = new Integer[count][count][count];
            for (int i = 0; i < count; i++) {
                  for (int j = 0; j < count; j++) {
                        for (int k = 0; k < count; k++) {
                              numbers[i][j][k] = random.nextInt(0, 10000);
                        }
                  }
            }
            value = 42.34f;
            response = true;
            smallNumbers = new short[count / 2][count];
            for (int i = 0; i < count / 2; i++) {
                  for (int j = 0; j < count; j++) {
                        smallNumbers[i][j] = (short) random.nextInt(0, 5000);
                  }
            }
            small = 42;
      }
      private Integer[][][] numbers;
      private Float value;
      @EqualsAndHashCode.Exclude
      private transient Integer id;
      private boolean response;
      private short[][] smallNumbers;
      private byte small;

}
@Test
@Order(0)
public void arrayTest() {
      ArrayOrigin origin = new ArrayOrigin(40);
      Serializer serializer = new Serializer();
      byte[] bytes = serializer.serialize(origin);
      ArrayOrigin origin1 = serializer.construct(bytes, ArrayOrigin.class);
      assertEquals(origin, origin1);
}
@Test
@Order(1)
public void simpleDtoTest() {
      Serializer serializer = new Serializer();
      serializer.register(Serializer.FIRST_FREE_CODE, DependencyInfoDTO.class).register(Serializer.FIRST_FREE_CODE + 1, FullPackageInfoDTO.class);
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