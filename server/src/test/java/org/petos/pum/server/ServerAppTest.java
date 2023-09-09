package org.petos.pum.server;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationContextFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Paval Shlyk
 * @since 09/09/2023
 */
@SpringBootTest
class ServerAppTest {
@Autowired
ApplicationContext context;

@Test
public void contextLoads() {
      Assertions.assertNotNull(context);
}
}