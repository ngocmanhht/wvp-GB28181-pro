package com.genersoft.iot.vmp.conf;

import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Global exception handling
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Default exception handling
     * @param e Abnormal
     * @return Return results uniformly
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public WVPResult<String> exceptionHandler(Exception e) {
        log.error("[global exception]： ", e);
        return WVPResult.fail(ErrorCode.ERROR500.getCode(), e.getMessage());
    }

    /**
     * Default exception handling
     * @param e Abnormal
     * @return Return results uniformly
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public WVPResult<String> exceptionHandler(MaxUploadSizeExceededException e) {
        return WVPResult.fail(ErrorCode.ERROR403.getCode(), "File too large");
    }

    /**
     * Default exception handling
     * @param e Abnormal
     * @return Return results uniformly
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public WVPResult<String> exceptionHandler(NoResourceFoundException e) {
        return WVPResult.fail(ErrorCode.ERROR404);
    }

    /**
     * Default exception handling
     * @param e Abnormal
     * @return Return results uniformly
     */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public WVPResult<String> exceptionHandler(IllegalStateException e) {
        return WVPResult.fail(ErrorCode.ERROR400);
    }

    /**
     * Default exception handling
     * @param e Abnormal
     * @return Return results uniformly
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public WVPResult<String> exceptionHandler(HttpRequestMethodNotSupportedException e) {
        return WVPResult.fail(ErrorCode.ERROR400);
    }
    /**
     * Assertion exception handling
     * @param e Abnormal
     * @return Return results uniformly
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.OK)
    public WVPResult<String> exceptionHandler(IllegalArgumentException e) {
        return WVPResult.fail(ErrorCode.ERROR100.getCode(), e.getMessage());
    }


    /**
     * Custom exception handling to handle errors returned in the controller
     * @param e Abnormal
     * @return Return results uniformly
     */
    @ExceptionHandler(ControllerException.class)
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<WVPResult<String>> exceptionHandler(ControllerException e) {
        return new ResponseEntity<>(WVPResult.fail(e.getCode(), e.getMsg()), HttpStatus.OK);
    }

    /**
     * Login failed
     * @param e Abnormal
     * @return Return results uniformly
     */
    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<WVPResult<String>> exceptionHandler(BadCredentialsException e) {
        return new ResponseEntity<>(WVPResult.fail(ErrorCode.ERROR100.getCode(), e.getMessage()), HttpStatus.OK);
    }
}
