package org.petos.pum.server.services;

import org.junit.jupiter.api.Test;
import org.petos.pum.server.network.ManagerController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.test.annotation.Rollback;
import org.petos.pum.networks.requests.IdRequest;

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
ManagerController service;

@Test
@Rollback(value = false)
public void test() {
      //NdsResolver nds = Mockito.mock(NdsResolver.class);
try {
      service.getPackageId(new IdRequest("asd"));
} catch (DataAccessException e) {
      System.out.println(e.getMessage());
}
}

}
