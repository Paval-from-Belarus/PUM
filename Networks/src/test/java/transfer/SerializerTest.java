package transfer;

import dto.DependencyInfoDTO;
import dto.FullPackageInfoDTO;
import dto.PublishInstanceDTO;
import dto.ShortPackageInfoDTO;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import requests.PayloadRequest;
import requests.PublishInstanceRequest;
import requests.VersionRequest;
import security.Encryptor;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SerializerTest {
@EqualsAndHashCode
public static class ArrayOrigin {
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
	    nothing = 110.0f;
      }

      private Integer[][][] numbers;
      private Float value;
      @EqualsAndHashCode.Exclude
      private transient Integer id;
      private Boolean response;
      private short[][] smallNumbers;
      private byte small;
      private Number nothing;

}

@Test
@Order(0)
public void arrayTest() {
      Object constructed;
      ArrayOrigin origin = new ArrayOrigin(40);
      Serializer serializer = new Serializer();
      byte[] bytes = serializer.serialize(origin);
      constructed = serializer.construct(bytes, ArrayOrigin.class);
      assertEquals(origin, constructed);
      origin.numbers[1][2][3] = null;
      origin.value -= 1.0f;
      origin.numbers[1][1][3] = null;
      bytes = serializer.serialize(origin);
      constructed = serializer.construct(bytes, ArrayOrigin.class);
      assertEquals(origin, constructed);
      int[] numbers = new int[20];
      bytes = serializer.serialize(numbers);
      constructed = serializer.construct(bytes, int[].class);
}

@Test
@Order(1)
public void simpleDtoTest() {
      Object constructed;
      byte[] bytes;
      Serializer serializer = new Serializer();
      serializer.register(Serializer.FIRST_FREE_CODE, DependencyInfoDTO.class).register(Serializer.FIRST_FREE_CODE + 1, PublishInstanceDTO.class);
      var dto = new PublishInstanceDTO(11, "0.0.0", 777);
      dto.setLicense("GNU");
      dto.setDependencies(new DependencyInfoDTO[]{new DependencyInfoDTO(111, "111"), new DependencyInfoDTO(222, "222")});
      var request = new PublishInstanceRequest(12, dto);
      bytes = serializer.serialize(request);
      constructed = serializer.construct(bytes, PublishInstanceRequest.class);
      assertEquals(request, constructed);
      VersionRequest version = new VersionRequest(12, 2);
      bytes = serializer.serialize(version);
      constructed = serializer.construct(bytes, VersionRequest.class);
      assertEquals(constructed, version);
      assertThrows(IllegalArgumentException.class, () -> serializer.register(20, Object.class));
      ShortPackageInfoDTO shortInfo = new ShortPackageInfoDTO("PetOS",  10,"0.0.1");
      bytes = serializer.serialize(shortInfo);
      constructed = serializer.construct(bytes, ShortPackageInfoDTO.class);
      assertEquals(constructed, shortInfo);
      serializer.register(20, Encryptor.Encryption.class).register(21, PackageAssembly.ArchiveType.class);
      PayloadRequest payloadRequest = new PayloadRequest(12, "0.0.1", PackageAssembly.ArchiveType.Brotli);
      bytes = serializer.serialize(payloadRequest);
      constructed = serializer.construct(bytes, PayloadRequest.class);
      assertEquals(constructed, payloadRequest);
      var fullInfo = new FullPackageInfoDTO("PetOS", "0.0.1", 10101);
      bytes = serializer.serialize(fullInfo);
      constructed = serializer.construct(bytes, FullPackageInfoDTO.class);
      assertEquals(fullInfo, constructed);
}


}