# 🎨 Finaskills Design System — Catalogue Complet

**Version** : 1.0 | **Date** : 10 Avril 2026

## 📦 Contenu du répertoire

Ce répertoire contient le catalogue interactif du design system Finaskills, prêt à être partagé avec l'ensemble de votre équipe.

```
finaskills-design-system-share/
├── 📄 finaskills-design-system.html    ← Ouvrir CELUI-CI (catalogue interactif)
├── 🎨 finaxys-black.svg               ← Logo Finaksys
├── 📋 README.md                       ← Ce fichier
├── 📖 GUIDE-UTILISATION.md            ← Guide complet
└── 📧 GUIDE-PARTAGE.md               ← Instructions de partage
```

---

## 🚀 Démarrage rapide

### 1. Ouvrir le catalogue
```
Double-clic sur : finaskills-design-system.html
```

### 2. Que trouver dedans ?
- ✅ **4 pages principales** avec 15 onglets
- ✅ **Palette de couleurs** (primaire + sémantique + neutres)
- ✅ **Typographie** (6 tailles, poids variés)
- ✅ **Espacement & Radii** (tokens complets)
- ✅ **Composants** (buttons, forms, badges, cards, tables, etc.)
- ✅ **Exemples** (dashboard, datatable)
- ✅ **Design du menu** et des **onglets**

### 3. Navigation
- Clic sur les onglets du header pour changer de page
- Clic sur les sous-onglets pour explorer les détails
- Breadcrumb en haut pour retour rapide

---

## 📋 Pages disponibles

| Page | Onglets | Description |
|------|---------|-------------|
| **Theme** | 3 | Couleurs, Typographie, Spacing & Radii |
| **Layout** | 5 | Dashboard, Principes, Datatable, Menu, Tabs |
| **Components** | 7 | Cards, Buttons, Forms, Badges, Feedback, Tables, Stats |
| **Chart** | 3 | Palette graphiques, Grid & Axes, Reference |

---

## 🎯 Comment l'utiliser

### Pour les designers
- 🎨 Inspirez-vous des composants
- 🎯 Consultez la palette de couleurs complète
- 📐 Vérifiez les espacements et radii
- ✏️ Prenez les codes hex des couleurs

### Pour les développeurs
- 💻 Copiez les variables CSS
- 🔗 Implémentez les tokens dans React
- 🏗️ Respectez la hiérarchie visuelle
- 📱 Vérifiez la responsivité

### Pour l'équipe produit
- 📊 Comprenez la stratégie de design
- 🎨 Maintenez la cohérence visuelle
- 📋 Renforcez l'identité de marque
- 🚀 Accélérez le développement

---

## 💾 Installation / Utilisation

### ✅ Important : Pas d'installation requise !

Le fichier fonctionne **100% hors-ligne** après ouverture initiale.

1. Ouvrir `finaskills-design-system.html` dans votre navigateur
2. (Connexion internet requise uniquement pour charger les fonts Google la première fois)
3. Après première page chargée → Complètement hors-ligne

### Navigateurs supportés
- ✅ Chrome/Chromium (recommandé)
- ✅ Firefox
- ✅ Safari
- ✅ Edge
- ✅ Tous les navigateurs modernes

---

## 📤 Partager avec d'autres

### Option 1 : Envoyer tout le répertoire
```
Zipper le dossier "finaskills-design-system-share"
Envoyer le ZIP par email
```

### Option 2 : Envoyer juste le HTML
```
Envoyer seulement : finaskills-design-system.html
(Le logo est déjà embédé dedans)
```

### Option 3 : Placer sur serveur intranet
```
Uploader le répertoire complet sur votre serveur
Les collègues y accèdent via URL
```

### Option 4 : Git
```bash
git clone <repo>
cd finaskills-design-system-share
open finaskills-design-system.html
```

---

## 📁 Fichiers

### `finaskills-design-system.html` (88 KB)
Catalogue interactif complet avec :
- Navigation par onglets
- Design tokens complètes
- Exemples de composants
- States (hover, active, disabled)
- **Logo SVG embédé**

### `finaxys-black.svg` (4.5 KB)
Logo Finaksys en format SVG (fichier source)

### `README.md` (ce fichier)
Documentation du répertoire

### `GUIDE-UTILISATION.md`
Guide détaillé pour chaque fonction

### `GUIDE-PARTAGE.md`
Instructions de partage et support

---

## 🔍 Détails techniques

**Format** : HTML5 + CSS3 + Vanilla JavaScript
**Dépendances** : Uniquement Google Fonts (externe)
**Responsive** : Oui, tous les écrans
**Hors-ligne** : Oui, après premier chargement
**Performance** : Très rapide (~88 KB minifié)

---

## 🎨 Design System Token

### Couleurs
- **Primaire** : Orange Finaksys (#FF4D00) — 10 nuances
- **Sémantique** : Success, Error, Warning, Info
- **Neutres** : 10 nuances de gris (gray-0 à gray-9)

### Typographie
- **Font** : Inter (Google Fonts)
- **Tailles** : xs (12px) → 3xl (32px)
- **Poids** : 300, 400, 500, 600, 700

### Espacement
- **Base** : 4px
- **Échelle** : xs (4px) → 2xl (32px)

### Radii
- **Base** : 4px
- **Échelle** : xs (4px) → xl (16px)

---

## ⚙️ Personn alisation avancée

### Editer les couleurs
Ouvrir `finaskills-design-system.html` en éditeur de texte :
```css
:root {
  --finaxys-6: #ff4d00;  /* Modifier ici */
  --color-gray-9: #0f172a;
  /* ... */
}
```

### Ajouter du contenu
Ajouter des sections HTML avant `</body>` (connaissances HTML requises)

### Darkmode (futur)
Framework en place pour ajouter un toggle darkmode

---

## ❓ Troubleshooting

| Problème | Solution |
|----------|----------|
| Fichier ne s'ouvre pas | Essayer double-clic ou glisser dans le navigateur |
| Onglets ne changent pas | Vérifier que JavaScript est activé (F12 → Console) |
| Fonts manquantes | Première visite = besoin d'internet pour charger fonts |
| Affichage cassé | Essayer autre navigateur ou F5 (rafraîchir) |
| Logo manquant | Vérifier que le fichier SVG est au même endroit |

---

## 🔗 Liens utiles

- **Figma Design** : [lien à venir]
- **Repository** : [lien à venir]
- **Documentation** : Voir `GUIDE-UTILISATION.md`
- **Support** : Voir `GUIDE-PARTAGE.md`

---

## 📞 Support

**Questions ?** Consultez :
1. `GUIDE-UTILISATION.md` — Utilisation du catalogue
2. `GUIDE-PARTAGE.md` — Partage et troubleshooting
3. Contacter l'équipe design

---

## 📝 Version & Changelog

### v1.0 (10/04/2026)
- ✅ 4 pages principales
- ✅ 15 onglets thématiques
- ✅ Design system tokens complets
- ✅ Composants UI
- ✅ Examples (Dashboard, Datatable)
- ✅ Menu & Tabs design
- ✅ Style SaaS sobre & moderne

---

**Prêt à explorer le design system ? Ouvrez `finaskills-design-system.html` maintenant ! 🚀**
