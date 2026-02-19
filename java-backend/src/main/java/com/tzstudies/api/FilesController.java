package com.tzstudies.api;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class FilesController {

  @Value("${app.files.examsDir:../exams}")
  private String examsDir;

  @Value("${app.files.answerKeysDir:../answer_keys}")
  private String answerKeysDir;

  @GetMapping("/health")
  public Map<String, Object> health() {
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("status", "ok");
    out.put("service", "tzstudies-java-api");
    return out;
  }

  @GetMapping("/exams")
  public List<String> listExams() throws IOException {
    return listPdfFiles(Paths.get(examsDir));
  }

  @GetMapping("/answer-keys")
  public List<String> listAnswerKeys() throws IOException {
    return listPdfFiles(Paths.get(answerKeysDir));
  }

  @GetMapping("/file/{type}/{filename:.+}")
  public ResponseEntity<Resource> download(
      @PathVariable String type,
      @PathVariable String filename
  ) throws IOException {

    Path base = resolveBaseDir(type);
    Path file = base.resolve(filename).normalize();

    // basic path traversal guard
    if (!file.startsWith(base)) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    if (!Files.exists(file) || Files.isDirectory(file)) {
      return ResponseEntity.notFound().build();
    }

    Resource resource = new UrlResource(file.toUri());

    String contentType = "application/octet-stream";
    if (filename.toLowerCase().endsWith(".pdf")) {
      contentType = "application/pdf";
    }

    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(contentType))
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .body(resource);
  }

  private Path resolveBaseDir(String type) {
    if ("exams".equalsIgnoreCase(type)) {
      return Paths.get(examsDir).toAbsolutePath().normalize();
    }
    if ("keys".equalsIgnoreCase(type) || "answer-keys".equalsIgnoreCase(type)) {
      return Paths.get(answerKeysDir).toAbsolutePath().normalize();
    }
    // default to exams for safety
    return Paths.get(examsDir).toAbsolutePath().normalize();
  }

  private List<String> listPdfFiles(Path dir) throws IOException {
    Path abs = dir.toAbsolutePath().normalize();
    if (!Files.exists(abs) || !Files.isDirectory(abs)) {
      return List.of();
    }

    try (var stream = Files.list(abs)) {
      return stream
          .filter(p -> !Files.isDirectory(p))
          .map(p -> p.getFileName().toString())
          .filter(name -> name.toLowerCase().endsWith(".pdf"))
          .sorted(String::compareToIgnoreCase)
          .toList();
    }
  }
}
