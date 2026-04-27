export type CvImportAction = 'CREATE' | 'UPDATE';
export type CvImportSkillResolution = 'REUSE' | 'CREATE';

export interface CvImportCollaboratorDto {
  firstName: string;
  lastName: string;
  email: string;
  jobTitle: string | null;
  keycloakId: string | null;
}

export interface CvImportSkillDraftDto {
  skillId: number | null;
  skillName: string;
  categoryId: number | null;
  categoryName: string;
  hrLevel: number | null;
  hrNote: string | null;
  sourceLevel: string | null;
  yearsExperience: number | null;
  resolution: CvImportSkillResolution;
  selected: boolean;
}

export interface CvImportSourceDto {
  sourceFilename: string;
  professionalTitle: string | null;
  profileSummary: string | null;
}

export interface CvImportDraftDto {
  action: CvImportAction;
  matchedCollaboratorId: number | null;
  matchReason: string;
  collaborator: CvImportCollaboratorDto;
  skills: CvImportSkillDraftDto[];
  source: CvImportSourceDto;
}

export interface CvImportCommitRequestDto {
  targetCollaboratorId: number | null;
  collaborator: CvImportCollaboratorDto;
  skills: CvImportSkillDraftDto[];
}

export interface CvImportCommitResponseDto {
  collaboratorId: number;
  action: CvImportAction;
  importedSkills: number;
}
