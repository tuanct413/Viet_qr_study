package com.example.demo.exception;

import com.example.dto.ResponseMessageDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final String ERROR_STATUS = "ERROR";
    private static final String FAILED_STATUS = "FAILED";

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ResponseMessageDTO> handleNoHandlerFoundException(NoHandlerFoundException ex, HttpServletRequest request) {
        log.error("handleNoHandlerFoundException: ERROR: {} - URI: {} - Method: {} at: {}", 
                ex.getMessage(), request.getRequestURI(), request.getMethod(), System.currentTimeMillis());
        
        return new ResponseEntity<>(new ResponseMessageDTO(ERROR_STATUS, "Path not found: " + ex.getRequestURL()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ResponseMessageDTO> handleResourceNotFoundException(ResourceNotFoundException ex, HttpServletRequest request) {
        log.error("handleResourceNotFoundException: ERROR: {} - URI: {} at: {}", 
                ex.getMessage(), request.getRequestURI(), System.currentTimeMillis());
        
        return new ResponseEntity<>(new ResponseMessageDTO(ERROR_STATUS, ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseMessageDTO> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String errors = ex.getBindingResult().getAllErrors().stream()
                .map((error) -> {
                    String fieldName = ((FieldError) error).getField();
                    String errorMessage = error.getDefaultMessage();
                    return fieldName + ": " + errorMessage;
                })
                .collect(Collectors.joining(", "));
        
        // Log INFO theo yêu cầu: Đây là lỗi đầu vào của người dùng, không phải lỗi hệ thống
        log.info("VALIDATION_FAILED: {} - URI: {} at: {}", 
                errors, request.getRequestURI(), System.currentTimeMillis());
        
        return new ResponseEntity<>(new ResponseMessageDTO(FAILED_STATUS, "Validation failed: " + errors), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseMessageDTO> handleGlobalException(Exception ex, HttpServletRequest request) {
        String exceptionType = ex.getClass().getSimpleName();
        String url = request.getRequestURL() != null ? request.getRequestURL().toString() : request.getRequestURI();
        
        log.error("Vị trí rõ ràng: URL đối tác ({}) - Loại lỗi ({}): {}", 
                url, exceptionType, ex.getMessage() != null ? ex.getMessage() : "No message");
        log.error("Stack trace: ", ex);
        
        return new ResponseEntity<>(new ResponseMessageDTO(ERROR_STATUS, "Internal Server Error: " + ex.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
