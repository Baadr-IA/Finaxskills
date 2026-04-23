export interface SkillCategoryDto {
  id: number;
  name: string;
  description: string | null;
}

export interface SkillCategoryWriteDto {
  name: string;
  description?: string | null;
}

export interface SkillDto {
  id: number;
  name: string;
  description: string | null;
  categoryId: number;
  categoryName: string;
}

export interface SkillWriteDto {
  name: string;
  description?: string | null;
  categoryId: number;
}
