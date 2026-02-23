import { computed, inject } from '@angular/core';
import {
  patchState,
  signalStore,
  withComputed,
  withMethods,
  withState,
} from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe, tap, switchMap, catchError, of } from 'rxjs';
import { SubmitDataService } from './submit-data.service';
import { initialSubmitState, SubmitState } from './models';

/**
 * Factory pour créer un Signal Store de soumission de données générique
 *
 * Ce store gère tout le cycle de vie d'une soumission de données :
 * - État de chargement
 * - Succès/Erreur
 * - Stockage de la réponse
 *
 * @template TRequest - Type des données envoyées au backend
 * @template TResponse - Type de la réponse du backend
 *
 * @example
 * ```typescript
 * // 1. Créer le service
 * @Injectable({ providedIn: 'root' })
 * export class UserRegistrationService extends SubmitDataService {
 *   constructor() {
 *     super({ apiUrl: '/api/users/register' });
 *   }
 * }
 *
 * // 2. Créer le store
 * export const UserRegistrationStore = createSubmitStore<UserData, UserResponse>();
 *
 * // 3. Utiliser dans un composant
 * export class RegisterComponent {
 *   readonly store = inject(UserRegistrationStore);
 *
 *   onSubmit(data: UserData) {
 *     this.store.submit(data);
 *   }
 * }
 * ```
 */
export function createSubmitStore<TRequest = unknown, TResponse = unknown>() {
  return signalStore(
    // ==========================================
    // STATE
    // ==========================================
    /**
     * État initial du store
     * Contient : data, loading, success, error
     */
    withState<SubmitState<TResponse>>(initialSubmitState<TResponse>()),

    // ==========================================
    // COMPUTED SIGNALS
    // ==========================================
    /**
     * Computed signals pour des accès simplifiés à l'état
     */
    withComputed((store) => ({
      /**
       * Indique si une requête est en cours
       */
      isLoading: computed(() => store.loading()),

      /**
       * Indique si une erreur est survenue
       */
      hasError: computed(() => store.error() !== null),

      /**
       * Indique si la dernière soumission a réussi
       */
      isSuccess: computed(() => store.success()),

      /**
       * Message d'erreur lisible (vide si pas d'erreur)
       */
      errorMessage: computed(() => store.error() || ''),

      /**
       * Données de réponse (null si pas encore de réponse)
       */
      responseData: computed(() => store.data()),
    })),

    // ==========================================
    // METHODS
    // ==========================================
    /**
     * Méthodes publiques exposées par le store
     */
    withMethods((store) => {
      const service = inject(SubmitDataService);

      return {
        /**
         * Soumet des données au backend
         * Déclenche automatiquement :
         * - loading = true
         * - Appel HTTP via le service
         * - Mise à jour de l'état selon le résultat
         *
         * @param payload - Données à envoyer
         */
        submit: rxMethod<TRequest>(
          pipe(
            // Avant l'appel : réinitialiser l'état et activer le loading
            tap(() => {
              patchState(store, {
                loading: true,
                error: null,
                success: false,
              });
            }),

            // Effectuer l'appel HTTP
            switchMap((payload) =>
              service.submit<TRequest, TResponse>(payload).pipe(
                // Succès : stocker la réponse
                tap((response) => {
                  patchState(store, {
                    data: response,
                    success: true,
                    loading: false,
                    error: null,
                  });
                }),

                // Erreur : stocker le message d'erreur
                catchError((error: Error) => {
                  patchState(store, {
                    error: error.message,
                    success: false,
                    loading: false,
                  });
                  return of(null); // Continue le flux sans propager l'erreur
                })
              )
            )
          )
        ),

        /**
         * Réinitialise complètement l'état du store
         * Utile après une soumission réussie ou pour annuler un formulaire
         */
        reset(): void {
          patchState(store, initialSubmitState<TResponse>());
        },

        /**
         * Efface uniquement l'erreur
         * Utile pour masquer un message d'erreur après que l'utilisateur ait cliqué sur "OK"
         */
        clearError(): void {
          patchState(store, { error: null });
        },

        /**
         * Efface uniquement le flag de succès
         * Utile pour masquer une notification de succès
         */
        clearSuccess(): void {
          patchState(store, { success: false });
        },
      };
    })
  );
}

/**
 * Type helper pour extraire le type du store créé
 * Utile pour le typage dans les tests ou les composants
 */
export type SubmitStoreType<TRequest, TResponse> = ReturnType<
  typeof createSubmitStore<TRequest, TResponse>
>;