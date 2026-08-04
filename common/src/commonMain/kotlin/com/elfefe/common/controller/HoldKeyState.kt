package com.elfefe.common.controller

import com.elfefe.common.model.HoldKey
import com.sun.jna.Library
import com.sun.jna.Native

/**
 * Lit l'etat d'une touche modificatrice **sans avoir le focus**.
 *
 * Le widget ne recoit aucun evenement clavier tant qu'on ne clique pas dedans :
 * or c'est justement en approchant la souris, sans l'avoir active, qu'on veut
 * savoir si Ctrl est maintenu. Les evenements AWT ne servent donc a rien ici, et
 * on interroge directement l'etat clavier du systeme.
 */
object HoldKeyState {

    private const val VK_LBUTTON = 0x01
    private const val VK_SHIFT = 0x10
    private const val VK_CONTROL = 0x11
    private const val VK_MENU = 0x12 // Alt

    private interface User32 : Library {
        fun GetAsyncKeyState(vKey: Int): Short
    }

    private val user32: User32? by lazy {
        runCatching { Native.load("user32", User32::class.java) }
            .onFailure { log("HoldKeyState: user32 indisponible\n" + it.stackTraceToString()) }
            .getOrNull()
    }

    /**
     * Le bouton principal de la souris est-il enfonce a cet instant ?
     *
     * Sert a reconnaitre un glisser qui n'a jamais ete conclu : Compose ne
     * delivre pas toujours la fin du geste quand la fenetre qui le suivait
     * disparait, et le widget restait alors persuade qu'un deplacement etait en
     * cours — plus aucun deploiement ni repli automatique.
     */
    fun isPrimaryMouseDown(): Boolean {
        val state = user32?.GetAsyncKeyState(VK_LBUTTON) ?: return false
        return (state.toInt() and 0x8000) != 0
    }

    /** La touche configuree est-elle enfoncee a cet instant ? */
    fun isHeld(key: HoldKey): Boolean {
        val code = when (key) {
            HoldKey.CONTROL -> VK_CONTROL
            HoldKey.ALT -> VK_MENU
            HoldKey.SHIFT -> VK_SHIFT
        }
        // Le bit de poids fort marque une touche actuellement enfoncee ; le bit
        // de poids faible signale un appui depuis le dernier appel, dont on ne
        // veut pas ici.
        val state = user32?.GetAsyncKeyState(code) ?: return false
        return (state.toInt() and 0x8000.toInt()) != 0
    }
}
