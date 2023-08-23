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
@Autowired
public PackageHatDao packageHatDaoImpl;
@Test
public void start() {
      	var hat = new PackageHat();
//      var hats = packageHatDao.findAll();
//      for (PackageHat hat : hats) {
//            int id = hat.getId();
//      }
      try{
	    packageHatDaoImpl.count();
      } catch (Exception e) {
	    System.out.println(e.getMessage());
      }
}
}