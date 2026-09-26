package com.p2p.escrow.web;

import com.p2p.escrow.domain.AppUser;
import com.p2p.escrow.domain.Enums.KycStatus;
import com.p2p.escrow.domain.Enums.Role;
import com.p2p.escrow.repo.Repositories.Users;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class KycDocumentController {
  private static final long MAX_DOCUMENT_BYTES = 5 * 1024 * 1024;
  private final Users users;
  private final Path storageDirectory;

  public KycDocumentController(Users users, @Value("${app.kyc-storage-dir:./data/kyc}") String storageDirectory) {
    this.users = users;
    this.storageDirectory = Path.of(storageDirectory).toAbsolutePath().normalize();
  }

  @PostMapping(value = "/kyc/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public KycDocumentResponse upload(@RequestParam("document") MultipartFile document, Authentication authentication) {
    AppUser user = currentUser(authentication);
    if (user.kycStatus == KycStatus.VERIFIED) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "KYC has already been verified");
    }
    if (document.isEmpty() || document.getSize() > MAX_DOCUMENT_BYTES) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document must be between 1 byte and 5 MB");
    }

    String extension = extensionFor(document.getContentType());
    try {
      Files.createDirectories(storageDirectory);
      deleteExistingDocument(user.id);
      Files.copy(document.getInputStream(), documentPath(user.id, extension), StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException exception) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to store KYC document");
    }

    user.kycStatus = KycStatus.PENDING;
    users.save(user);
    return new KycDocumentResponse(user.id, user.kycStatus.name(), document.getOriginalFilename());
  }

  @GetMapping("/admin/kyc/pending")
  public List<KycReviewItem> pending(Authentication authentication) {
    requireAdmin(authentication);
    return users.findAll().stream()
        .filter(user -> user.kycStatus == KycStatus.PENDING)
        .map(user -> new KycReviewItem(user.id, user.email, hasDocument(user.id)))
        .toList();
  }

  @GetMapping("/admin/kyc/{userId}/document")
  public ResponseEntity<FileSystemResource> document(@PathVariable UUID userId, Authentication authentication) {
    requireAdmin(authentication);
    Path document = findDocument(userId);
    MediaType mediaType = mediaType(document);
    return ResponseEntity.ok()
        .contentType(mediaType)
        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + document.getFileName() + "\"")
        .body(new FileSystemResource(document));
  }

  private AppUser currentUser(Authentication authentication) {
    if (authentication == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
    }
    return users.findById(UUID.fromString(authentication.getName()))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account not found"));
  }

  private void requireAdmin(Authentication authentication) {
    if (currentUser(authentication).role != Role.ADMIN) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
    }
  }

  private String extensionFor(String contentType) {
    if (MediaType.APPLICATION_PDF_VALUE.equals(contentType)) return ".pdf";
    if (MediaType.IMAGE_JPEG_VALUE.equals(contentType)) return ".jpg";
    if (MediaType.IMAGE_PNG_VALUE.equals(contentType)) return ".png";
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PDF, JPEG, and PNG documents are accepted");
  }

  private Path documentPath(UUID userId, String extension) {
    return storageDirectory.resolve(userId + extension);
  }

  private boolean hasDocument(UUID userId) {
    try {
      return findDocument(userId) != null;
    } catch (ResponseStatusException ignored) {
      return false;
    }
  }

  private Path findDocument(UUID userId) {
    if (!Files.isDirectory(storageDirectory)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "KYC document not found");
    }
    try (Stream<Path> documents = Files.list(storageDirectory)) {
      return documents.filter(path -> path.getFileName().toString().startsWith(userId.toString() + "."))
          .findFirst()
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "KYC document not found"));
    } catch (IOException exception) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to retrieve KYC document");
    }
  }

  private void deleteExistingDocument(UUID userId) throws IOException {
    if (!Files.isDirectory(storageDirectory)) return;
    try (Stream<Path> documents = Files.list(storageDirectory)) {
      for (Path document : documents.filter(path -> path.getFileName().toString().startsWith(userId + ".")).toList()) {
        Files.deleteIfExists(document);
      }
    }
  }

  private MediaType mediaType(Path document) {
    try {
      String detected = Files.probeContentType(document);
      return detected == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(detected);
    } catch (IOException exception) {
      return MediaType.APPLICATION_OCTET_STREAM;
    }
  }

  public record KycDocumentResponse(UUID userId, String status, String fileName) {}
  public record KycReviewItem(UUID userId, String email, boolean documentAttached) {}
}
