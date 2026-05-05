package com.finaxys.skillsrh.rag;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JobSearchCandidate(
    @JsonProperty("candidateId")    String candidateId,
    @JsonProperty("name")           String name,
    @JsonProperty("jobTitle")       String jobTitle,
    @JsonProperty("matchedSkills")  List<String> matchedSkills,
    @JsonProperty("missingSkills")  List<String> missingSkills,
    @JsonProperty("coverageScore")  double coverageScore,
    @JsonProperty("relevanceScore") double relevanceScore
) {}
