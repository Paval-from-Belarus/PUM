package org.petos.pum.server.network;

import lombok.RequiredArgsConstructor;
import org.petos.pum.server.services.LocalService;
import org.petos.pum.server.services.PackageService;
import org.petos.pum.server.services.PublisherService;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.handler.annotation.Payload;
import org.petos.pum.networks.requests.IdRequest;
import org.petos.pum.networks.requests.VersionRequest;

import java.io.OutputStream;


/**
 * @author Paval Shlyk
 * @since 16/08/2023
 */
@MessageEndpoint("repository")
@RequiredArgsConstructor
public class ManagerController {
private final PackageService packageService;
private final PublisherService publisherService;
private final LocalService localService;
@ServiceActivator(inputChannel = "input", outputChannel = "output")
public Integer getPackageId(@Payload IdRequest request) {
      return 1;
}
@ServiceActivator(inputChannel = "input", outputChannel = "output")
public Integer getVersionId(@Payload VersionRequest request) {
      return 0;
}
public Object getAvailablePackages() {
      return null;
}
public Object getLocalInfo(){
      return null;
}
public Object getPackageInfo() {
      return null;
}
public OutputStream uploadLocalPackage() {
      return null;
}
public Object authorizePublisher() {
      return null;
}
public void publishPackage() {
      return;
}
}
