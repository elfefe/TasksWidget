package com.elfefe.common.controller

import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.WString
import com.sun.jna.ptr.IntByReference

/**
 * Envoi d'un message à une session Claude Code **déjà ouverte dans un terminal**.
 *
 * Claude Code n'expose aujourd'hui aucun canal local pour cela : le format de
 * `~/.claude/sessions/<pid>.json` prévoit bien un `messagingSocketPath`, mais le
 * CLI ne le renseigne jamais (2.1.220). On passe donc par la console elle-même :
 * `AttachConsole(pid)` rattache le widget à la console de la session, puis
 * `WriteConsoleInput` y dépose des événements clavier — exactement ce que le
 * sous-système console produirait si l'utilisateur tapait. La TUI les lit sans
 * savoir d'où ils viennent.
 *
 * Le texte part en **collage entre crochets** (bracketed paste, `ESC[200~ …
 * ESC[201~`) : c'est ainsi qu'un terminal signale un collage, et c'est ce qui
 * permet au markdown multiligne d'arriver entier sans que chaque saut de ligne
 * ne valide le prompt. La validation est un `Entrée` envoyé séparément, une fois
 * le collage digéré.
 */
object ClaudeInject {

    /** Taille d'un INPUT_RECORD (variante KEY_EVENT) sur Windows. */
    private const val RECORD_SIZE = 20L

    private const val KEY_EVENT: Short = 1
    private const val VK_RETURN: Short = 0x0D

    /** Échappement ASCII (0x1B), construit plutôt qu'écrit : un caractère de
     * contrôle dans un littéral ne survit pas au premier outil qui reformate. */
    private val ESC = Char(27).toString()
    private const val CR = "\r"

    private const val GENERIC_READ = 0x80000000.toInt()
    private const val GENERIC_WRITE = 0x40000000
    private const val FILE_SHARE_READ = 0x00000001
    private const val FILE_SHARE_WRITE = 0x00000002
    private const val OPEN_EXISTING = 3

    /** Délai laissé à la TUI pour absorber le collage avant la validation. */
    private const val PASTE_SETTLE_MS = 150L

    private interface Kernel32Ex : Library {
        fun AttachConsole(dwProcessId: Int): Boolean
        fun FreeConsole(): Boolean
        fun CreateFileW(
            lpFileName: WString,
            dwDesiredAccess: Int,
            dwShareMode: Int,
            lpSecurityAttributes: Pointer?,
            dwCreationDisposition: Int,
            dwFlagsAndAttributes: Int,
            hTemplateFile: Pointer?
        ): Pointer

        fun WriteConsoleInputW(
            hConsoleInput: Pointer,
            lpBuffer: Pointer,
            nLength: Int,
            lpNumberOfEventsWritten: IntByReference
        ): Boolean

        fun CloseHandle(hObject: Pointer): Boolean
    }

    private val kernel32: Kernel32Ex? by lazy {
        runCatching { Native.load("kernel32", Kernel32Ex::class.java) }
            .onFailure { log("ClaudeInject: kernel32 indisponible\n" + it.stackTraceToString()) }
            .getOrNull()
    }

    /**
     * Envoie [text] à la session dont le process CLI porte le pid [pid], puis
     * valide. Renvoie `false` si la console n'a pas pu être atteinte (session
     * fermée entre-temps, terminal sans console attachée).
     *
     * `AttachConsole` agit sur le process entier : deux envois simultanés se
     * marcheraient dessus, d'où la synchronisation.
     */
    @Synchronized
    fun send(pid: Long, text: String): Boolean {
        val message = text.replace("\r\n", "\n").replace('\r', '\n').trimEnd()
        if (message.isBlank()) return false

        // Le collage entre crochets véhicule les sauts de ligne sans valider ;
        // dans un flux terminal, une fin de ligne est un retour chariot.
        val payload = ESC + "[200~" + message.replace("\n", CR) + ESC + "[201~"

        if (!write(pid, payload)) return false
        Thread.sleep(PASTE_SETTLE_MS)
        return write(pid, CR)
    }

    /** Interrompt la session (Échap), comme la touche du même nom dans la TUI. */
    @Synchronized
    fun interrupt(pid: Long): Boolean = write(pid, ESC)

    /**
     * Une passe d'écriture : rattachement à la console de [pid], dépôt des
     * événements clavier, détachement. Le détachement est impératif et vit dans
     * un `finally` — laisser le widget attaché à la console d'un terminal le
     * ferait mourir avec elle.
     */
    private fun write(pid: Long, text: String): Boolean {
        val kernel = kernel32 ?: return false
        if (text.isEmpty()) return false
        var attached = false
        var handle: Pointer? = null
        try {
            kernel.FreeConsole()
            if (!kernel.AttachConsole(pid.toInt())) return false
            attached = true

            val opened = kernel.CreateFileW(
                WString("CONIN$"),
                GENERIC_READ or GENERIC_WRITE,
                FILE_SHARE_READ or FILE_SHARE_WRITE,
                null,
                OPEN_EXISTING,
                0,
                null
            )
            if (Pointer.nativeValue(opened) == -1L || Pointer.nativeValue(opened) == 0L) return false
            handle = opened

            val records = Memory(RECORD_SIZE * 2 * text.length)
            text.forEachIndexed { index, char ->
                val virtualKey = if (char == '\r') VK_RETURN else 0
                keyRecord(records, (index * 2).toLong(), char, virtualKey, down = true)
                keyRecord(records, (index * 2 + 1).toLong(), char, virtualKey, down = false)
            }

            val written = IntByReference()
            return kernel.WriteConsoleInputW(opened, records, text.length * 2, written)
        } catch (throwable: Throwable) {
            log("ClaudeInject: envoi vers le pid $pid impossible\n" + throwable.stackTraceToString())
            return false
        } finally {
            handle?.let { runCatching { kernel.CloseHandle(it) } }
            if (attached) runCatching { kernel.FreeConsole() }
        }
    }

    /**
     * Écrit un INPUT_RECORD de type clavier à l'emplacement [slot] du tampon.
     * Disposition Windows : EventType (2 o) + bourrage (2 o), puis
     * KEY_EVENT_RECORD = bKeyDown (4 o), wRepeatCount, wVirtualKeyCode,
     * wVirtualScanCode (2 o chacun), UnicodeChar (2 o), dwControlKeyState (4 o).
     */
    private fun keyRecord(buffer: Memory, slot: Long, char: Char, virtualKey: Short, down: Boolean) {
        val base = slot * RECORD_SIZE
        buffer.setShort(base, KEY_EVENT)
        buffer.setShort(base + 2, 0)
        buffer.setInt(base + 4, if (down) 1 else 0)
        buffer.setShort(base + 8, 1)          // wRepeatCount
        buffer.setShort(base + 10, virtualKey)
        buffer.setShort(base + 12, 0)         // wVirtualScanCode
        buffer.setChar(base + 14, char)
        buffer.setInt(base + 16, 0)           // dwControlKeyState
    }
}
