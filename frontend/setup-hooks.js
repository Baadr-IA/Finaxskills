const fs = require('fs');
const path = require('path');

const projectRoot = path.join(__dirname, '..');
const gitHooksDir = path.join(projectRoot, '.git', 'hooks');
const preCommitHookPath = path.join(gitHooksDir, 'pre-commit');

const hookScript = `#!/bin/sh
# Finaxys Design System Pre-commit Hook
echo "🔍 Vérification de la conformité du design..."

# Aller dans le dossier frontend
cd frontend

# Lancer le script de vérification
npm run check-design

# Récupérer le code de sortie
RESULT=$?

if [ $RESULT -ne 0 ]; then
  echo "❌ Erreur : Le commit a été bloqué car le code n'est pas conforme au Design System Finaxys."
  echo "Veuillez corriger la police (Inter) ou les logos SVG avant de réessayer."
  exit 1
fi

echo "✅ Design conforme. Commit autorisé."
exit 0
`;

if (!fs.existsSync(gitHooksDir)) {
    console.error("❌ Erreur : Dossier .git/hooks introuvable. Assurez-vous d'être à la racine du projet Git.");
    process.exit(1);
}

try {
    fs.writeFileSync(preCommitHookPath, hookScript);
    // Donner les permissions d'exécution (sur macOS/Linux, Windows l'ignore mais c'est pas grave)
    try {
        fs.chmodSync(preCommitHookPath, '755');
    } catch (e) {}
    
    console.log("✅ Hook pre-commit installé avec succès !");
    console.log("Désormais, chaque commit sera vérifié par 'npm run check-design'.");
} catch (err) {
    console.error("❌ Erreur lors de l'installation du hook :", err.message);
}
