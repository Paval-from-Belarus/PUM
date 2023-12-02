package org.petos.pum.http.controller;

import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.petos.pum.http.service.PackageInfoService;
import org.petos.pum.http.service.PublisherInfoService;
import org.petos.pum.networks.dto.AuthorizationDto;
import org.petos.pum.networks.dto.PublicationDto;
import org.petos.pum.networks.dto.RegistrationDto;
import org.petos.pum.networks.dto.credentials.ValidationInfo;
import org.petos.pum.networks.dto.packages.ShortInstanceInfo;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.StringWriter;
import java.util.Collection;


/**
 * @author Paval Shlyk
 * @since 01/11/2023
 */
@RestController
@RequestMapping(value = "/api/pum", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class HttpApiGatewayController {
private final PackageInfoService packageInfoService;
private final PublisherInfoService publisherInfoService;

@GetMapping("/info/header")
public Mono<ResponseEntity<String>> getPackageInfo(@NotNull @RequestParam("package_name") String packageName) {
      return monoToResponse(packageInfoService.getHeaderInfo(packageName));
}

@GetMapping("/info/short/{package_id}")
public Mono<ResponseEntity<String>> getShortPackageInfo(@PathVariable("package_id") int packageId) {
      Flux<ShortInstanceInfo> flux = packageInfoService.getAllInstanceInfo(packageId);
      return fluxToResponse(flux);
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

//probably, replace such request to Authorization filter
@PostMapping("/publisher/auth")
public Mono<ResponseEntity<String>> authorizePublisher(@Validated @RequestBody AuthorizationDto dto) {
      return monoToResponse(publisherInfoService.authorize(dto));
}

@PostMapping("/publisher/reg")
public Mono<ResponseEntity<String>> registerPublisher(@Validated @RequestBody RegistrationDto dto) {
      return errorMonoToResponse(publisherInfoService.register(dto));
}

@PostMapping("/publisher/deploy")
public Mono<ResponseEntity<String>> publishPackage(@RequestParam("token") String token,
						   @Validated @RequestBody PublicationDto dto) {
      Mono<ValidationInfo> mono = publisherInfoService.validate(token);
      Mono<GeneratedMessageV3> response = mono.flatMap(validation -> {
	    if (validation.hasError()) {
		  return Mono.just(validation);
	    }
	    return packageInfoService.publish(validation.getPublisherId(), dto);
      });
      return monoToResponse(response);
}

@GetMapping("/repo/info")
public Object getRepositoryInfo(@NotNull @RequestParam("repo_name") String repoName) {
      return "repo info";
}
//@GetMapping("/repo/info/full")
//@GetMapping("/repo/info/archives")


@SneakyThrows
private <T extends Message> String elementToJson(T instance) {
      return JsonFormat.printer().print(instance);
}

@SneakyThrows
private <T extends Message> String listToJson(Collection<T> collection) {
      JsonFormat.Printer printer = JsonFormat.printer();
      StringWriter writer = new StringWriter();
      int restCount = collection.size();
      writer.append("[");
      for (T element : collection) {
	    printer.appendTo(element, writer);
	    restCount -= 1;
	    if (restCount != 0) {
		  writer.append(",");
	    }
      }
      writer.append("]");
      return writer.toString();
}

public static final String NOT_FOUND_MESSAGE = "Failed to found data for given input params";


private <T extends Message> Mono<ResponseEntity<String>> monoToResponse(Mono<T> mono) {
      return mono.map(this::dataToResponse)
		 .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).body(NOT_FOUND_MESSAGE));

}

private <T extends Message> Mono<ResponseEntity<String>> errorMonoToResponse(Mono<T> mono) {
      return mono.map(error -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(elementToJson(error)))
		 .defaultIfEmpty(ResponseEntity.ok().build());
}


private <T extends Message> ResponseEntity<String> dataToResponse(T instance) {
      return ResponseEntity.status(HttpStatus.OK).body(elementToJson(instance));
}

private <T extends Message> Mono<ResponseEntity<String>> fluxToResponse(Flux<T> flux) {
      return flux.collectList()
		 .mapNotNull(this::listToJson)
		 .map(json -> ResponseEntity.status(HttpStatus.OK).body(json))
		 .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).body(NOT_FOUND_MESSAGE));

}
}