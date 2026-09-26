package com.p2p.escrow.config;

import com.p2p.escrow.domain.Enums.KycStatus;
import com.p2p.escrow.repo.Repositories.Users;
import java.util.UUID;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class KycTradingInterceptor implements WebMvcConfigurer {
  private final Users users;

  public KycTradingInterceptor(Users users) {
    this.users = users;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(new TradingGuard(users))
        .addPathPatterns("/api/orders/*/accept", "/api/escrows/*/paid", "/api/escrows/*/release");
  }

  private static class TradingGuard implements HandlerInterceptor {
    private final Users users;

    TradingGuard(Users users) {
      this.users = users;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
      if (!"POST".equalsIgnoreCase(request.getMethod())) {
        return true;
      }

      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication == null) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
      }

      UUID userId = UUID.fromString(authentication.getName());
      boolean verified = users.findById(userId)
          .map(user -> user.kycStatus == KycStatus.VERIFIED && !user.suspended)
          .orElse(false);
      if (!verified) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "KYC verification is required to trade");
      }
      return true;
    }
  }
}
