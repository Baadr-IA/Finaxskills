export interface JobSearchCandidateDto {
  candidateId: string;
  name: string;
  jobTitle: string;
  matchedSkills: string[];
  missingSkills: string[];
  coverageScore: number;
  relevanceScore: number;
}

export interface JobSearchResponseDto {
  jobTitle: string;
  requiredSkills: string[];
  knownPoste: boolean;
  candidates: JobSearchCandidateDto[];
}
