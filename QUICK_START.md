# 🚀 Démarrage Rapide - Signal Store NgRx

## En 3 étapes simples

### 1️⃣ Créer vos types (1 fichier)

```typescript
// ma-fonctionnalite.models.ts
export interface MaRequete {
  champ1: string;
  champ2: number;
}

export interface MaReponse {
  id: string;
  message: string;
}
```

### 2️⃣ Créer votre service (1 fichier)

```typescript
// ma-fonctionnalite.service.ts
import { Injectable } from '@angular/core';
import { SubmitDataService } from '@shared';

@Injectable({ providedIn: 'root' })
export class MaFonctionnaliteService extends SubmitDataService {
  constructor() {
    super({ apiUrl: '/api/mon-endpoint' });
  }
}
```

### 3️⃣ Créer votre store (1 fichier)

```typescript
// ma-fonctionnalite.store.ts
import { createSubmitStore } from '@shared';
import { MaRequete, MaReponse } from './ma-fonctionnalite.models';

export const MaFonctionnaliteStore = createSubmitStore<MaRequete, MaReponse>();
```

### 4️⃣ Utiliser dans votre composant

```typescript
// mon-composant.component.ts
import { Component, inject } from '@angular/core';
import { MaFonctionnaliteStore } from './ma-fonctionnalite.store';

@Component({
  selector: 'app-mon-composant',
  standalone: true,
  providers: [MaFonctionnaliteStore],
  template: `
    <button
      (click)="envoyer()"
      [disabled]="store.isLoading()">
      Envoyer
    </button>

    @if (store.isLoading()) {
      <p>Chargement...</p>
    }

    @if (store.isSuccess()) {
      <p>✅ Succès : {{ store.responseData()?.message }}</p>
    }

    @if (store.hasError()) {
      <p>❌ Erreur : {{ store.errorMessage() }}</p>
    }
  `
})
export class MonComposant {
  readonly store = inject(MaFonctionnaliteStore);

  envoyer() {
    this.store.submit({
      champ1: 'valeur1',
      champ2: 123
    });
  }
}
```

---

## 🎯 C'est tout !

Votre store gère automatiquement :
- ✅ Le loading
- ✅ Les erreurs
- ✅ Le succès
- ✅ Le stockage de la réponse

---

## 📚 Documentation complète

Pour plus de détails, consultez `SUBMIT_STORE_README.md`

## 🧪 Exemple complet

Un exemple complet est disponible dans `src/app/features/example/`

---

**Happy coding! 🎉**