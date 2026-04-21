---
name: finaxys-ui-angular
description: "Use when creating or updating Angular 21 + Tailwind v4 UI in finaskills-ng with Finaxys design tokens, layouts, forms, tables, dialogs, and role-based pages."
---

# Finaxys UI Skill — Guide Complet Angular 21 + Tailwind v4

> [!IMPORTANT]
> **RÉFÉRENCES ET ASSETS LOCAUX**
> Les fichiers sources officiels sont disponibles dans le sous-dossier `references/` :
> - **Logos :** `references/finaxys-black.svg` (fond clair), `references/finaxys-logo-light.svg` (fond sombre)
> - **Documentation :** `references/GUIDE-UTILISATION.md`, `references/GUIDE-PARTAGE.md`, `references/README.md`
> - **Métadonnées :** `references/PACKAGE-INFO.md`
>
> **Règle impérative :** Ne jamais recréer les logos. Toujours se référer aux fichiers SVG locaux ou au `GUIDE-UTILISATION.md` pour les spécifications visuelles exactes.

> Référence canonique pour toute nouvelle page ou composant du projet **finaskills-ng**.
> Extrait directement des sources React + design tokens officiels Finaxys.
> Toujours lire ce fichier avant d'écrire un composant Angular.

---

## 1. TOKENS DE DESIGN OFFICIELS

### Couleurs
```
Brand red (PRIMARY) : #E63946   ← TOUJOURS utiliser cette valeur, pas #FF4D00
Surface dark (navbar/sidebar) : #0A0A0A
Surface background (page bg) : #F9F9F9
Surface card (cards/panels) : #FFFFFF

Texte principal   : #1F1F1F
Texte secondaire  : #6B7280
Bordure par défaut: #E5E7EB

Success  : #059669
Warning  : #F59E0B
Error    : #DC2626
Info     : #3B82F6
```

### Typographie
```
Police   : Inter, system-ui, -apple-system, sans-serif
H1       : 32px / bold (700) / letter-spacing -0.02em
H2       : 24px / semibold (600)
H3       : 20px / semibold (600)
H4       : 16px / medium (500)
Body     : 14px / normal (400) / line-height 1.6
Label    : 11px / semibold (600) / letter-spacing 0.08em / uppercase
```

### Radius
```
sm: 6px   → rounded-md (approx)
md: 8px   → rounded-lg (défaut cards, boutons)
lg: 12px  → rounded-xl
xl: 16px  → rounded-2xl
```

### Ombres
```
sm : 0 1px 2px rgba(0,0,0,0.05)   → shadow-sm
md : 0 1px 3px rgba(0,0,0,0.08)   → shadow-sm (défaut cards)
lg : 0 4px 6px rgba(0,0,0,0.1)    → shadow-md
xl : 0 10px 15px rgba(0,0,0,0.1)  → shadow-lg
```

### Espacement
```
xs:4px  sm:8px  md:16px  lg:24px  xl:32px  xxl:48px
```

---

## 2. CONFIGURATION ANGULAR (OBLIGATOIRE)

### Boilerplate de tout composant
```typescript
import { Component, inject, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AppService } from '../../../core/services/app.service';

@Component({
  selector: 'app-ma-page',
  standalone: true,           // TOUJOURS standalone: true
  imports: [FormsModule],     // FormsModule si ngModel utilisé
  template: `...`,
})
export class MaPageComponent {
  private svc = inject(AppService);
  private router = inject(Router);
}
```

### Règles Angular 21
- **PAS de CommonModule** (Angular 17+ = built-in control flow)
- **PAS de `*ngIf` / `*ngFor`** → utiliser `@if` / `@for`
- **PAS de ngx-charts** → CSS bars uniquement
- **PAS de lucide-angular** → inline SVG uniquement
- **PAS de Angular Material** → pure Tailwind + HTML
- **Signal pour tout état** : `signal()`, `computed()`, `effect()`
- `@let` est valide Angular 17+ mais éviter si possible (utiliser computed())

### AppService — signaux disponibles
```typescript
// Lecture
currentUser = signal<User | null>(null)
users        = signal<User[]>([...])
skills       = signal<Skill[]>([...])
categories   = signal<SkillCategory[]>([...])
jobPositions = signal<JobPosition[]>([...])
evaluations  = signal<Evaluation[]>([...])
testAssignments = signal<TestAssignment[]>([...])
testResults     = signal<TestResult[]>([...])

// Mutations (IDs: User.id = number, Skill.id = string)
addUser(user: User): void
deleteUser(userId: number): void
updateUser(userId: number, updates: Partial<User>): void
addSkill(skill: Skill): void
addCategory(category: SkillCategory): void
addJobPosition(job: JobPosition): void
addTestAssignment(assignment: TestAssignment): void
updateTestAssignment(id: string, updates: Partial<TestAssignment>): void
addTestResult(result: TestResult): void
updateEvaluation(id: string, updates: Partial<Evaluation>): void
addEvaluation(evaluation: Evaluation): void
login(email: string): void
logout(): void
```

---

## 3. LAYOUT GLOBAL

### Structure de page (MainLayout)
```
min-h-screen bg-[#F9F9F9]
├── <nav> bg-white border-b border-[#E5E7EB] h-16
│   └── max-w-7xl mx-auto px-4 sm:px-6 lg:px-8
│       ├── Logo (64px) + nav links (espace-x-8)
│       └── Avatar + dropdown (droite)
└── <main> max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8
    └── <router-outlet>
```

### Navbar — nav links par rôle
```
collab     → /dashboard, /tests, /skills-catalog
rh         → /rh/dashboard, /rh/users, /rh/evaluations, /skills-catalog
admin_data → /admin/data, /skills-catalog
expert_test→ /expert/tests, /skills-catalog
```

### Navbar — lien actif
```html
<!-- actif -->
<a style="color:#E63946; background:rgba(230,57,70,0.08)" 
   class="inline-flex items-center px-4 py-2 text-sm font-medium rounded-md">

<!-- inactif -->
<a style="color:#6B7280; background:transparent"
   class="inline-flex items-center px-4 py-2 text-sm font-medium rounded-md">
```

### Logo Finaxys (toujours en haut à gauche)
```html
<!-- Navbar (variant light = fond blanc) -->
<div class="font-black text-[#0A0A0A] text-xl tracking-wider">
  FINA<span style="color:#E63946">XYS</span>
</div>

<!-- Login (variant dark = fond noir) -->
<div class="font-black text-white text-4xl tracking-wider">
  FINA<span style="color:#E63946">XYS</span>
</div>
```

---

## 4. PAGE LOGIN

### Structure exacte (2 panneaux)
```html
<div class="min-h-screen flex">
  <!-- Panneau gauche : fond noir #0A0A0A, hidden sur mobile -->
  <div class="hidden lg:flex lg:w-1/2 flex-col justify-between p-12" style="background:#0A0A0A">
    <!-- Logo blanc + tagline gris + liste features avec points #E63946 -->
    <!-- Footer : © 2026 Finaxys. Tous droits réservés. text-gray-400 text-xs -->
  </div>

  <!-- Panneau droit : fond blanc -->
  <div class="w-full lg:w-1/2 flex items-center justify-center p-8 bg-white">
    <div class="w-full max-w-md space-y-8">
      <!-- h2 "Connexion" text-3xl font-semibold -->
      <!-- Form email + password (icônes SVG inline, h-12 inputs, pl-10) -->
      <!-- Bouton "Se connecter" bg-[#E63946] w-full h-12 text-white font-semibold -->
      <!-- Séparateur "Accès rapide (démo)" -->
      <!-- Grid 2x2 de boutons outline (Collaborateur, RH, Admin, Expert) -->
    </div>
  </div>
</div>
```

### Champ de formulaire (pattern standard)
```html
<div class="space-y-2">
  <label class="text-sm font-medium text-gray-700">Label</label>
  <div class="relative">
    <!-- SVG icon absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400 -->
    <input type="email" placeholder="..."
      class="w-full h-12 pl-10 pr-4 border border-gray-300 rounded-lg text-sm
             focus:outline-none focus:ring-2 focus:ring-[#E63946] focus:border-transparent" />
  </div>
</div>
```

### Alert d'erreur
```html
@if (error()) {
  <div class="flex items-center gap-2 p-3 bg-red-50 border border-red-200 rounded-lg text-red-700 text-sm">
    <!-- SVG warning icon -->
    {{ error() }}
  </div>
}
```

---

## 5. COMPOSANTS PARTAGÉS

### Bouton principal (brand red)
```html
<button class="inline-flex items-center gap-2 bg-[#E63946] hover:bg-[#d32f3c] 
               text-white font-medium px-4 py-2 rounded-lg transition-colors">
  <!-- SVG icon optionnel -->
  Label
</button>
```

### Bouton secondaire (outline)
```html
<button class="inline-flex items-center gap-2 border border-gray-300 hover:border-gray-900 
               bg-white text-gray-700 font-medium px-4 py-2 rounded-lg transition-colors">
  Label
</button>
```

### Bouton ghost (icon seul, actions de table)
```html
<button class="p-1.5 hover:bg-gray-100 rounded-md transition-colors text-gray-500 hover:text-gray-900">
  <!-- SVG h-4 w-4 -->
</button>
<!-- Pour supprimer, ajouter text-red-500 hover:text-red-700 -->
```

### Card standard
```html
<div class="bg-white rounded-xl border border-gray-200 shadow-sm">
  <div class="px-6 pt-6 pb-2">
    <h3 class="text-lg font-semibold text-gray-900">Titre</h3>
    <p class="text-sm text-gray-600 mt-1">Description</p>
  </div>
  <div class="px-6 pb-6">
    <!-- contenu -->
  </div>
</div>
```

### Stat card COMPACT (py-2 px-3) — CollabDashboard style
```html
<div class="bg-white rounded-xl border border-gray-200 shadow-sm py-2 px-3">
  <div class="flex flex-row items-center justify-between mb-1">
    <span class="text-sm font-medium leading-none text-gray-600">Titre</span>
    <!-- SVG h-3.5 w-3.5 text-gray-600 -->
  </div>
  <div class="text-lg font-semibold leading-none">42</div>
  <p class="text-[10px] text-gray-600 leading-none mt-0.5">sous-titre</p>
</div>
```

### Stat card STANDARD (px-6 pt-6) — RHDashboard style
```html
<div class="bg-white rounded-xl border border-gray-200 shadow-sm">
  <div class="px-6 pt-6 pb-0">
    <div class="flex flex-row items-center justify-between pb-2">
      <span class="text-sm font-medium text-gray-600">Titre</span>
      <!-- SVG h-4 w-4 text-gray-600 -->
    </div>
  </div>
  <div class="px-6 pb-6">
    <div class="text-2xl font-semibold text-gray-900">42</div>
    <p class="text-xs text-gray-600 mt-1">sous-titre</p>
  </div>
</div>
```

### Grid de stat cards
```html
<!-- 4 cards = grid-cols-1 md:grid-cols-4 gap-4 -->
<!-- 3 cards = grid-cols-1 md:grid-cols-3 gap-4 -->
<!-- 2 cards = grid-cols-1 md:grid-cols-2 gap-4 -->
<div class="grid grid-cols-1 md:grid-cols-4 gap-4">
  <!-- stat cards ici -->
</div>
```

### Badge statut
```html
<!-- Vert : assigné/complété/validé -->
<span class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">Complété</span>

<!-- Orange : en cours/warning -->
<span class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-orange-100 text-orange-800">En cours</span>

<!-- Bleu : assigné -->
<span class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">Assigné</span>

<!-- Rouge : annulé/erreur -->
<span class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-red-100 text-red-800">Annulé</span>

<!-- Gris : outline tag -->
<span class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium border border-gray-300 text-gray-600">Tag</span>
```

### Badge niveau (skills)
```
Débutant    → bg-gray-100 text-gray-600
Initié      → bg-blue-100 text-blue-700
Intermédiaire → bg-yellow-100 text-yellow-700
Confirmé    → bg-orange-100 text-orange-700
Expert      → bg-green-100 text-green-700
```
```typescript
// Dans le composant
readonly levelNames = ['Débutant', 'Initié', 'Intermédiaire', 'Confirmé', 'Expert'];
readonly levelColors = ['bg-gray-100 text-gray-600','bg-blue-100 text-blue-700','bg-yellow-100 text-yellow-700','bg-orange-100 text-orange-700','bg-green-100 text-green-700'];
```

### Map des statuts (FR)
```typescript
readonly statusLabels: Record<string, string> = {
  assigned: 'Assigné',
  in_progress: 'En cours',
  completed: 'Complété',
  cancelled: 'Annulé',
};
readonly statusColors: Record<string, string> = {
  assigned: 'bg-blue-100 text-blue-800',
  in_progress: 'bg-orange-100 text-orange-800',
  completed: 'bg-green-100 text-green-800',
  cancelled: 'bg-red-100 text-red-800',
};
```

---

## 6. TABLEAU (Table)

```html
<div class="overflow-x-auto">
  <table class="w-full text-sm">
    <thead>
      <tr class="border-b border-gray-200">
        <th class="text-left py-3 px-4 font-medium text-gray-600">Colonne</th>
        <!-- ... -->
        <th class="text-right py-3 px-4 font-medium text-gray-600">Actions</th>
      </tr>
    </thead>
    <tbody>
      @for (item of items(); track item.id) {
        <tr class="border-b border-gray-100 hover:bg-gray-50 transition-colors">
          <td class="py-3 px-4 font-medium text-gray-900">{{ item.name }}</td>
          <!-- ... -->
          <td class="py-3 px-4 text-right">
            <div class="flex justify-end gap-2">
              <!-- boutons ghost -->
            </div>
          </td>
        </tr>
      }
      @if (items().length === 0) {
        <tr>
          <td colspan="6" class="py-12 text-center text-gray-500">Aucun élément</td>
        </tr>
      }
    </tbody>
  </table>
</div>
```

---

## 7. TABS (Onglets)

```typescript
// Dans le composant
activeTab = signal<'all' | 'pending' | 'done'>('all');
```

```html
<!-- Tab bar -->
<div class="flex gap-1 bg-gray-100 p-1 rounded-lg">
  <button (click)="activeTab.set('all')"
    [class]="activeTab() === 'all' 
      ? 'flex-1 py-2 px-4 text-sm font-medium rounded-md bg-white shadow-sm text-gray-900 transition-all'
      : 'flex-1 py-2 px-4 text-sm font-medium rounded-md text-gray-600 hover:text-gray-900 transition-all'">
    Tous
  </button>
  <button (click)="activeTab.set('pending')"
    [class]="activeTab() === 'pending'
      ? 'flex-1 py-2 px-4 text-sm font-medium rounded-md bg-white shadow-sm text-gray-900 transition-all'
      : 'flex-1 py-2 px-4 text-sm font-medium rounded-md text-gray-600 hover:text-gray-900 transition-all'">
    En attente
  </button>
</div>

<!-- Contenu conditionnel -->
@if (activeTab() === 'all') {
  <!-- contenu Tous -->
}
@if (activeTab() === 'pending') {
  <!-- contenu En attente -->
}
```

---

## 8. DIALOG / MODAL

```typescript
// Signal
showDialog = signal(false);

// Méthodes
openDialog(): void  { this.showDialog.set(true); }
closeDialog(): void { this.showDialog.set(false); }
```

```html
<!-- Déclencheur -->
<button (click)="openDialog()" class="...">Ouvrir</button>

<!-- Modal overlay -->
@if (showDialog()) {
  <div class="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4"
       (click)="closeDialog()">
    <div class="bg-white rounded-xl shadow-xl w-full max-w-md"
         (click)="$event.stopPropagation()">
      <!-- Header -->
      <div class="px-6 pt-6 pb-4 border-b border-gray-200">
        <h3 class="text-lg font-semibold text-gray-900">Titre dialog</h3>
        <p class="text-sm text-gray-600 mt-1">Description</p>
      </div>
      <!-- Body -->
      <div class="p-6 space-y-4">
        <!-- Formulaire ici -->
      </div>
      <!-- Footer -->
      <div class="px-6 pb-6 flex gap-3 justify-end">
        <button (click)="closeDialog()"
          class="px-4 py-2 border border-gray-300 rounded-lg text-sm font-medium hover:bg-gray-50 transition-colors">
          Annuler
        </button>
        <button (click)="submit()"
          class="px-4 py-2 bg-[#E63946] hover:bg-[#d32f3c] text-white rounded-lg text-sm font-medium transition-colors">
          Valider
        </button>
      </div>
    </div>
  </div>
}
```

---

## 9. INPUTS (Formulaires)

### Input texte standard
```html
<div class="space-y-1">
  <label class="text-sm font-medium text-gray-700">Label</label>
  <input [(ngModel)]="value" type="text" placeholder="..."
    class="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm
           focus:outline-none focus:ring-2 focus:ring-[#E63946] focus:border-transparent" />
</div>
```

### Textarea
```html
<div class="space-y-1">
  <label class="text-sm font-medium text-gray-700">Label</label>
  <textarea [(ngModel)]="value" rows="4" placeholder="..."
    class="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm resize-none
           focus:outline-none focus:ring-2 focus:ring-[#E63946] focus:border-transparent">
  </textarea>
</div>
```

### Select natif
```html
<div class="space-y-1">
  <label class="text-sm font-medium text-gray-700">Label</label>
  <select [(ngModel)]="value"
    class="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm bg-white
        focus:outline-none focus:ring-2 focus:ring-[#E63946] focus:border-transparent">
    <option value="">Sélectionner...</option>
    @for (item of items(); track item.id) {
      <option [value]="item.id">{{ item.name }}</option>
    }
  </select>
</div>
```

### Checkbox
```html
<label class="flex items-center gap-2 cursor-pointer">
  <input type="checkbox" [(ngModel)]="checked"
    class="w-4 h-4 rounded border-gray-300 accent-[#E63946]" />
  <span class="text-sm text-gray-700">Label</span>
</label>
```

### Radio
```html
<div class="space-y-2">
  @for (option of options; track option.value) {
    <label class="flex items-center gap-3 cursor-pointer p-3 rounded-lg border border-gray-200
                  hover:border-[#E63946] transition-colors"
           [class.border-[#E63946]]="selected() === option.value"
           [class.bg-orange-50]="selected() === option.value">
      <input type="radio" [value]="option.value" [(ngModel)]="selectedValue"
        class="accent-[#E63946]" />
      <span class="text-sm font-medium">{{ option.label }}</span>
    </label>
  }
</div>
```

### Toggle / Switch
```html
<button (click)="value.set(!value())" type="button"
  class="relative inline-flex h-6 w-11 rounded-full transition-colors duration-200 focus:outline-none"
  [style.background]="value() ? '#E63946' : '#D1D5DB'">
  <span class="inline-block h-4 w-4 rounded-full bg-white shadow transition-transform duration-200 mt-1"
    [style.transform]="value() ? 'translateX(1.375rem)' : 'translateX(0.25rem)'">
  </span>
</button>
```

### Barre de recherche
```html
<div class="relative">
  <svg class="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400"
       fill="none" viewBox="0 0 24 24" stroke="currentColor">
    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
      d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
  </svg>
  <input [(ngModel)]="searchTerm" type="text" placeholder="Rechercher..."
    class="w-full pl-9 pr-4 py-2 border border-gray-300 rounded-lg text-sm
           focus:outline-none focus:ring-2 focus:ring-[#E63946] focus:border-transparent" />
</div>
```

---

## 10. GRAPHIQUES (CSS bars — PAS de librairie)

### Barre horizontale simple
```html
<div class="space-y-2">
  <div class="flex justify-between items-center text-sm mb-1">
    <span class="text-gray-600">Label</span>
    <span class="font-medium">{{ value }}%</span>
  </div>
  <div class="h-2 bg-gray-100 rounded-full overflow-hidden">
    <div class="h-full rounded-full transition-all duration-500"
         style="background:#E63946"
         [style.width.%]="value">
    </div>
  </div>
</div>
```

### Graphique en barres verticales (grouped, 2 séries)
```html
<div class="space-y-3">
  @for (item of chartData(); track item.name) {
    <div class="space-y-1">
      <div class="flex justify-between text-xs text-gray-600">
        <span>{{ item.name }}</span>
        <span>{{ item.declared }} / {{ item.validated }}</span>
      </div>
      <!-- Barre déclarée (bleue) -->
      <div class="h-3 bg-gray-100 rounded-full overflow-hidden">
        <div class="h-full bg-blue-500 rounded-full transition-all"
             [style.width.%]="item.declared * 25"></div>
      </div>
      <!-- Barre validée (verte) -->
      <div class="h-3 bg-gray-100 rounded-full overflow-hidden">
        <div class="h-full bg-emerald-500 rounded-full transition-all"
             [style.width.%]="(item.validated ?? 0) * 25"></div>
      </div>
    </div>
  }
</div>
```

### Légende de graphique
```html
<div class="flex gap-4 mt-2 text-xs text-gray-600">
  <span class="flex items-center gap-1">
    <span class="inline-block w-3 h-3 rounded-sm bg-blue-500"></span> Déclaré
  </span>
  <span class="flex items-center gap-1">
    <span class="inline-block w-3 h-3 rounded-sm bg-emerald-500"></span> Validé
  </span>
</div>
```

### Barre de progression de complétion
```html
<div class="flex items-center gap-2">
  <div class="w-16 h-2 bg-gray-200 rounded-full overflow-hidden">
    <div class="h-full bg-blue-600 rounded-full" [style.width.%]="completion"></div>
  </div>
  <span class="text-sm text-gray-700">{{ completion }}%</span>
</div>
```

---

## 11. ICÔNES SVG INLINE

> RÈGLE : Toujours inline SVG. Jamais de lucide-angular, jamais de ngx-icons.

### Dashboard / LayoutDashboard
```html
<svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
    d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"/>
</svg>
```

### Users / Collaborateurs
```html
<svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
    d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z"/>
</svg>
```

### FileText / Tests
```html
<svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
    d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
</svg>
```

### BookOpen / Catalogue
```html
<svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
    d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"/>
</svg>
```

### Settings / Admin
```html
<svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
    d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"/><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
</svg>
```

### Plus / Ajouter
```html
<svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
</svg>
```

### Search / Loupe
```html
<svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
    d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
</svg>
```

### Eye / Voir
```html
<svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
    d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/>
</svg>
```

### Trash / Supprimer
```html
<svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
    d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
</svg>
```

### User / Profil
```html
<svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
    d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"/>
</svg>
```

### LogOut / Déconnexion
```html
<svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
    d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"/>
</svg>
```

### ChevronDown / Flèche bas
```html
<svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
</svg>
```

### Award / Trophée
```html
<svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
    d="M9 12l2 2 4-4M7.835 4.697a3.42 3.42 0 001.946-.806 3.42 3.42 0 014.438 0 3.42 3.42 0 001.946.806 3.42 3.42 0 013.138 3.138 3.42 3.42 0 00.806 1.946 3.42 3.42 0 010 4.438 3.42 3.42 0 00-.806 1.946 3.42 3.42 0 01-3.138 3.138 3.42 3.42 0 00-1.946.806 3.42 3.42 0 01-4.438 0 3.42 3.42 0 00-1.946-.806 3.42 3.42 0 01-3.138-3.138 3.42 3.42 0 00-.806-1.946 3.42 3.42 0 010-4.438 3.42 3.42 0 00.806-1.946 3.42 3.42 0 013.138-3.138z"/>
</svg>
```

### CheckCircle / Validé
```html
<svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
    d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
</svg>
```

### Clock / Horloge
```html
<svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
    d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/>
</svg>
```

### Alert Triangle / Avertissement
```html
<svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
    d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/>
</svg>
```

### UserPlus / Ajouter user
```html
<svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
    d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z"/>
</svg>
```

### Play / Démarrer test
```html
<svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
    d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z"/><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
</svg>
```

### Clipboard / Tests assignés
```html
<svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
    d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"/>
</svg>
```

---

## 12. STRUCTURE DE PAGE TYPE

### Pattern Header + Content
```html
<div class="space-y-6">
  <!-- En-tête de page -->
  <div class="flex justify-between items-start">
    <div>
      <h1 class="text-3xl font-semibold text-gray-900">Titre de la page</h1>
      <p class="text-gray-600 mt-2">Description courte</p>
    </div>
    <!-- Bouton d'action principal (optionnel) -->
    <button (click)="openDialog()" class="inline-flex items-center gap-2 bg-[#E63946] hover:bg-[#d32f3c] text-white font-medium px-4 py-2 rounded-lg transition-colors">
      <!-- SVG + --> Nouvelle action
    </button>
  </div>

  <!-- Grid de stat cards (si applicable) -->
  <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
    <!-- stat cards -->
  </div>

  <!-- Contenu principal -->
  <div class="bg-white rounded-xl border border-gray-200 shadow-sm">
    <div class="px-6 pt-6 pb-2">
      <h3 class="text-lg font-semibold text-gray-900">Section</h3>
    </div>
    <div class="px-6 pb-6">
      <!-- table, liste, formulaire... -->
    </div>
  </div>
</div>
```

---

## 13. ÉTAT VIDE

```html
<div class="text-center py-12">
  <div class="flex justify-center mb-4">
    <!-- SVG icon h-12 w-12 text-gray-300 -->
  </div>
  <h3 class="text-lg font-medium text-gray-900 mb-2">Aucun élément</h3>
  <p class="text-gray-500 text-sm">Message descriptif</p>
</div>
```

---

## 14. NAVIGATE (Router)

```typescript
private router = inject(Router);

// Navigation simple
this.router.navigate(['/dashboard']);

// Avec paramètre
this.router.navigate(['/execute-test', assignmentId]);

// ExecuteTest: lire paramètre
import { ActivatedRoute } from '@angular/router';
private route = inject(ActivatedRoute);
readonly assignmentId = this.route.snapshot.paramMap.get('id') ?? '';
```

---

## 15. PATTERNS SPÉCIFIQUES PAR PAGE

### CollabDashboard
- 4 stat cards COMPACT (py-2 px-3)
- CSS bars grouped (declared=bleu, validated=vert) pour chaque skill
- Table des 5 derniers tests (reverse)
- `currentUser.skills.filter(s => s.validatedLevel !== undefined).length`

### RHDashboard
- 4 stat cards STANDARD (px-6 pt-6)
- Barre de complétion de profil moyenne
- Liste de distribution des statuts

### CollabTests
- Tabs : Tous / Assignés / En cours / Complétés
- Bouton "Démarrer" `router.navigate(['/execute-test', a.id])`
- Badges statut colorés

### CollabProfile
- Avatar initiales bg-[#E63946]
- FormsModule pour édition nom/email en ligne
- Liste des compétences avec badges niveau

### RHUsers
- Search bar en haut de card
- Table avec barre de complétion (w-16 h-2)
- Dialog "Nouveau collaborateur" : nom + email + select jobPosition
- Delete avec confirm

### RHEvaluations
- 4 stat cards COMPACT
- Tabs : Tous / Assignés / En cours / Complétés
- Dialog "Assigner un test" : select user + select evaluation

### AdminData
- 3 tabs : Compétences / Catégories / Fiches de poste
- Chaque tab : table + bouton "Nouvelle X" + dialog de création
- Skill dialog : nom + select catégorie
- JobPosition dialog : nom + checkboxes catégories requises

### ExpertTests
- 3 stat cards STANDARD
- Tabs : Tous / En attente / Validés
- Cards par évaluation avec toggle isValidated / isActive
- Dialog création : nom + select skill + nbQuestionsPerLevel + textarea prompts

### SkillsCatalog
- Search input + tabs catégories
- Cards par skill avec liste levelDefinitions
- Badge niveau coloré par index
- Légende catégories en bas

### ExecuteTest
- `inject(ActivatedRoute)` pour `assignmentId`
- Écran de démarrage : sélection de niveau de départ (radio)
- Écran test : progress bar CSS, question card, réponses (radio/checkbox/textarea)
- Logique adaptive : pass → niveau+1, fail → niveau-1, edge → complet
- `addTestResult()` + `updateTestAssignment()` + navigate('/tests')

### NotFound
- Carte centrée max-w-md
- Icône alerte rouge
- Bouton retour bg-[#E63946]

---

## 16. TAILWIND V4 — RÈGLES CRITIQUES

```css
/* styles.css — DOIT contenir exactement ceci */
@import "tailwindcss";
@source "./app";   ← LIGNE CRITIQUE : sans cette ligne, 0 class Tailwind ne fonctionne

@theme inline {
  --color-brand: #E63946;
  /* ... autres tokens */
}
```

- Classes personnalisées dans `@layer utilities {}` uniquement
- `bg-[#E63946]` = JIT couleur arbitraire → toujours mettre sans espace
- `text-[10px]` = taille arbitraire → valide en Tailwind v4
- Pas de `tailwind.config.js` → configuration dans CSS uniquement

---

## 17. CHECKLIST NOUVELLE PAGE

Avant de créer un composant Angular, vérifier :

- [ ] `standalone: true` dans `@Component`
- [ ] `FormsModule` dans `imports` si utilisation de `[(ngModel)]`
- [ ] Pas de `CommonModule` (inutile Angular 17+)
- [ ] Tous les signaux déclarés (`signal()`, `computed()`)
- [ ] Icônes en inline SVG (pas de lucide-angular)
- [ ] Graphiques en CSS bars (pas de ngx-charts)
- [ ] Dialogs en `@if (showX())` + overlay fixe
- [ ] Tabs en `signal<TabType>()` + `@if (activeTab() === 'x')`
- [ ] Couleur brand = `#E63946` (pas `#FF4D00`)
- [ ] `@for (item of items(); track item.id)` — toujours `track`
- [ ] IDs numériques pour `User` (`Date.now()` → number), strings pour `Skill`
- [ ] Route déclarée dans `app.routes.ts`
- [ ] Composant exporté (export class)
