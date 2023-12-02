package org.petos.pum.repository.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.petos.pum.repository.service.RepositoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;

/**
 * @author Paval Shlyk
 * @since 02/12/2023
 */
@Controller
@RequestMapping(produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
@RequiredArgsConstructor
public class HttpRepositoryController {
private final RepositoryService repositoryService;

@GetMapping("/download")
public void shareFile(HttpServletResponse response, @RequestParam("secret") @NotNull String token) throws IOException {
      repositoryService.download(token.getBytes(), response.getOutputStream());
}

@PutMapping("/upload")
public void saveFile(
    @RequestParam("secret") @NotNull String token,
    MultipartFile file,
    HttpServletResponse response) throws IOException {
      repositoryService.upload(token.getBytes(), file.getInputStream());
      response.setStatus(HttpServletResponse.SC_CREATED);
}
}
