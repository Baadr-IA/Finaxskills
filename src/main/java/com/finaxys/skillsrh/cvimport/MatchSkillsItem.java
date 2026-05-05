package com.finaxys.skillsrh.cvimport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MatchSkillsItem(
    @JsonProperty("skill_cv")   String skillCv,
    @JsonProperty("skill_ref")  String skillRef,
    double score,
    @JsonProperty("match_type") String matchType
) {
    public boolean isMatched() {
        return skillRef != null && !"NO_MATCH".equals(matchType);
    }
}
