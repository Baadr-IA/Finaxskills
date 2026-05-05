package com.finaxys.skillsrh.rag;

import com.finaxys.skillsrh.cvimport.SmartCvClient;
import org.springframework.stereotype.Service;

@Service
public class JobSearchService {

    private final SmartCvClient smartCvClient;

    public JobSearchService(SmartCvClient smartCvClient) {
        this.smartCvClient = smartCvClient;
    }

    public JobSearchResponse searchByJobTitle(String jobTitle, int maxResults) {
        return smartCvClient.searchJob(jobTitle, maxResults);
    }
}
