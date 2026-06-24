export interface CollaboratorDto {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  jobTitle: string | null;
  keycloakId: string | null;
}

export interface CollaboratorWriteDto {
  firstName: string;
  lastName: string;
  email: string;
  jobTitle?: string | null;
  keycloakId?: string | null; // still allowed for updates via API, but create UI won't send it
  skillId?: number | null;
  selfLevel?: number | null;
}
