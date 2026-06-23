package com.looky.api.support.error;

import com.looky.api.support.response.ApiResponse;
import com.looky.common.exception.ErrorCode;
import com.looky.common.exception.LookyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(LookyException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleLookyException(LookyException exception) {
        ErrorCode errorCode = exception.errorCode();
        return ResponseEntity
                .status(HttpStatus.valueOf(errorCode.httpStatus()))
                .body(ApiResponse.fail(exception.getMessage(), ErrorResponse.of(errorCode.name())));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleValidationException(MethodArgumentNotValidException exception) {
        List<ErrorResponse.FieldErrorPayload> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ErrorResponse.FieldErrorPayload(error.getField(), error.getDefaultMessage()))
                .toList();

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.fail(
                        ErrorCode.VALIDATION_ERROR.message(),
                        ErrorResponse.of(ErrorCode.VALIDATION_ERROR.name(), errors)
                ));
    }
}
