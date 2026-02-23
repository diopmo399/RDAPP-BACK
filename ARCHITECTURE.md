# 🏗️ Architecture du Signal Store NgRx

## 📁 Structure des fichiers créés

```
RDAPP/
│
├── 📄 QUICK_START.md                    # Guide de démarrage rapide
├── 📄 SUBMIT_STORE_README.md            # Documentation complète
├── 📄 ARCHITECTURE.md                   # Ce fichier
│
└── src/app/
    │
    ├── 📂 shared/                        # Modules réutilisables
    │   ├── index.ts                      # Export général
    │   └── stores/
    │       └── submit-data/              # Store générique de soumission
    │           ├── index.ts              # Barrel export
    │           ├── models.ts             # Interfaces et types
    │           ├── submit-data.service.ts # Service HTTP abstrait
    │           └── submit-data.store.ts  # Factory du Signal Store
    │
    └── 📂 features/                      # Fonctionnalités métier
        └── example/                      # Exemple d'inscription utilisateur
            ├── example.models.ts         # Types Request/Response
            ├── example.service.ts        # Service spécialisé
            ├── example.store.ts          # Store configuré
            ├── example.component.ts      # Composant standalone
            ├── example.component.html    # Template
            └── example.component.css     # Styles
```

---

## 🔄 Flux de données

```
┌─────────────────────────────────────────────────────────────┐
│                        COMPOSANT                            │
│  - Affichage UI                                             │
│  - Gestion formulaire                                       │
│  - Appel store.submit(data)                                 │
│  - Lecture signals (isLoading, hasError, isSuccess)         │
└────────────────────┬────────────────────────────────────────┘
                     │
                     │ inject(Store)
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                      SIGNAL STORE                           │
│  - État (data, loading, success, error)                     │
│  - Computed signals                                         │
│  - Méthodes (submit, reset, clearError)                     │
│  - Effect RxJS pour appel HTTP                              │
└────────────────────┬────────────────────────────────────────┘
                     │
                     │ inject(Service)
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                         SERVICE                             │
│  - Appel HTTP (POST, PUT, PATCH)                            │
│  - Gestion d'erreur                                         │
│  - Formatage messages d'erreur                              │
└────────────────────┬────────────────────────────────────────┘
                     │
                     │ HttpClient
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                      BACKEND API                            │
│  - REST API JSON                                            │
└─────────────────────────────────────────────────────────────┘
```

---

## 🎯 Séparation des responsabilités

### 1️⃣ **Service** (`submit-data.service.ts`)

**Responsabilité unique :** Appels HTTP

```typescript
✅ Effectuer les requêtes HTTP
✅ Gérer les erreurs HTTP
✅ Formater les messages d'erreur
❌ JAMAIS gérer l'état
❌ JAMAIS stocker des données
```

### 2️⃣ **Store** (`submit-data.store.ts`)

**Responsabilité unique :** Gestion d'état

```typescript
✅ Stocker l'état (data, loading, error, success)
✅ Exposer des computed signals
✅ Orchestrer les appels via rxMethod
✅ Mettre à jour l'état selon les résultats
❌ JAMAIS faire d'appels HTTP directement
```

### 3️⃣ **Composant** (`example.component.ts`)

**Responsabilité unique :** UI et interactions

```typescript
✅ Afficher les données via signals
✅ Gérer le formulaire
✅ Appeler store.submit()
✅ Réagir aux changements d'état (effects)
❌ JAMAIS gérer RxJS
❌ JAMAIS faire d'appels HTTP
```

---

## 🔑 Concepts clés

### Signals

Les **signals** sont des valeurs réactives qui notifient automatiquement Angular quand elles changent.

```typescript
// Dans le store
store.isLoading()    // Signal<boolean>
store.hasError()     // Signal<boolean>
store.errorMessage() // Signal<string>

// Dans le template
@if (store.isLoading()) {
  <p>Chargement...</p>
}
```

### Computed Signals

Les **computed signals** dérivent automatiquement d'autres signals.

```typescript
withComputed((store) => ({
  isLoading: computed(() => store.loading()),
  hasError: computed(() => store.error() !== null)
}))
```

### rxMethod

**rxMethod** permet de créer des méthodes qui utilisent RxJS en interne mais exposent une API simple.

```typescript
submit: rxMethod<TRequest>(
  pipe(
    tap(() => patchState(store, { loading: true })),
    switchMap((payload) => service.submit(payload)),
    tap((response) => patchState(store, { data: response }))
  )
)
```

---

## 📊 État du Store

```typescript
interface SubmitState<TResponse> {
  data: TResponse | null;   // Réponse du backend
  loading: boolean;         // Requête en cours ?
  success: boolean;         // Dernière requête OK ?
  error: string | null;     // Message d'erreur
}
```

### Transitions d'état

```
INITIAL STATE
{ data: null, loading: false, success: false, error: null }
                    │
                    │ store.submit(data)
                    ▼
LOADING STATE
{ data: null, loading: true, success: false, error: null }
                    │
        ┌───────────┴───────────┐
        │                       │
        ▼                       ▼
SUCCESS STATE              ERROR STATE
{ data: response,          { data: null,
  loading: false,            loading: false,
  success: true,             success: false,
  error: null }              error: "message" }
```

---

## 🎨 Pattern générique

Le store utilise des **types génériques** pour être réutilisable :

```typescript
// Factory générique
createSubmitStore<TRequest, TResponse>()

// Instance typée
UserRegistrationStore = createSubmitStore<
  UserRegistrationRequest,    // Type des données envoyées
  UserRegistrationResponse     // Type de la réponse reçue
>()
```

Avantages :
- ✅ Type-safe à 100%
- ✅ Autocomplétion complète
- ✅ Détection d'erreurs à la compilation
- ✅ Réutilisable pour n'importe quelle API

---

## 🔧 Configuration du service

```typescript
interface SubmitStoreConfig {
  apiUrl: string;                       // URL de l'endpoint
  method?: 'POST' | 'PUT' | 'PATCH';   // Méthode HTTP
}

// Exemple
super({ apiUrl: '/api/users', method: 'POST' })
```

---

## 🧪 Exemple de bout en bout

### 1. Modèles

```typescript
// example.models.ts
export interface UserRequest {
  nom: string;
  email: string;
}

export interface UserResponse {
  id: string;
  message: string;
}
```

### 2. Service

```typescript
// example.service.ts
@Injectable({ providedIn: 'root' })
export class UserService extends SubmitDataService {
  constructor() {
    super({ apiUrl: '/api/users' });
  }
}
```

### 3. Store

```typescript
// example.store.ts
export const UserStore = createSubmitStore<UserRequest, UserResponse>();
```

### 4. Composant

```typescript
// example.component.ts
@Component({
  providers: [UserStore]
})
export class ExampleComponent {
  readonly store = inject(UserStore);

  onSubmit(data: UserRequest) {
    this.store.submit(data);
  }
}
```

### 5. Template

```html
<!-- example.component.html -->
@if (store.isLoading()) {
  <p>Chargement...</p>
}

@if (store.isSuccess()) {
  <p>{{ store.responseData()?.message }}</p>
}

@if (store.hasError()) {
  <p>{{ store.errorMessage() }}</p>
}
```

---

## ✅ Avantages de cette architecture

| Avantage | Description |
|----------|-------------|
| 🧩 **Séparation claire** | Chaque couche a une responsabilité unique |
| 🔒 **Type-safe** | TypeScript strict à tous les niveaux |
| ♻️ **Réutilisable** | Un seul store générique pour toutes les APIs |
| 🚀 **Performant** | Signals = mises à jour granulaires |
| 🧪 **Testable** | Chaque couche se teste indépendamment |
| 📚 **Maintenable** | Code clair, commenté, facile à comprendre |
| 🎯 **Moderne** | Utilise les dernières features Angular 17+ |

---

## 🎓 Concepts Angular utilisés

- ✅ Standalone Components (Angular 17+)
- ✅ Signal-based State Management
- ✅ Dependency Injection
- ✅ Reactive Forms
- ✅ HttpClient
- ✅ RxJS Operators
- ✅ NgRx Signals Store
- ✅ Effects
- ✅ Computed Signals

---

## 📖 Pour aller plus loin

1. **Ajouter un cache** : Stocker les réponses pour éviter les appels répétés
2. **Ajouter un retry** : Réessayer automatiquement en cas d'erreur réseau
3. **Ajouter un debounce** : Éviter les soumissions multiples rapides
4. **Ajouter des analytics** : Tracker les succès/erreurs
5. **Ajouter un loading global** : Combiner plusieurs stores

---

**Cette architecture est prête pour la production ! 🚀**