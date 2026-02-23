import { createSubmitStore } from '../../shared/stores/submit-data';
import {
  UserRegistrationRequest,
  UserRegistrationResponse,
} from './example.models';

/**
 * Store NgRx Signal pour gérer l'inscription des utilisateurs
 *
 * Ce store est une instance typée du store générique de soumission
 * Il gère automatiquement :
 * - L'état de chargement
 * - Les erreurs
 * - Le succès
 * - Les données de réponse
 */
export const UserRegistrationStore = createSubmitStore<
  UserRegistrationRequest,
  UserRegistrationResponse
>();