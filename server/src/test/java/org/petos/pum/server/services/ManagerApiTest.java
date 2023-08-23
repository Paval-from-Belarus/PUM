package org.petos.pum.server.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.petos.pum.server.services.ManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;
import requests.IdRequest;

/**
 * @author Paval Shlyk
 * @since 22/08/2023
 */
//@ExtendWith(SpringExtension.class)
//@ContextConfiguration(classes = {ServerTestConfig.class})
@SpringBootTest(classes = {org.petos.pum.server.ServerApp.class})

//@SpringBootTest
//@ContextHierarchy({
//               @ContextConfiguration(classes = ServerTestConfig.class)
//})
public class ManagerApiTest {
@Autowired
ManagerService service;

@Test
@Rollback(value = false)
public void test() {
      //NdsResolver nds = Mockito.mock(NdsResolver.class);
try {
      service.onPackageIdRequest(new IdRequest("asd"));
} catch (DataAccessException e) {
      System.out.println(e.getMessage());
}
}

}
