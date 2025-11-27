package com.side.hhplusecommerce.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
@Slf4j
class ApiControllerAdvice extends ResponseEntityExceptionHandler {
    @ExceptionHandler(value = CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {
        log.warn("[CustomException] code={}, message={}, errorCode={}, httpStatus={}",
                e.getCode(),
                e.getMessage(),
                e.getErrorCode(),
                e.getErrorCode().getHttpStatus(),
                e);
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(new ErrorResponse(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("[UnhandledException] type={}, message={}, cause={}",
                e.getClass().getSimpleName(),
                e.getMessage(),
                e.getCause() != null ? e.getCause().toString() : "N/A",
                e);
        return ResponseEntity
                .status(500)
                .body(new ErrorResponse("500", "에러가 발생했습니다."));
    }
}
