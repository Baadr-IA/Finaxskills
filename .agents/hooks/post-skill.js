/**
 * Post-Skill Hook
 * S'exécute APRÈS l'exécution réussie d'une tâche de skill.
 */
console.error(JSON.stringify({ 
    systemMessage: "✅ Tâche terminée. Vérification de la stabilité du build...",
    decision: "allow" 
}));
