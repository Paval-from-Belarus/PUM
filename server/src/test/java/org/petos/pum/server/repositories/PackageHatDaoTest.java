package org.petos.pum.server.repositories;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.petos.pum.server.dto.PackageHatMapper;
import org.petos.pum.server.dto.PackageHatMapperImpl;
import org.petos.pum.server.repositories.entities.PackageHat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author Paval Shlyk
 * @since 23/08/2023
 */
@ContextConfiguration(classes = JpaConfig.class)
class PackageHatDaoTest extends AbstractDaoTest {

@Autowired
private PackageHatDao dao;
private PackageHatMapper mapper = new PackageHatMapperImpl();

@Test
@DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
public void testInsertDelete() {
      long count = dao.count();
      Assertions.assertEquals(count, dao.findAll().size());
      var hat = randomHat();
      dao.save(hat);
      Assertions.assertEquals(count + 1, dao.count());
      dao.delete(hat);
      Assertions.assertEquals(count, dao.count());
      dao.save(randomHat());
      dao.save(randomHat());
      hat = randomHat();
      dao.save(hat);
      var ref = dao.getReferenceById(hat.getId());
//      hat.setAliases(List.of("aboba", "heloo", "vasya").stream().map());
      Assertions.assertEquals(count + 3, dao.count());
//      dao.deleteAll();//impossible to remove all
//      Assertions.assertEquals(0, dao.count());
}

@Test
public void testPublisherSearch() {
      long count = dao.count();
      if (count == 0) {
	    dao.save(randomHat());
	    count += 1;
      }
      PackageHat any = dao.findAll().get(0);
      List<PackageHat> packages = dao.findAllByPublisherId(any.getPublisherId());
      Assertions.assertFalse(packages.isEmpty());
      Integer publisher = 0;
      count = dao.count();
      List<PackageHat> dummyList = List.of(hatByPublisherAndName(publisher, "XXX"),
	  hatByPublisherAndName(publisher, "XXXX"), hatByPublisherAndName(publisher, "XXXXX"));
      dummyList.forEach(hat -> dao.save(hat));
      packages = dao.findAllByPublisherId(publisher);
      Assertions.assertEquals(count + dummyList.size(), dao.count());
      dao.deleteAnyByPublisherIdAndValid(publisher, PackageHat.INVALID);
      Assertions.assertEquals(count, dao.count());
//      dao.deleteAllByPublisherId(any.getPublisherId());
//      Assertions.assertEquals(count - packages.size(), dao.count());
}

public static PackageHat hatByPublisherAndName(Integer id, String name) {
      return PackageHat.builder()
		 .name(name).publisherId(id).valid(false)
		 .build();
}

public static PackageHat randomHat() {
      Random random = ThreadLocalRandom.current();
      byte[] bytes = new byte[40];
      random.nextBytes(bytes);
      char[] letters = new char[bytes.length];
      int index = 0;
      for (byte b : bytes) {
	    letters[index] = (char) b;
	    index += 1;
      }
      return PackageHat.builder()
		 .valid(random.nextBoolean())
		 .name(String.valueOf(letters))
		 .publisherId(random.nextInt(2))
		 .build();
}

}