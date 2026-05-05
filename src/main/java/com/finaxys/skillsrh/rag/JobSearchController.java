package com.finaxys.skillsrh.rag;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rag")
public class JobSearchController {

    private final JobSearchService jobSearchService;

    public JobSearchController(JobSearchService jobSearchService) {
        this.jobSearchService = jobSearchService;
    }

    /**
     * GET /api/rag/search?jobTitle=DevOps&results=5
     * Recherche les candidats les plus pertinents pour un intitulé de poste,
     * avec skill gap calculé par le Python RAG.
     */
    @GetMapping("/search")
    public ResponseEntity<JobSearchResponse> search(
        @RequestParam String jobTitle,
        @RequestParam(defaultValue = "5") int results
    ) {
        JobSearchResponse response = jobSearchService.searchByJobTitle(jobTitle, results);
        return ResponseEntity.ok(response);
    }
}
