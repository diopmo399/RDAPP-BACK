import { Injectable } from '@angular/core';
import { SubmitDataService } from '../../shared/stores/submit-data';

/**
 * Service spécialisé pour l'inscription des utilisateurs
 * Hérite de SubmitDataService et configure l'URL de l'API
 */
@Injectable({ providedIn: 'root' })
export class UserRegistrationService extends SubmitDataService {
  constructor() {
    // Configuration : URL de l'endpoint et méthode HTTP
    super({
      apiUrl: '/api/users/register', // À adapter selon votre backend
      method: 'POST',
    });
  }
}