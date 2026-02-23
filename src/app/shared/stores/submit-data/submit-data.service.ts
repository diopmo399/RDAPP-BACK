import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { SubmitStoreConfig } from './models';

/**
 * Service générique pour les appels HTTP de soumission de données
 * Responsabilité unique : effectuer les requêtes HTTP sans gérer l'état
 *
 * @example
 * ```typescript
 * // Créer un service spécialisé
 * @Injectable({ providedIn: 'root' })
 * export class UserRegistrationService extends SubmitDataService {
 *   constructor() {
 *     super({ apiUrl: '/api/users/register' });
 *   }
 * }
 * ```
 */
@Injectable()
export abstract class SubmitDataService {
  private readonly http = inject(HttpClient);

  constructor(private config: SubmitStoreConfig) {}

  /**
   * Soumet des données au backend
   * @param payload - Données à envoyer
   * @returns Observable de la réponse
   */
  submit<TRequest, TResponse>(payload: TRequest): Observable<TResponse> {
    const method = this.config.method || 'POST';
    const url = this.config.apiUrl;

    let request$: Observable<TResponse>;

    switch (method) {
      case 'POST':
        request$ = this.http.post<TResponse>(url, payload);
        break;
      case 'PUT':
        request$ = this.http.put<TResponse>(url, payload);
        break;
      case 'PATCH':
        request$ = this.http.patch<TResponse>(url, payload);
        break;
      default:
        request$ = this.http.post<TResponse>(url, payload);
    }

    return request$.pipe(
      catchError((error: HttpErrorResponse) => {
        return throwError(() => this.formatError(error));
      })
    );
  }

  /**
   * Formate les erreurs HTTP en messages lisibles
   * @param error - Erreur HTTP
   * @returns Message d'erreur formaté
   */
  private formatError(error: HttpErrorResponse): Error {
    let errorMessage = 'Une erreur est survenue lors de la soumission';

    if (error.error instanceof ErrorEvent) {
      // Erreur côté client ou réseau
      errorMessage = `Erreur réseau: ${error.error.message}`;
    } else if (error.status === 0) {
      // Pas de connexion au serveur
      errorMessage = 'Impossible de se connecter au serveur. Vérifiez votre connexion internet.';
    } else {
      // Erreur côté serveur
      switch (error.status) {
        case 400:
          errorMessage = error.error?.message || 'Données invalides';
          break;
        case 401:
          errorMessage = 'Non autorisé. Veuillez vous reconnecter.';
          break;
        case 403:
          errorMessage = 'Accès refusé';
          break;
        case 404:
          errorMessage = 'Ressource non trouvée';
          break;
        case 409:
          errorMessage = error.error?.message || 'Conflit de données';
          break;
        case 422:
          errorMessage = error.error?.message || 'Validation échouée';
          break;
        case 500:
        case 502:
        case 503:
          errorMessage = 'Erreur serveur. Veuillez réessayer ultérieurement.';
          break;
        default:
          errorMessage = error.error?.message || `Erreur HTTP ${error.status}`;
      }
    }

    return new Error(errorMessage);
  }
}