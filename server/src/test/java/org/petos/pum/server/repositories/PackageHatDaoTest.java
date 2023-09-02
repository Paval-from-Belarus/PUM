package org.petos.pum.server.repositories;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.petos.pum.server.repositories.entities.PackageHat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

/**
 * @author Paval Shlyk
 * @since 23/08/2023
 */
@DataJpaTest
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@ContextConfiguration(classes = JpaConfig.class)
class PackageHatDaoTest {
@Autowired
public PackageHatDao dao;
@Test
public void start() {
      Optional<PackageHat> hat = dao.findById(1);
      if (hat.isPresent()) {
	    var realHat = hat.get();
	    realHat.setName("petos");
	    var aliases = realHat.getAliases();
	    aliases.forEach(System.out::println);
	    realHat = dao.save(realHat);
      }
      var list = dao.findAll();
      for (PackageHat packageHat : list) {
	    System.out.println(packageHat.getName());
      }
//      var hats = packageHatDao.findAll();
//      for (PackageHat hat : hats) {
//            int id = hat.getId();
//      }
//      try{
//	    packageHatDaoImpl.count();
//      } catch (Exception e) {
//	    System.out.println(e.getMessage());
//      }
}
}