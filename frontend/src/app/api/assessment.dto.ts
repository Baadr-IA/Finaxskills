export interface AssessmentDto {
  collaboratorId: number;
  skillId: number;
  skillName: string;
  categoryId: number;
  categoryName: string;
  selfLevel: number | null;
  selfNote: string | null;
  hrLevel: number | null;
  hrNote: string | null;
  updatedAt: string;
}

export interface SelfAssessmentWriteDto {
  selfLevel: number;
  selfNote?: string | null;
}

export interface HrAssessmentWriteDto {
  hrLevel: number;
  hrNote?: string | null;
}
