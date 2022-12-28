package com.rajesh.files.fileprocessor.exception;

import com.rajesh.files.fileprocessor.domain.ProcessStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(value = {InputFileMissingException.class, BadInputDataException.class, BadOutputDataException.class, EmptyInputFileException.class, MissingHeaderInputFileException.class})
    public ResponseEntity<ProcessStatus> handleException(RuntimeException rte, WebRequest wReq) {
        ProcessStatus status = new ProcessStatus();
        status.setCode("FAIL");
        status.setDescription(rte.getMessage());
        return new ResponseEntity<>(status, HttpStatus.EXPECTATION_FAILED);
    }
}
