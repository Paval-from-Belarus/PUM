package org.petos.pum.server.repositories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.petos.pum.server.repositories.JpaConfig;
import org.petos.pum.server.repositories.PackageHatDao;
import org.petos.pum.server.repositories.entities.PackageHat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Profile;
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
public PackageHatDao packageHatDao;
@Test
public void start() {
      Optional<PackageHat> hat = packageHatDao.findById(1);
      if (hat.isPresent()) {
	    var realHat = hat.get();
	    realHat.setName("petos");
	    realHat = packageHatDao.save(realHat);
      }
      var list = packageHatDao.findAll();
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