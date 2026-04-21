# 📧 Guide de partage — Distribution et support

## 🎯 Vue d'ensemble

Ce guide explique comment partager le design system avec vos collègues et comment les supporter.

---

## 📦 Ce que vous pouvez partager

### Option 1️⃣ : Répertoire complet
```
finaskills-design-system-share/
├── finaskills-design-system.html
├── finaxys-black.svg
├── README.md
├── GUIDE-UTILISATION.md
└── GUIDE-PARTAGE.md  (ce fichier)
```
**Avantage** : Tout en un, fichiers de référence inclus
**Taille** : ~100 KB

### Option 2️⃣ : Fichier HTML seul
```
finaskills-design-system.html
```
**Avantage** : Super léger, logo déjà embédé
**Taille** : 88 KB
**Inconvénient** : Pas de guide inclus

### Option 3️⃣ : HTML + Logo source
```
finaskills-design-system.html
finaxys-black.svg
```
**Avantage** : Accès au logo en haute qualité
**Taille** : ~93 KB

---

## 🚀 Méthodes de partage

### Méthode 1 : Email

**Sujet** : 📎 Finaskills Design System v1.0

**Contenu du message** :
```
Bonjour,

Voici le catalogue complet du design system Finaskills !

📄 Fichier principal : finaskills-design-system.html
   → Double-clic pour ouvrir

📖 Guides :
   ✓ README.md — Vue d'ensemble
   ✓ GUIDE-UTILISATION.md — Mode d'emploi détaillé
   ✓ GUIDE-PARTAGE.md — Support et troubleshooting

🎨 Logo :
   ✓ finaxys-black.svg — Logo source (optionnel)

Fonctionnement :
✅ Pas d'installation requise
✅ Fonctionne hors-ligne après première ouverture
✅ Tous les navigateurs modernes

Questions ? Voir GUIDE-PARTAGE.md section Support.

Bon design ! 🚀
```

**Attachements** :
- Zipper le répertoire complet OU
- Envoyer juste finaskills-design-system.html

---

### Méthode 2 : Partage en ligne (Drive/OneDrive/Dropbox)

#### Google Drive
```
1. Créer un dossier "Finaskills-DesignSystem"
2. Uploader le répertoire complet
3. Partager le dossier avec :
   ├── Collègues spécifiques (email)
   ├── Équipe entière (groupe)
   └── Lien public (si pas sensible)
4. Droits d'accès : "Lecteur" (consultation seulement)
```

**Lien à envoyer** :
```
https://drive.google.com/drive/folders/xxxxx
```

#### OneDrive
```
1. Créer un dossier "Finaskills-DesignSystem"
2. Uploader le répertoire
3. Clic droit → Partager
4. Inviter utilisateurs ou générer lien
5. Droits : "Affichage"
```

#### Dropbox
```
1. Créer un dossier / Finaskills-Design
2. Uploader les fichiers
3. Clic droit → Partager ce lien
4. Envoyer le lien (Dropbox gère les droits)
```

---

### Méthode 3 : Repository Git

#### Initialiser un repo Git
```bash
# Créer un repo
mkdir finaskills-design-system
cd finaskills-design-system
git init

# Ajouter les fichiers
copy finaskills-design-system.html .
copy finaxys-black.svg .
copy README.md .
copy GUIDE-UTILISATION.md .
copy GUIDE-PARTAGE.md .

# Commit initial
git add -A
git commit -m "chore: add finaskills design system v1.0"
```

#### GitHub / GitLab
```bash
# Créer un repo sur GitHub/GitLab
# Exemple : finaskills-design-system (public)

# Ajouter remote
git remote add origin https://github.com/finaxys/finaskills-design-system.git
git branch -M main
git push -u origin main
```

**Partager le lien** :
```
https://github.com/finaxys/finaskills-design-system
```

**Pour les collègues** :
```bash
git clone https://github.com/finaxys/finaskills-design-system.git
cd finaskills-design-system
# Ouvrir finaskills-design-system.html
```

---

### Méthode 4 : Serveur intranet / SharePoint

1. Créer un dossier `/resources/design`
2. Uploader le répertoire complet
3. Créer un lien intranet
4. Communiquer l'URL à l'équipe

**URL exemple** :
```
https://intranet.finaxys.com/resources/design/finaskills-design-system
```

---

### Méthode 5 : Slack / Teams

#### Slack
```
1. Créer un channel #design-system
2. Uploader le fichier HTML
3. Message :

   "📊 Finaskills Design System v1.0

   Ouvrez le fichier 👆 pour explorer le catalogue complet.

   🌐 Ou via lien réseau :
   https://...

   📖 Guides :
   • README.md — Vue d'ensemble
   • GUIDE-UTILISATION.md — Mode d'emploi

   Questions ? Thread ci-dessous 👇"
```

#### Teams
```
Même processus, dans un channel Teams
Fichier uploadé automatiquement disponible
```

---

## 👥 Distribution par rôle

### Pour les designers
```
Partager :
✓ finaskills-design-system.html
✓ GUIDE-UTILISATION.md (section "Pour les designers")

Message :
"Voici le design system interactif. 
Explorez les couleurs, typographie, et composants.
Inspirez-vous pour vos designs Figma."
```

### Pour les développeurs
```
Partager :
✓ finaskills-design-system.html
✓ GUIDE-UTILISATION.md (section "Pour les développeurs")
✓ README.md (section "Tokens")

Message :
"Design system avec CSS variables prêtes à l'emploi.
Implémenter les tokens dans votre codebase React.
Tous les détails dans le guide."
```

### Pour l'équipe produit
```
Partager :
✓ finaskills-design-system.html
✓ README.md

Message :
"Catalogue interactif du design Finaskills.
Comprenez la stratégie visuelle et maintenez la cohérence
lors des futurs développements."
```

### Pour la direction
```
Partager :
✓ finaskills-design-system.html (lien ou fichier)
✓ README.md

Message courts (executive summary) :
"Design system complet documenté et partagé.
Accélère le développement et assure la cohérence visuelle.
Tous les tokens (couleurs, typo, spacing) disponibles."
```

---

## ❓ Support client

### FAQ — Questions fréquentes

#### Q : "Le fichier ne s'ouvre pas"
**R** : 
```
1. Essayer double-clic
2. Sinon : Glissez le fichier dans votre navigateur
3. Essayez un autre navigateur (Chrome, Firefox, Safari, Edge)
4. Vérifiez que vous avez un navigateur à jour
```

#### Q : "Je ne vois pas les onglets"
**R** :
```
1. Actualiser la page (F5 ou Cmd+R)
2. Vérifier JavaScript est activé :
   - F12 (DevTools)
   - Chercher des erreurs dans la console
3. Essayez avec un autre navigateur
```

#### Q : "Les fonts manquent"
**R** :
```
1. Première visite = connexion internet pour charger les fonts Google
2. Après première visite = fonctionne hors-ligne (fonts cachées)
3. Si encore manquantes : actualisez après 10 secondes
```

#### Q : "Je veux utiliser les couleurs"
**R** :
```
1. Ouvrir Theme → Couleurs
2. Copier le code HEX ou la variable CSS
3. Utiliser dans Figma, CSS, React, etc.

Exemple : #ff4d00 ou var(--finaxys-6)
```

#### Q : "Puis-je modificar le fichier ?"
**R** :
```
✅ Lire le fichier : OUI
❌ Modifier le fichier : NON (non recommandé, perte d'accès aux mises à jour)

Pour personnaliser :
1. Contact l'équipe design
2. Créez une branche/version locale
3. Documentez les changements
```

#### Q : "Le fichier est trop volumineux"
**R** :
```
88 KB est très léger.
Problème probable : lien de téléchargement, pas le fichier lui-même.

Essayez une autre source ou méthode de partage.
```

#### Q : "Je peux l'utiliser sur [plateforme] ?"
**R** :
```
✅ Email : Oui (attachment)
✅ Drive / OneDrive / Dropbox : Oui
✅ GitHub / GitLab : Oui (public repo)
✅ Intranet / Sharepoint: Oui
✅ Slack / Teams : Oui (upload)
✅ Figma : Non (fichier HTML, pas Figma)
✅ Impression : Oui (Ctrl+P)
✅ Web : Oui (si serveur)
```

---

### Dépannage : Console d'erreurs

**Pour afficher la console** :
```
Windows/Linux : F12 → Console
macOS : Cmd+Option+I → Console
```

**Erreurs courantes** :

| Erreur | Cause | Solution |
|--------|-------|----------|
| `Uncaught SyntaxError` | HTML corrompu | Retélécharger le fichier |
| `Cannot find element` | DOM pas chargé | Actualiser (F5) |
| `CORS error` | Problème cross-origin | Rien à faire, fonctionnelle normalement |
| `Font loading failed` | Pas d'internet | Attendre réaction, puis hors-ligne OK |

---

## 📊 Informations à partager

### Checklist pour chaque partage

```
☐ Fichier principal (finaskills-design-system.html)
☐ Logo (finaxys-black.svg) — optionnel
☐ README.md — Vue d'ensemble
☐ GUIDE-UTILISATION.md — Mode d'emploi
☐ GUIDE-PARTAGE.md — Ce fichier

☐ Message expliquant le contenu
☐ Lien de support (ce guide)
☐ Contact personne responsable
☐ Version du design system (v1.0)
```

### Template de message

```
Bonjour [Nom/Équipe],

🎨 Finaskills Design System v1.0 — Catalogue complet disponible !

📖 Démarrage rapide :
1. Ouvrir : finaskills-design-system.html
2. Lire : README.md
3. Apprendre : GUIDE-UTILISATION.md

📋 Contenu :
✓ 4 pages principales
✓ 15 onglets thématiques
✓ Design tokens complets (couleurs, typo, spacing)
✓ Composants UI avec états
✓ Exemples (dashboard, datatable)

🌐 Accès :
[Drive / Email / GitHub / Intranet]

❓ Questions ?
Consultez GUIDE-PARTAGE.md ou contactez [Contact]

Bon design ! 🚀
```

---

## 🔄 Mises à jour futures

### Processus de mise à jour

```
1. Design team met à jour le système
2. Génère nouveau HTML
3. Tag version (v1.1, v2.0, etc.)
4. Notification à tous les utilisateurs
5. Migration docs/guides si nécessaire
```

### Versioning

```
v1.0 — Version initiale (10/04/2026)
v1.1 — [À venir] Améliorations mineures
v2.0 — [À venir] Grands changements (breaking changes)
```

---

## 📞 Support & Contacts

### Qui contacter pour quoi

| Sujet | Contact | Email |
|-------|---------|-------|
| Questions sur design | Équipe design | design@finaxys.com |
| Problèmes techniques | DevOps / IT | support@finaxys.com |
| Intégration React | Tech lead | tech-lead@finaxys.com |
| Bugs / Issues | GitHub Issues | [lien repo] |

---

## ✅ Checklist finalisation

Avant de partager, vérifiez :

- [ ] Fichier HTML s'ouvre sans erreur
- [ ] Tous les onglets fonctionnent
- [ ] Logo s'affiche bien
- [ ] Fonts chargées (première visite)
- [ ] Légers hors-ligne après chargement
- [ ] Guides inclus et à jour
- [ ] Contacts / Support précisés
- [ ] Version correcte (v1.0)

---

**Prêt à partager ? Heureux de diffuser le design system ! 🎉**
