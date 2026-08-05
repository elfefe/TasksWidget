package com.elfefe.common.model

/**
 * États sous lesquels une carte de la pile peut se trouver — ce que le filtre de
 * la barre de navigation permet de montrer ou de taire.
 *
 * Toute carte est dans exactement un de ces états : les sessions Claude Code
 * selon ce que le CLI rapporte, les tâches ordinaires selon qu'elles sont faites
 * ou non. L'ordre de déclaration est aussi l'ordre d'affichage des sessions dans
 * la pile : ce qui travaille passe devant ce qui attend, qui passe devant ce qui
 * dort.
 */
enum class TaskState(val id: String, val label: String) {
    BUSY("busy", "Claude · en cours"),
    WAITING("waiting", "Claude · en attente"),
    IDLE("idle", "Claude · au repos"),
    TODO("todo", "Tâches à faire"),
    DONE("done", "Tâches terminées");

    companion object {
        fun of(id: String?): TaskState? = values().firstOrNull { it.id == id }

        /**
         * Sélection d'origine : tout sauf les tâches terminées — exactement ce
         * que la pile affichait avant que le filtre n'existe.
         */
        val DEFAULT: Set<TaskState> = setOf(BUSY, WAITING, IDLE, TODO)
    }
}
