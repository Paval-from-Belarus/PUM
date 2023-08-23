package org.petos.pum.server.services;

import org.petos.pum.server.repositories.entities.PackageHat;
import org.petos.pum.server.repositories.PackageHatDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import requests.IdRequest;
import requests.VersionRequest;



/**
 * @author Paval Shlyk
 * @since 16/08/2023
 */
@Service("repository")
public class ManagerService {
@Autowired
public ManagerService(PackageHatDao packageHatDao) {
      this.storage = packageHatDao;
}

@ServiceActivator(inputChannel = "input", outputChannel = "output")
public Integer onPackageIdRequest(@Payload IdRequest request) {
      storage.findAll();
      return 1;
}
@ServiceActivator(inputChannel = "input", outputChannel = "output")
public Integer onVersionIdRequest(@Payload VersionRequest request) {
      return 0;
}
private final PackageHatDao storage;

}
