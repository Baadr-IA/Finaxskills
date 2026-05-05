package com.finaxys.skillsrh.rag;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JobSearchResponse(
    @JsonProperty("jobTitle")        String jobTitle,
    @JsonProperty("requiredSkills")  List<String> requiredSkills,
    @JsonProperty("knownPoste")      boolean knownPoste,
    @JsonProperty("candidates")      List<JobSearchCandidate> candidates
) {}
