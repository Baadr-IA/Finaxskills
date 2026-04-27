package com.finaxys.skillsrh.controller;

import com.finaxys.skillsrh.cvimport.CvImportService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/cv-imports")
public class CvImportController {

    private final CvImportService cvImportService;

    public CvImportController(CvImportService cvImportService) {
        this.cvImportService = cvImportService;
    }

    @PostMapping(value = "/draft", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(
        "@permissions.has(authentication, 'COLLABORATORS', 'CREATE', 'ALL') " +
            "or @permissions.has(authentication, 'COLLABORATORS', 'UPDATE', 'ALL')"
    )
    public CvImportService.CvImportDraft createDraft(@RequestPart("file") MultipartFile file) {
        return cvImportService.analyze(file);
    }

    @PostMapping("/commit")
    @PreAuthorize(
        "@permissions.has(authentication, 'COLLABORATORS', 'CREATE', 'ALL') " +
            "or @permissions.has(authentication, 'COLLABORATORS', 'UPDATE', 'ALL')"
    )
    public CvImportService.CvImportCommitResponse commit(@Valid @RequestBody CvImportService.CvImportCommitRequest request) {
        return cvImportService.commit(request);
    }
}
