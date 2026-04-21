# 📖 Guide d'utilisation — Finaskills Design System

## Table des matières
1. [Ouverture du fichier](#-ouverture-du-fichier)
2. [Navigation](#-navigation)
3. [Pour les designers](#-pour-les-designers)
4. [Pour les développeurs](#-pour-les-développeurs)
5. [Tokens explicités](#-tokens-explicités)
6. [Copier les valeurs](#-copier-les-valeurs)
7. [Tips & Astuces](#-tips--astuces)

---

## 🚀 Ouverture du fichier

### Méthode 1 : Double-clic
```
Windows
├── Clic droit sur finaskills-design-system.html
├── "Ouvrir avec..." → Sélectionnez votre navigateur
└── Clic

macOS
├── Double-clic sur le fichier
└── S'ouvre dans le navigateur par défaut

Linux
├── Double-clic sur le fichier
└── Ou: firefox finaskills-design-system.html
```

### Méthode 2 : Glisser-déposer
```
Glissez le fichier HTML dans votre navigateur (onglet ouvert)
```

### Méthode 3 : Terminal
```bash
# Windows
start finaskills-design-system.html

# macOS
open finaskills-design-system.html

# Linux
firefox finaskills-design-system.html
```

---

## 🧭 Navigation

### Structure générale

```
┌─────────────────────────────────────────┐
│  📍 Header Menu                         │
│  [Theme] [Layout] [Components] [Chart]  │
└─────────────────────────────────────────┘
            ↓ Clic
┌─────────────────────────────────────────┐
│  Design System › Theme                  │ ← Breadcrumb
│  Design Tokens                          │
│  ──────────────────────────────────────│
│  [Couleurs] [Typographie] [Spacing] ← Secondary tabs
└─────────────────────────────────────────┘
```

### Naviguer entre pages

1. **Header Menu** (en haut)
   - Clic : Change la page complète
   - States : Active (bordure orange), Hover (gris clair)
   - 4 pages : Theme, Layout, Components, Chart

2. **Secondary Tabs** (sous le titre)
   - Clic : Bascule entre sous-sections
   - Exemple : Dans Theme → Couleurs | Typographie | Spacing
   - Chaque page a sa propre structure

3. **Breadcrumb** (chemin de navigation)
   - Clic : Retour à la page principale
   - Texte gris clair = lien cliquable
   - Format : Design System › [Page actuelle]

### Navigation clavier

```
Tab   → Naviguer entre les éléments
Enter → Activer un onglet
Esc   → Fermer (si applicable)
F5    → Rafraîchir la page
```

---

## 🎨 Pour les designers

### 1. Explorer la palette de couleurs

📌 **Où** : Page Theme → Onglet "Couleurs"

```
Primaire
├── Finaksys (orange) — 10 nuances
│   ├── finaxys-0 : #fff7f2 (très clair)
│   ├── finaxys-6 : #ff4d00 (principal) ← MARQUE
│   └── finaxys-9 : #9a2a00 (très foncé)

Sémantique
├── Success (vert) — 10 nuances
├── Error (rouge) — 10 nuances
├── Warning (jaune) — 10 nuances
└── Info (bleu) — 10 nuances

Neutres
├── Gray (0-9) — 10 nuances du blanc au noir
├── White (#ffffff)
└── Black (#000000)
```

### 2. Vérifier la typographie

📌 **Où** : Page Theme → Onglet "Typographie"

```
Familles
├── Inter (texte principal)
└── JetBrains Mono (code)

Tailles
├── XS : 12px — Petits labels
├── SM : 14px — Description, hint
├── Base : 16px — Texte normal
├── LG : 18px — Sous-titres
├── XL : 20px — Titres mineurs
├── 2XL : 24px — Titres
└── 3XL : 32px — Titres principaux

Poids
├── 300 : Très léger (rare)
├── 400 : Régulier (standard)
├── 500 : Moyen (accents)
├── 600 : Semi-bold (sous-titres)
└── 700 : Bold (titres)
```

### 3. Comprendre les espacements

📌 **Où** : Page Theme → Onglet "Spacing & Radii"

```
Espacement (base 4px)
├── XS : 4px — Entre éléments très proches
├── SM : 8px — Entre éléments proches
├── MD : 12px — Espacement standard
├── LG : 16px — Espacement confortable
├── XL : 24px — Espacement large
└── 2XL : 32px — Espacement très large

Radii (arrondis)
├── XS : 4px — Subtle
├── SM : 6px — Léger
├── MD : 8px — Standard
├── LG : 12px — Confortable
└── XL : 16px — Very rounded
```

### 4. S'inspirer des composants

📌 **Où** : Page Components → 7 onglets

```
Chaque onglet montre :
├── Visuels interactifs
├── États (hover, active, disabled)
├── Variantes (tailles, couleurs)
└── Utilisation recommandée
```

### Tips pour designers

✅ **Exporter les couleurs**
- Hex values visibles dans le catalogue
- Copier-coller dans Figma

✅ **Construire avec les tokens**
- Toujours utiliser base, SM, MD, LG pour espacements
- Respecter la hiérarchie typographique

✅ **Mainten ir la cohérence**
- 1 couleur primaire (orange)
- Palette sémantique pour statuts
- Gris neutres pour texte

---

## 💻 Pour les développeurs

### 1. Comprendre les CSS Variables

Toutes les couleurs, tailles, etc. sont des **CSS variables** :

```css
/* Dans le <style> du HTML */
:root {
  --finaxys-6: #ff4d00;          /* Couleur primaire */
  --color-gray-9: #0f172a;       /* Texte principal */
  --font-size-base: 16px;        /* Taille texte */
  --spacing-lg: 16px;            /* Espacement */
  --radius-md: 8px;              /* Radii */
}

/* Utilisation en CSS */
.button {
  background-color: var(--finaxys-6);
  padding: var(--spacing-lg);
  border-radius: var(--radius-md);
}
```

### 2. Implémenter en React

```tsx
// App.tsx
import './design-system.css';  // Importer les variables

function Button() {
  return (
    <button style={{
      backgroundColor: 'var(--finaxys-6)',
      padding: 'var(--spacing-lg)',
      borderRadius: 'var(--radius-md)',
      color: 'var(--color-white)',
      border: 'none',
      cursor: 'pointer',
      fontSize: 'var(--font-size-sm)',
      fontWeight: '500',
    }}>
      Click me
    </button>
  );
}
```

### 3. Couleurs sémantiques

```tsx
// Pour succès/erreur/warning/info
const success = 'var(--color-success-6)';   // #10b981
const error = 'var(--color-error-6)';       // #dc2626
const warning = 'var(--color-warning-5)';   // #d97706
const info = 'var(--color-info-5)';         // #3b82f6

function Badge({ status }) {
  const statusColor = {
    success,
    error,
    warning,
    info,
  }[status];
  
  return <span style={{ color: statusColor }}>{status}</span>;
}
```

### 4. Componentes réutilisables

```tsx
// Components/Button.tsx
interface ButtonProps {
  variant?: 'primary' | 'light' | 'danger';
  size?: 'sm' | 'md' | 'lg';
  disabled?: boolean;
  children: React.ReactNode;
}

export function Button({
  variant = 'primary',
  size = 'md',
  disabled = false,
  children,
}: ButtonProps) {
  const baseStyle = {
    backgroundColor: 'var(--finaxys-6)',
    padding: 'var(--spacing-lg)',
    borderRadius: 'var(--radius-md)',
    border: 'none',
    cursor: 'pointer',
    fontFamily: 'var(--font-family-base)',
    transition: 'all 120ms ease',
  };
  
  // Appliquer les variantes...
  
  return <button style={baseStyle}>{children}</button>;
}
```

### 5. Vérifier les états

Consultez les sections **Hover**, **Active**, **Disabled** de chaque composant dans le design system.

---

## 🎯 Tokens explicités

### Les 4 types de tokens

| Type | Exemple | Utilisation |
|------|---------|-------------|
| **Couleur** | `--finaxys-6` | Backgrounds, texte, bordures |
| **Typographie** | `--font-size-lg` | Tailles et poids de police |
| **Espacement** | `--spacing-md` | Padding, margin, gaps |
| **Radii** | `--radius-lg` | border-radius |

### Utiliser les variables

```css
/* ✅ BON */
.card {
  padding: var(--spacing-lg);
  background: var(--color-white);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
}

/* ❌ MAUVAIS */
.card {
  padding: 16px;  /* Hardcoder */
  background: white;  /* Pas d'accès au design system */
  border: 1px solid #e0e0e0;
  border-radius: 8px;
}
```

---

## 📋 Copier les valeurs

### Récupérer une couleur

1. Ouvrir le catalogue
2. Aller à Theme → Couleurs
3. Trouver la couleur désirée
4. Hex code visible : copier-coller

Exemple :
```
Finaksys Primary (Orange)
Hex: #ff4d00
CSS Var: var(--finaxys-6)
```

### Récupérer un espacement

1. Theme → Spacing & Radii
2. Lire le tableau
3. Exemple : md = 12px → utiliser `var(--spacing-md)`

### Récupérer une taille de police

1. Theme → Typographie
2. Consulter le tableau des tailles
3. Copier la valeur ou la variable CSS

---

## 💡 Tips & Astuces

### 1. Mode hors-ligne
Après première visite, le fichier fonctionne **sans internet** (fonts cachées).

### 2. Imprimer le catalogue
```
Clic droit → Imprimer
Ou Ctrl+P / Cmd+P
→ Sauvegarder en PDF
```

### 3. Partagez vos questions
Si vous modifiez le design system :
- Documentez les changements
- Notifiez l'équipe design
- Mettez à jour les variables

### 4. Responsive
Le catalogue fonctionne sur :
- 📱 Mobile (portrait/paysage)
- 📊 Tablet
- 💻 Desktop
- 🖥️ Large screens

### 5. Performance
Fichier minimaliste (~88 KB) :
- Chargement rapide
- Performance optimale
- Aucune dépendance JavaScript lourde

### 6. Accessibilité
- Tous les onglets au clavier
- Contraste WCAG AA
- Textes descriptifs

---

## 🔗 Prochaines étapes

1. **Ouvrir le catalogue** → Explorez les 4 pages
2. **Consulter les composants** → Page Components → Chaque onglet
3. **Copier les tokens** → Pour vos projets
4. **Implémenter** → Dans votre codebase
5. **Maintenir** → Garder la cohérence

---

**Besoin d'aide ? Consultez `GUIDE-PARTAGE.md` pour le support ! 🤝**
