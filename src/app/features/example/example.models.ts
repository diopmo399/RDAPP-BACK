/**
 * Modèles pour l'exemple d'inscription utilisateur
 */

/**
 * Données d'inscription envoyées au backend
 */
export interface UserRegistrationRequest {
  nom: string;
  prenom: string;
  email: string;
  telephone: string;
  password: string;
}

/**
 * Réponse du backend après inscription
 */
export interface UserRegistrationResponse {
  id: string;
  nom: string;
  prenom: string;
  email: string;
  telephone: string;
  dateCreation: string;
  message: string;
}