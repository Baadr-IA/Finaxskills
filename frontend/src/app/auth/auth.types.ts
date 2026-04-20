export type PermissionScope = 'SELF' | 'TEAM' | 'ORG' | 'ALL';
export type PermissionAction =
  | 'READ'
  | 'CREATE'
  | 'UPDATE'
  | 'DELETE'
  | 'PUBLISH';

export interface PermissionRequirement {
  resource: string;
  action: PermissionAction;
  scope: PermissionScope;
}

export interface EffectivePermission {
  resource: string;
  actions: PermissionAction[];
  scope: PermissionScope;
}

export interface UserInfoResponse {
  username: string;
  profileKeys: string[];
  permissions: EffectivePermission[];
}
