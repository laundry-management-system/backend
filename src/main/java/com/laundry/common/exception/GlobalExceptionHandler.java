package com.laundry.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/** Mọi lỗi trả về cùng định dạng ProblemDetail (RFC 9457) cho FE. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String BOOKING_PATH = "/api/customer-portal/bookings";

    /** Câu 401: đường đặt đơn có câu riêng cho khách (R3), còn lại giữ câu chung. */
    public static String unauthorizedDetail(String uri) {
        return isBookingPath(uri) ? "Vui lòng đăng nhập để đặt đơn" : "Chưa đăng nhập hoặc token không hợp lệ";
    }

    public static boolean isBookingPath(String uri) {
        return uri != null && (uri.equals(BOOKING_PATH) || uri.startsWith(BOOKING_PATH + "/"));
    }

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFound(NotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusiness(BusinessException ex) {
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, ex.getMessage());
        if (ex.getErrors() != null && !ex.getErrors().isEmpty()) pd.setProperty("errors", ex.getErrors());
        else if (ex.getField() != null) pd.setProperty("errors", Map.of(ex.getField(), ex.getMessage()));
        return pd;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadable(HttpMessageNotReadableException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ");
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ProblemDetail handleConflict(ResourceConflictException ex) {
        return problem(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ProblemDetail handleTooManyRequests(TooManyRequestsException ex) {
        return problem(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ");
        pd.setProperty("errors", errors);
        return pd;
    }

    // Tham số query / path sai kiểu (enum lạ, chữ ở chỗ số) là lỗi của người gọi, không phải lỗi hệ thống
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = "channel".equals(ex.getName())
                ? "Kênh không hợp lệ"
                : "Giá trị không hợp lệ cho tham số " + ex.getName();
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, message);
        pd.setProperty("errors", Map.of(ex.getName(), message));
        return pd;
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleUploadTooLarge(MaxUploadSizeExceededException ex) {
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "File tối đa 5MB");
        pd.setProperty("errors", Map.of("file", "File tối đa 5MB"));
        return pd;
    }

    // Gọi sai method (vd DELETE khi API không hỗ trợ) là lỗi của người gọi, không phải lỗi hệ thống
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ProblemDetail handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return problem(HttpStatus.METHOD_NOT_ALLOWED, "Phương thức không được hỗ trợ");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, unauthorizedDetail(request.getRequestURI()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        return problem(HttpStatus.FORBIDDEN, "Không có quyền thực hiện thao tác này");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống, vui lòng thử lại sau");
    }

    private static ProblemDetail problem(HttpStatus status, String detail) {
        return ProblemDetail.forStatusAndDetail(status, detail);
    }
}
