import { KeycloakConfig } from 'keycloak-angular';

let keycloakConfig: KeycloakConfig = {
  "realm": "vertx",
  "url": "http://localhost:8080/auth/",
  "clientId": "angular-frontend",
}

export const environment = {
  production: true,
  keycloakConfig
};
