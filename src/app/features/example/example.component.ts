import { Component, inject, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { UserRegistrationStore } from './example.store';
import { UserRegistrationService } from './example.service';

/**
 * Composant d'exemple montrant comment utiliser le Signal Store de soumission
 *
 * Architecture :
 * - Composant standalone (Angular 17+)
 * - Reactive Forms pour le formulaire
 * - Signal Store pour la gestion d'état
 * - Service HTTP séparé
 * - Pas de RxJS dans le composant (tout géré par le store)
 *
 * Points clés :
 * - Utilisation de signals() pour afficher les données
 * - submit() déclenche automatiquement l'appel HTTP
 * - Le store gère loading, error, success automatiquement
 */
@Component({
  selector: 'app-example',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  providers: [
    // Le store est fourni au niveau du composant (création d'une instance)
    UserRegistrationStore,
    // Le service est déjà providedIn: 'root', pas besoin de le fournir ici
  ],
  templateUrl: './example.component.html',
  styleUrl: './example.component.css',
})
export class ExampleComponent {
  // ==========================================
  // INJECTIONS
  // ==========================================
  private readonly fb = inject(FormBuilder);

  /**
   * Injection du Signal Store
   * Le store expose automatiquement tous ses signals et méthodes
   */
  readonly store = inject(UserRegistrationStore);

  // ==========================================
  // FORMULAIRE
  // ==========================================
  readonly registrationForm: FormGroup = this.fb.group({
    nom: ['', [Validators.required, Validators.minLength(2)]],
    prenom: ['', [Validators.required, Validators.minLength(2)]],
    email: ['', [Validators.required, Validators.email]],
    telephone: ['', [Validators.required, Validators.pattern(/^[0-9]{10}$/)]],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  // ==========================================
  // EFFECTS
  // ==========================================
  /**
   * Effect qui s'exécute quand la soumission réussit
   * Réinitialise le formulaire et le store après 3 secondes
   */
  constructor() {
    effect(() => {
      // Utilisation du signal isSuccess()
      if (this.store.isSuccess()) {
        console.log('✅ Inscription réussie!');
        console.log('Données reçues:', this.store.responseData());

        // Réinitialiser le formulaire et le store après 3 secondes
        setTimeout(() => {
          this.registrationForm.reset();
          this.store.reset();
        }, 3000);
      }
    });

    effect(() => {
      // Utilisation du signal hasError()
      if (this.store.hasError()) {
        console.error('❌ Erreur:', this.store.errorMessage());
      }
    });
  }

  // ==========================================
  // MÉTHODES PUBLIQUES
  // ==========================================

  /**
   * Soumet le formulaire
   * Délègue tout le travail au store via submit()
   */
  onSubmit(): void {
    if (this.registrationForm.invalid) {
      this.registrationForm.markAllAsTouched();
      return;
    }

    // Récupérer les données du formulaire
    const formData = this.registrationForm.value;

    // Déclencher la soumission via le store
    // Le store gère automatiquement :
    // - loading = true
    // - Appel HTTP
    // - success ou error
    // - loading = false
    this.store.submit(formData);
  }

  /**
   * Efface l'erreur affichée
   */
  dismissError(): void {
    this.store.clearError();
  }

  /**
   * Efface le message de succès
   */
  dismissSuccess(): void {
    this.store.clearSuccess();
  }

  /**
   * Réinitialise le formulaire et le store
   */
  resetForm(): void {
    this.registrationForm.reset();
    this.store.reset();
  }

  // ==========================================
  // HELPERS POUR LE TEMPLATE
  // ==========================================

  /**
   * Vérifie si un champ est invalide et touché
   */
  isFieldInvalid(fieldName: string): boolean {
    const field = this.registrationForm.get(fieldName);
    return !!(field?.invalid && field?.touched);
  }

  /**
   * Récupère le message d'erreur d'un champ
   */
  getFieldError(fieldName: string): string {
    const field = this.registrationForm.get(fieldName);
    if (!field || !field.errors) return '';

    if (field.errors['required']) return 'Ce champ est requis';
    if (field.errors['email']) return 'Email invalide';
    if (field.errors['minlength']) {
      const required = field.errors['minlength'].requiredLength;
      return `Minimum ${required} caractères`;
    }
    if (field.errors['pattern']) return 'Format invalide (10 chiffres)';

    return '';
  }
}