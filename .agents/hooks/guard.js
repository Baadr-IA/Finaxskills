const fs = require('fs');

/**
 * Finaxys Design Guard
 * Vérifie la conformité du code généré par l'agent.
 */
function checkConformity(content, filePath) {
    const errors = [];
    const lowerContent = content.toLowerCase();

    // Règle 1 : Police Inter obligatoire
    if (content.includes('font-family') && !content.includes('Inter')) {
        errors.push("La police 'Inter' doit être spécifiée dans le font-family.");
    }

    // Règle 2 : Interdiction du logo SVG Finaxys inline
    // On détecte la signature du logo (viewbox ou nom)
    if (content.includes('<svg') && (lowerContent.includes('finaxys') || content.includes('91.97 61.22'))) {
        errors.push("Logo Finaxys détecté en SVG inline. Utilisez les fichiers .svg de référence.");
    }

    return errors;
}

// Lecture des arguments (passés par l'orchestrateur)
const toolName = process.argv[2];
const toolInput = JSON.parse(process.argv[3] || '{}');
const content = toolInput.content || toolInput.new_string || "";
const filePath = toolInput.file_path || "unknown";

const errors = checkConformity(content, filePath);

if (errors.length > 0) {
    console.error(JSON.stringify({
        decision: "deny",
        reason: "DESIGN SYSTEM VIOLATION: " + errors.join(" | "),
        systemMessage: "Correction requise avant application."
    }));
    process.exit(1);
} else {
    console.log(JSON.stringify({ decision: "allow" }));
    process.exit(0);
}
