/**
 * État de soumission de données génériques
 * @template T - Type des données de réponse
 */
export interface SubmitState<T> {
  /** Données de la réponse du backend */
  data: T | null;
  /** Indique si une requête est en cours */
  loading: boolean;
  /** Indique si la dernière soumission a réussi */
  success: boolean;
  /** Message d'erreur si la soumission a échoué */
  error: string | null;
}

/**
 * Configuration pour le Signal Store de soumission
 */
export interface SubmitStoreConfig {
  /** URL de l'endpoint API */
  apiUrl: string;
  /** Méthode HTTP (par défaut: POST) */
  method?: 'POST' | 'PUT' | 'PATCH';
}

/**
 * État initial par défaut pour le store de soumission
 */
export const initialSubmitState = <T>(): SubmitState<T> => ({
  data: null,
  loading: false,
  success: false,
  error: null,
});