package org.petos.pum.http.controller;

import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.petos.pum.http.service.PackageInfoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;


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
      return monoToResponse(packageInfoService.getHeaderInfo(packageName));
}

@GetMapping("/info/short/{package_id}")
public Mono<ResponseEntity<List<String>>> getShortPackageInfo(@PathVariable("package_id") int packageId) {
      return fluxToResponse(packageInfoService.getAllInstanceInfo(packageId));
}

@GetMapping("/info/full/{package_id}/version/{version}")
public Mono<ResponseEntity<String>> getFullPackageInfo(@PathVariable("package_id") int packageId,
						       @NotNull @PathVariable("version") String version) {
      return monoToResponse(packageInfoService.getFullInfo(packageId, version));
}

//the response holds exact information about server
@GetMapping("/info/payload/{package_id}/version/{version}/archive/{archive}")
public Mono<ResponseEntity<String>> getPayloadInfo(@PathVariable("package_id") int packageId,
						   @NotNull @PathVariable String version,
						   @NotNull @PathVariable String archive) {
      return monoToResponse(packageInfoService.getPayloadInfo(packageId, version, archive));
}

//@GetMapping("/info/payload/{package_id}/version/{version}")
//public Mono<PayloadInfo> get

@GetMapping("/repo/info")
public Object getRepositoryInfo(@NotNull @RequestParam("repo_name") String repoName) {

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

public static final String NOT_FOUND_MESSAGE = "Failed to found data for given input params";


private <T extends Message> Mono<ResponseEntity<String>> monoToResponse(Mono<T> mono) {
      return mono.map(data -> ResponseEntity.status(HttpStatus.OK).body(toJson(data)))
		 .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).body(NOT_FOUND_MESSAGE));
}

private <T extends Message> Mono<ResponseEntity<List<String>>> fluxToResponse(Flux<T> flux) {
      return flux.mapNotNull(this::toJson)
		 .collectList()
		 .map(list -> ResponseEntity.status(HttpStatus.OK).body(list))
		 .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).body(List.of(NOT_FOUND_MESSAGE)));
}
}