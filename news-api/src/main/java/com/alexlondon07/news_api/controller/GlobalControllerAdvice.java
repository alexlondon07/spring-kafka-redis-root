package com.alexlondon07.news_api.controller;

import com.alexlondon07.news_api.models.dto.response.enums.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.time.LocalDate;
import java.util.Collections;

import static com.alexlondon07.news_api.models.dto.response.ErrorType.FUNCTIONAL;
import static com.alexlondon07.news_api.utils.ErrorCatalog.INVALID_PARAMETERS;

@Slf4j
@RestController
public class GlobalControllerAdvice {

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ErrorResponse handlerMethodValidationException(HandlerMethodValidationException e) {
        getErrorMessage(e.getMessage());
        return ErrorResponse.builder()
                .code(INVALID_PARAMETERS.getCode())
                .errorType(FUNCTIONAL)
                .message(   INVALID_PARAMETERS.getMessage())
                .details(Collections.singletonList(e.getMessage()))
                .timestamp(String.valueOf(System.currentTimeMillis()))
                .build();
    }



    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ErrorResponse handlerMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        getErrorMessage(e.getMessage());
        BindingResult bindingResult = e.getBindingResult();
        return ErrorResponse.builder()
                .code(INVALID_PARAMETERS.getCode())
                .errorType(FUNCTIONAL)
                .message(   INVALID_PARAMETERS.getMessage())
                .details(bindingResult.getFieldErrors().stream()
                        .map(DefaultMessageSourceResolvable::getDefaultMessage)
                        .toList())
                .timestamp(LocalDate.now().toString())
                .build();
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ErrorResponse handlerException(Exception e) {
        getErrorMessage(e.getMessage());
        return ErrorResponse.builder()
                .code(INVALID_PARAMETERS.getCode())
                .errorType(FUNCTIONAL)
                .message(   INVALID_PARAMETERS.getMessage())
                .details(Collections.singletonList(e.getMessage()))
                .timestamp(LocalDate.now().toString())
                .build();
    }

    private static void getErrorMessage(String e) {
        log.error("Error : {}", e);
    }
}
