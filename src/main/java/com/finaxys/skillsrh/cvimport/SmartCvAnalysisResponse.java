package com.finaxys.skillsrh.cvimport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SmartCvAnalysisResponse(
    Identity identite,
    @JsonProperty("titre_professionnel") String titreProfessionnel,
    @JsonProperty("type_poste") String typePoste,
    String profil,
    List<SkillItem> competences,
    Metadata metadata
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Identity(
        String nom,
        String prenom,
        String email
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SkillItem(
        String nom,
        String categorie,
        String niveau,
        @JsonProperty("annees_experience") Integer anneesExperience,
        String justification
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Metadata(
        @JsonProperty("source_fichier") String sourceFichier
    ) {
    }
}
