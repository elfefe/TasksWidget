package com.elfefe.common.controller

import com.elfefe.common.model.Task
import com.elfefe.common.model.TaskState

/**
 * Filtre d'états de la barre de navigation : quels états de cartes la pile
 * montre. Sélection multiple, appliquée aussitôt et retenue d'un démarrage à
 * l'autre (voir `Configs.stateFilters`).
 *
 * Le filtrage se fait à deux endroits, parce que la pile mêle deux sources :
 *
 * * les **tâches persistées** passent par [Tasks.filter], réévalué à chaque
 *   changement de sélection ;
 * * les **cartes de sessions Claude** sont fabriquées à la volée depuis
 *   `ClaudeSessions.running` par `StackController.displayed`, qui appelle
 *   [accepts] lui-même — leur état change toutes les secondes, il ne peut pas
 *   être figé dans un filtre posé une fois pour toutes.
 */
object StateFilter {

    /** Clé du filtre posé sur [Tasks] ; l'ancien « show done » qu'il remplace. */
    private const val KEY = "états"

    val selected: Set<TaskState> get() = Tasks.Configs.configs.stateFilters

    fun isSelected(state: TaskState): Boolean = state in selected

    fun accepts(state: TaskState): Boolean = state in selected

    /** La sélection est-elle celle d'origine ? Sert à dessiner l'icône. */
    fun isDefault(): Boolean = selected == TaskState.DEFAULT

    /**
     * Montre ou tait un état. Le choix est enregistré puis les tâches sont
     * refiltrées dans la foulée : rien à valider, la pile suit.
     */
    fun toggle(state: TaskState) {
        val next = selected.toMutableSet().apply { if (!remove(state)) add(state) }
        Tasks.Configs.configs.updateStateFilters(next)
        apply()
    }

    /**
     * Pose le filtre sur les tâches persistées. À appeler une fois au démarrage :
     * [toggle] s'en charge ensuite.
     *
     * Les tâches de type « claude » passent sans être jugées ici — leur état
     * dépend de la session vivante, et c'est la pile qui les filtre.
     */
    fun apply() {
        Tasks.filter(KEY) { task -> task.type == "claude" || accepts(stateOf(task)) }
    }

    /** État d'une session Claude Code, tel que le CLI le rapporte. */
    fun stateOf(session: ClaudeCode.RunningSession): TaskState = when {
        session.busy -> TaskState.BUSY
        session.waiting -> TaskState.WAITING
        else -> TaskState.IDLE
    }

    /**
     * État d'une carte de tâche. Une tâche épinglée sur une session vivante
     * prend l'état de celle-ci ; sans session, elle vaut ce que vaut une tâche
     * ordinaire.
     */
    fun stateOf(task: Task): TaskState {
        if (task.type == "claude" && task.claudeSessionId.isNotBlank()) {
            ClaudeSessions.running.firstOrNull { it.sessionId == task.claudeSessionId }
                ?.let { return stateOf(it) }
        }
        return if (task.done) TaskState.DONE else TaskState.TODO
    }
}
