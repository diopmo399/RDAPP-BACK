# Signal Store NgRx - Guide d'utilisation

## 📋 Table des matières

1. [Vue d'ensemble](#vue-densemble)
2. [Architecture](#architecture)
3. [Installation](#installation)
4. [Utilisation rapide](#utilisation-rapide)
5. [Guide détaillé](#guide-détaillé)
6. [API Reference](#api-reference)
7. [Exemples avancés](#exemples-avancés)
8. [Bonnes pratiques](#bonnes-pratiques)

---

## 🎯 Vue d'ensemble

Ce module fournit un **Signal Store NgRx générique** pour gérer l'envoi de données vers un backend REST. Il suit les principes d'architecture clean et utilise les dernières fonctionnalités d'Angular 17+ et NgRx Signals.

### ✨ Fonctionnalités

- ✅ Gestion automatique de l'état (loading, success, error)
- ✅ Type-safe avec TypeScript strict
- ✅ Architecture propre (séparation Store / Service)
- ✅ Pas de RxJS dans les composants
- ✅ Signals et Computed signals
- ✅ Gestion d'erreur centralisée et lisible
- ✅ 100% réutilisable et générique

---

## 🏗️ Architecture

```
src/app/
├── shared/stores/submit-data/     # Store générique réutilisable
│   ├── models.ts                  # Interfaces et types
│   ├── submit-data.service.ts     # Service HTTP abstrait
│   ├── submit-data.store.ts       # Factory du Signal Store
│   └── index.ts                   # Barrel export
│
└── features/example/              # Exemple d'utilisation
    ├── example.models.ts          # Types Request/Response
    ├── example.service.ts         # Service spécialisé
    ├── example.store.ts           # Store configuré
    ├── example.component.ts       # Composant
    ├── example.component.html     # Template
    └── example.component.css      # Styles
```

### Principe de séparation des responsabilités

| Composant | Responsabilité |
|-----------|---------------|
| **Service** | Appels HTTP uniquement |
| **Store** | Gestion d'état et logique métier |
| **Component** | Affichage et interactions utilisateur |

---

## 📦 Installation

Le package `@ngrx/signals` a déjà été installé dans votre projet :

```bash
npm install @ngrx/signals --save
```

---

## 🚀 Utilisation rapide

### 1️⃣ Créer vos types

```typescript
// user-registration.models.ts
export interface UserRegistrationRequest {
  nom: string;
  prenom: string;
  email: string;
}

export interface UserRegistrationResponse {
  id: string;
  message: string;
}
```

### 2️⃣ Créer votre service

```typescript
// user-registration.service.ts
import { Injectable } from '@angular/core';
import { SubmitDataService } from '@shared/stores/submit-data';

@Injectable({ providedIn: 'root' })
export class UserRegistrationService extends SubmitDataService {
  constructor() {
    super({
      apiUrl: '/api/users/register',
      method: 'POST'
    });
  }
}
```

### 3️⃣ Créer votre store

```typescript
// user-registration.store.ts
import { createSubmitStore } from '@shared/stores/submit-data';

export const UserRegistrationStore = createSubmitStore<
  UserRegistrationRequest,
  UserRegistrationResponse
>();
```

### 4️⃣ Utiliser dans un composant

```typescript
// registration.component.ts
import { Component, inject } from '@angular/core';
import { UserRegistrationStore } from './user-registration.store';

@Component({
  selector: 'app-registration',
  standalone: true,
  providers: [UserRegistrationStore],
  template: `
    <form (ngSubmit)="onSubmit()">
      <!-- Vos champs de formulaire -->

      <button [disabled]="store.isLoading()">
        @if (store.isLoading()) {
          Envoi en cours...
        } @else {
          S'inscrire
        }
      </button>

      @if (store.hasError()) {
        <div class="error">{{ store.errorMessage() }}</div>
      }

      @if (store.isSuccess()) {
        <div class="success">Inscription réussie !</div>
      }
    </form>
  `
})
export class RegistrationComponent {
  readonly store = inject(UserRegistrationStore);

  onSubmit() {
    const data = { nom: 'Doe', prenom: 'John', email: 'john@example.com' };
    this.store.submit(data);
  }
}
```

---

## 📚 Guide détaillé

### État du Store

Le store expose automatiquement cet état :

```typescript
interface SubmitState<T> {
  data: T | null;        // Réponse du backend
  loading: boolean;      // Requête en cours ?
  success: boolean;      // Dernière requête réussie ?
  error: string | null;  // Message d'erreur lisible
}
```

### Signals exposés

| Signal | Type | Description |
|--------|------|-------------|
| `isLoading()` | `boolean` | Requête en cours |
| `hasError()` | `boolean` | Une erreur est présente |
| `isSuccess()` | `boolean` | Dernière soumission réussie |
| `errorMessage()` | `string` | Message d'erreur (vide si pas d'erreur) |
| `responseData()` | `T \| null` | Données de la réponse |

### Méthodes exposées

| Méthode | Paramètres | Description |
|---------|-----------|-------------|
| `submit()` | `payload: TRequest` | Soumet les données au backend |
| `reset()` | - | Réinitialise complètement l'état |
| `clearError()` | - | Efface uniquement l'erreur |
| `clearSuccess()` | - | Efface le flag de succès |

### Cycle de vie d'une soumission

```
1. User clique sur "Soumettre"
   ↓
2. store.submit(data)
   ↓
3. loading = true, error = null, success = false
   ↓
4. Appel HTTP via le service
   ↓
5a. SUCCÈS                    5b. ERREUR
    - data = response             - error = message
    - success = true              - success = false
    - loading = false             - loading = false
```

---

## 📖 API Reference

### `createSubmitStore<TRequest, TResponse>()`

Factory qui crée un Signal Store typé.

**Paramètres génériques :**
- `TRequest` : Type des données envoyées
- `TResponse` : Type de la réponse reçue

**Retourne :** Un Signal Store configuré

**Exemple :**
```typescript
export const MyStore = createSubmitStore<MyRequest, MyResponse>();
```

---

### `SubmitDataService`

Classe abstraite pour créer des services HTTP spécialisés.

**Configuration :**
```typescript
interface SubmitStoreConfig {
  apiUrl: string;              // URL de l'endpoint
  method?: 'POST' | 'PUT' | 'PATCH';  // Méthode HTTP
}
```

**Méthode :**
```typescript
submit<TRequest, TResponse>(payload: TRequest): Observable<TResponse>
```

**Gestion d'erreur automatique :**
- Erreurs réseau
- Status HTTP (400, 401, 403, 404, 409, 422, 500, etc.)
- Messages lisibles en français

---

## 🔥 Exemples avancés

### Exemple 1 : Effect sur succès

```typescript
export class MyComponent {
  readonly store = inject(MyStore);
  private readonly router = inject(Router);

  constructor() {
    effect(() => {
      if (this.store.isSuccess()) {
        // Redirection après succès
        this.router.navigate(['/success']);
      }
    });
  }
}
```

### Exemple 2 : Méthode PUT au lieu de POST

```typescript
@Injectable({ providedIn: 'root' })
export class UpdateUserService extends SubmitDataService {
  constructor() {
    super({
      apiUrl: '/api/users/123',  // URL avec ID
      method: 'PUT'              // Utiliser PUT
    });
  }
}
```

### Exemple 3 : Affichage conditionnel

```typescript
@Component({
  template: `
    @if (store.isLoading()) {
      <app-spinner />
    } @else if (store.isSuccess()) {
      <app-success-message [data]="store.responseData()" />
    } @else if (store.hasError()) {
      <app-error-alert [message]="store.errorMessage()" />
    } @else {
      <app-form (submit)="store.submit($event)" />
    }
  `
})
```

### Exemple 4 : Notification toast

```typescript
export class MyComponent {
  readonly store = inject(MyStore);
  private readonly toast = inject(ToastService);

  constructor() {
    effect(() => {
      if (this.store.isSuccess()) {
        this.toast.success('Opération réussie !');
      }
      if (this.store.hasError()) {
        this.toast.error(this.store.errorMessage());
      }
    });
  }
}
```

---

## ✅ Bonnes pratiques

### ✅ À FAIRE

1. **Typer vos données**
   ```typescript
   // ✅ BON
   export const MyStore = createSubmitStore<MyRequest, MyResponse>();

   // ❌ MAUVAIS
   export const MyStore = createSubmitStore<any, any>();
   ```

2. **Un store par fonctionnalité**
   ```typescript
   // ✅ BON
   UserRegistrationStore
   UserLoginStore
   ProductCreationStore

   // ❌ MAUVAIS
   GenericUserStore (trop vague)
   ```

3. **Fournir le store au niveau du composant**
   ```typescript
   // ✅ BON
   @Component({
     providers: [UserRegistrationStore]
   })

   // ❌ MAUVAIS (sauf si vous voulez un singleton global)
   providedIn: 'root'
   ```

4. **Réinitialiser après succès**
   ```typescript
   // ✅ BON
   effect(() => {
     if (this.store.isSuccess()) {
       setTimeout(() => this.store.reset(), 3000);
     }
   });
   ```

### ❌ À ÉVITER

1. **Ne pas gérer RxJS dans le composant**
   ```typescript
   // ❌ MAUVAIS
   this.service.submit(data).subscribe(...)

   // ✅ BON
   this.store.submit(data)
   ```

2. **Ne pas modifier l'état directement**
   ```typescript
   // ❌ MAUVAIS
   this.store.loading.set(true)

   // ✅ BON
   this.store.submit(data)
   ```

3. **Ne pas oublier de typer**
   ```typescript
   // ❌ MAUVAIS
   const data: any = this.form.value;

   // ✅ BON
   const data: UserRegistrationRequest = this.form.value;
   ```

---

## 🧪 Testing

### Tester le service

```typescript
describe('UserRegistrationService', () => {
  let service: UserRegistrationService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [UserRegistrationService]
    });

    service = TestBed.inject(UserRegistrationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  it('should submit data', () => {
    const request: UserRegistrationRequest = { nom: 'Doe', prenom: 'John', email: 'john@test.com' };
    const response: UserRegistrationResponse = { id: '123', message: 'OK' };

    service.submit(request).subscribe(res => {
      expect(res).toEqual(response);
    });

    const req = httpMock.expectOne('/api/users/register');
    expect(req.request.method).toBe('POST');
    req.flush(response);
  });
});
```

### Tester le composant

```typescript
describe('RegistrationComponent', () => {
  it('should submit form', async () => {
    const fixture = TestBed.createComponent(RegistrationComponent);
    const component = fixture.componentInstance;

    const data = { nom: 'Doe', prenom: 'John', email: 'john@test.com' };
    component.store.submit(data);

    fixture.detectChanges();
    await fixture.whenStable();

    expect(component.store.isLoading()).toBe(false);
    expect(component.store.isSuccess()).toBe(true);
  });
});
```

---

## 📞 Support

Pour toute question ou problème :
1. Consultez cet exemple complet : `src/app/features/example/`
2. Vérifiez la console pour les logs (les effects affichent des logs)
3. Utilisez le debug info dans le template (voir `example.component.html`)

---

## 📝 License

Ce code est fourni comme template réutilisable pour vos projets Angular.

---

## 🎉 Conclusion

Ce Signal Store NgRx vous permet de :
- ✅ Écrire moins de code boilerplate
- ✅ Avoir une architecture propre et maintenable
- ✅ Gérer automatiquement loading/error/success
- ✅ Type-safe à 100%
- ✅ Réutiliser facilement pour d'autres fonctionnalités

**Bon développement ! 🚀**