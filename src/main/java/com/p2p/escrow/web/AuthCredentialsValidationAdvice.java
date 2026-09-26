package com.p2p.escrow.web;

import jakarta.validation.Validator;
import java.lang.reflect.Type;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice;
import org.springframework.web.server.ResponseStatusException;

@ControllerAdvice
public class AuthCredentialsValidationAdvice implements RequestBodyAdvice {
  private final Validator validator;

  public AuthCredentialsValidationAdvice(Validator validator) {
    this.validator = validator;
  }

  @Override
  public boolean supports(
      MethodParameter methodParameter,
      Type targetType,
      Class<? extends HttpMessageConverter<?>> converterType) {
    return AuthController.Credentials.class.equals(targetType);
  }

  @Override
  public Object afterBodyRead(
      Object body,
      HttpInputMessage inputMessage,
      MethodParameter parameter,
      Type targetType,
      Class<? extends HttpMessageConverter<?>> converterType) {
    return validator.validate(body).stream()
        .findFirst()
        .map(violation -> {
          throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST,
              violation.getPropertyPath() + " " + violation.getMessage());
        })
        .orElse(body);
  }

  @Override
  public HttpInputMessage beforeBodyRead(
      HttpInputMessage inputMessage,
      MethodParameter parameter,
      Type targetType,
      Class<? extends HttpMessageConverter<?>> converterType) {
    return inputMessage;
  }

  @Override
  public Object handleEmptyBody(
      Object body,
      HttpInputMessage inputMessage,
      MethodParameter parameter,
      Type targetType,
      Class<? extends HttpMessageConverter<?>> converterType) {
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
  }
}
