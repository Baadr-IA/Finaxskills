import type { KeycloakConfig } from 'keycloak-js';

export const KEYCLOAK_CONFIG: KeycloakConfig = {
  url: 'https://awskeycloak.lab-finaxys.net',
  realm: 'Template-app-Java-angular',
  clientId: 'template-app-angular-frontend',
};

export const KEYCLOAK_API_AUDIENCE = 'template-app-spring-api';
