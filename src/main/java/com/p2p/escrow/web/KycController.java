package com.p2p.escrow.web;

import com.p2p.escrow.domain.AppUser;
import com.p2p.escrow.domain.Enums.KycStatus;
import com.p2p.escrow.domain.Enums.Role;
import com.p2p.escrow.repo.Repositories.Users;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class KycController {
  private final Users users;

  public KycController(Users users) {
    this.users = users;
  }

  @GetMapping("/kyc/status")
  public KycStatusResponse status(Authentication authentication) {
    return response(currentUser(authentication));
  }

  @PostMapping("/kyc/submit")
  public KycStatusResponse submit(
      @Valid @RequestBody KycSubmission submission,
      Authentication authentication) {
    AppUser user = currentUser(authentication);
    if (user.kycStatus == KycStatus.VERIFIED) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "KYC has already been verified");
    }

    user.kycStatus = KycStatus.PENDING;
    users.save(user);
    return response(user);
  }

  @PostMapping("/admin/kyc/{userId}/verify")
  public KycStatusResponse verify(@PathVariable UUID userId, Authentication authentication) {
    AppUser target = targetUser(userId, authentication);
    target.kycStatus = KycStatus.VERIFIED;
    users.save(target);
    return response(target);
  }

  @PostMapping("/admin/kyc/{userId}/reject")
  public KycStatusResponse reject(@PathVariable UUID userId, Authentication authentication) {
    AppUser target = targetUser(userId, authentication);
    target.kycStatus = KycStatus.REJECTED;
    users.save(target);
    return response(target);
  }

  private AppUser targetUser(UUID userId, Authentication authentication) {
    requireAdmin(authentication);
    return users.findById(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
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

  private KycStatusResponse response(AppUser user) {
    return new KycStatusResponse(user.id, user.kycStatus.name(), user.trustScore);
  }

  public record KycSubmission(
      @NotBlank String legalName,
      @NotBlank String country,
      @NotBlank String documentType) {}

  public record KycStatusResponse(UUID userId, String status, int trustScore) {}
}
