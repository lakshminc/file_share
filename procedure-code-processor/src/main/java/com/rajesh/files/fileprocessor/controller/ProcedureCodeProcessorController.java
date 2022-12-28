package com.rajesh.files.fileprocessor.controller;

import com.rajesh.files.fileprocessor.domain.ProcessStatus;
import com.rajesh.files.fileprocessor.service.ProcedureCodeProcessorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/file")
public class ProcedureCodeProcessorController {

    @Autowired
    ProcedureCodeProcessorService service;

    @GetMapping("/process")
    public ProcessStatus process(){
        ProcessStatus processStatus = null;
        try {
             processStatus = service.processProcData();
            System.out.println(processStatus);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return processStatus;
    }
}
