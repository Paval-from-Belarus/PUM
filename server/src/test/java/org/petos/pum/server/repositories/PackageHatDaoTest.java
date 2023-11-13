package org.petos.pum.server.repositories;

import org.junit.jupiter.api.*;
import org.petos.pum.server.dto.PackageHatMapperImpl;
import org.petos.pum.server.repositories.entities.PackageHat;
import org.petos.pum.server.repositories.entities.PackageType;
import org.petos.pum.server.dto.PackageHatMapper;
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
private PackageHatDao hatDao;
@Autowired
private PackageTypeDao typesDao;
private PackageHatMapper mapper = new PackageHatMapperImpl();
public PackageHat hatByPublisherAndName(Integer id, String name) {
      return PackageHat.builder()
		 .name(name).publisherId(id).valid(false).type(randomType())
		 .build();
}
public PackageType randomType() {
      return typesDao.findByName(PackageType.APP).orElseThrow();
}
public PackageHat randomHat() {
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
		 .type(randomType())
		 .build();
}
@Test
@DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
public void testInsertDelete() {
      long count = hatDao.count();
      Assertions.assertEquals(count, hatDao.findAll().size());
      var hat = randomHat();
      hatDao.save(hat);
      Assertions.assertEquals(count + 1, hatDao.count());
      hatDao.delete(hat);
      Assertions.assertEquals(count, hatDao.count());
      hatDao.save(randomHat());
      hatDao.save(randomHat());
      hat = randomHat();
      hatDao.save(hat);
      var ref = hatDao.getReferenceById(hat.getId());
//      hat.setAliases(List.of("aboba", "heloo", "vasya").stream().map());
      Assertions.assertEquals(count + 3, hatDao.count());
//      dao.deleteAll();//impossible to remove all
//      Assertions.assertEquals(0, dao.count());
}

@Test
public void testPublisherSearch() {
//      PackageType type = typesDao.findByName(PackageType.APP).orElseThrow();
      long count = hatDao.count();
      if (count == 0) {
	    hatDao.save(randomHat());
	    count += 1;
      }
      PackageHat any = hatDao.findAll().get(0);
      List<PackageHat> packages = hatDao.findAllByPublisherId(any.getPublisherId());
      Assertions.assertFalse(packages.isEmpty());
      Integer publisher = 0;
      count = hatDao.count();
      List<PackageHat> dummyList = List.of(hatByPublisherAndName(publisher, "XXX"),
	  hatByPublisherAndName(publisher, "XXXX"), hatByPublisherAndName(publisher, "XXXXX"));
      dummyList.forEach(hat -> hatDao.save(hat));
      packages = hatDao.findAllByPublisherId(publisher);
      Assertions.assertEquals(count + dummyList.size(), hatDao.count());
//      hatDao.deleteAnyByPublisherIdAndValid(publisher, PackageHat.INVALID);
//      Assertions.assertEquals(count, hatDao.count());
//      dao.deleteAllByPublisherId(any.getPublisherId());
//      Assertions.assertEquals(count - packages.size(), dao.count());
}


}