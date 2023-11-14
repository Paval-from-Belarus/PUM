package org.petos.pum.http.controller;

import com.google.gson.Gson;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import io.netty.handler.codec.http.HttpResponseStatus;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.petos.pum.http.service.PackageInfoService;
import org.petos.pum.networks.dto.packages.FullInstanceInfo;
import org.petos.pum.networks.dto.packages.HeaderInfo;
import org.petos.pum.networks.dto.packages.PayloadInfo;
import org.petos.pum.networks.dto.packages.ShortInstanceInfo;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


/**
 * @author Paval Shlyk
 * @since 01/11/2023
 */
@RestController
@RequestMapping("/api/pum")
@RequiredArgsConstructor
public class Controller {
private final PackageInfoService packageInfoService;

@GetMapping("/info/header")
public Mono<ResponseEntity<String>> getPackageInfo(@NotNull @RequestParam("package_name") String packageName) {
      return toResponse(packageInfoService.getHeaderInfo(packageName));
}

@GetMapping("/info/short/{package_id}")
public Flux<ShortInstanceInfo> getShortPackageInfo(@PathVariable("package_id") int packageId) {
      return packageInfoService.getAllInstanceInfo(packageId);
}

@GetMapping("/info/full/{package_id}/version/{version}")
public Mono<FullInstanceInfo> getFullPackageInfo(@PathVariable("package_id") int packageId,
						 @NotNull @PathVariable("version") String version) {
      return packageInfoService.getFullInfo(packageId, version);
}

//@GetMapping("/info/payload/{package_id}/version/{version}")
//public Mono<PayloadInfo> get

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

@PostMapping("/publisher/deploy")
public Object publishPackage(@Validated Object publicationInfo) {
      return null;
}

@SneakyThrows
private <T extends Message> String toJson(T instance) {
      return JsonFormat.printer().print(instance);
}

private <T extends Message> Mono<ResponseEntity<String>> toResponse(Mono<T> mono) {
      return mono.map(data -> ResponseEntity.status(HttpStatus.OK).body(toJson(data)));
//      return mono.map(data -> new ResponseEntity<>(toJson(data), HttpStatusCode.valueOf(HttpStatus.OK.value())));
}
}