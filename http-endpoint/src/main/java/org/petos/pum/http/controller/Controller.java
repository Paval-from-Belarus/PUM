package org.petos.pum.http.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Paval Shlyk
 * @since 01/11/2023
 */
@RestController("/api/v1/pum/")
public class Controller {
@GetMapping("/info/all")
public Object getAllInfo() {
      return null;
}

@GetMapping("/info/{package_id}")
public Object getPackageInfo(@PathVariable("package_id") long packageId) {
      return packageId;
}
@GetMapping("/info/{package_id}/short")
public Object getShortPackageInf(@PathVariable("package_id") long packageId) {
      return null;
}
@GetMapping("/repo/info")
public Object getRepositoryInfo() {
      return null;
}
//the response holds exact information about server
@GetMapping("/payload")
public Object getPayloadInfo(@Validated Object payloadInfo) {
      return null;
}
//probably, replace such request to Authorization filter
@PostMapping("/publisher/auth")
public Object authorizePublisher(@Validated Object publisherInfo) {
      return null;
}
@PostMapping("/publisher/reg")
public Object registerPublisher(@Validated Object registrationPublisherInfo) {
      return null;
}
@PostMapping("/publisher/publish")
public Object publishPackage(@Validated Object publicationInfo) {
      return null;
}

}
