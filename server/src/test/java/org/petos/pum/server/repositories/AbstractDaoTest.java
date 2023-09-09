package org.petos.pum.server.repositories;

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Paval Shlyk
 * @since 09/09/2023
 */

@DataJpaTest
@ActiveProfiles("test")
@ContextConfiguration(classes = JpaConfig.class)
public abstract class AbstractDaoTest {
}
