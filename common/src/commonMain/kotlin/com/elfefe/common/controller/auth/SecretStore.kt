package com.elfefe.common.controller.auth

import com.elfefe.common.controller.appPrivateDir
import com.elfefe.common.controller.log
import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import java.io.File
import java.util.Base64

/**
 * Range un secret sur le disque en le liant au compte Windows courant.
 *
 * Le jeton de rafraîchissement vaut un mot de passe : il rouvre le compte sans
 * rien demander, indéfiniment. L'écrire en clair dans `%LOCALAPPDATA%`
 * reviendrait à le laisser lisible par n'importe quel programme lancé sur la
 * machine, et à le voir partir dans la moindre sauvegarde du dossier.
 *
 * DPAPI (`CryptProtectData`) chiffre avec une clé dérivée de la session
 * Windows : le fichier est illisible sur une autre machine ou sous un autre
 * compte, sans que l'application ait à gérer la moindre clé.
 */
object SecretStore {

    private const val CRYPTPROTECT_UI_FORBIDDEN = 0x1

    private val file: File get() = File(appPrivateDir, "credentials.dat")

    @Structure.FieldOrder("cbData", "pbData")
    class DataBlob : Structure() {
        @JvmField var cbData: Int = 0
        @JvmField var pbData: Pointer? = null
    }

    private interface Crypt32 : Library {
        fun CryptProtectData(
            dataIn: DataBlob, description: String?, optionalEntropy: DataBlob?,
            reserved: Pointer?, promptStruct: Pointer?, flags: Int, dataOut: DataBlob
        ): Boolean

        fun CryptUnprotectData(
            dataIn: DataBlob, description: Array<String?>?, optionalEntropy: DataBlob?,
            reserved: Pointer?, promptStruct: Pointer?, flags: Int, dataOut: DataBlob
        ): Boolean
    }

    private interface Kernel32Local : Library {
        fun LocalFree(hMem: Pointer?): Pointer?
    }

    private val crypt32: Crypt32? by lazy {
        runCatching { Native.load("crypt32", Crypt32::class.java) }
            .onFailure { log("SecretStore: crypt32 indisponible\n" + it.stackTraceToString()) }
            .getOrNull()
    }

    private val kernel32: Kernel32Local? by lazy {
        runCatching { Native.load("kernel32", Kernel32Local::class.java) }.getOrNull()
    }

    /** Écrit [secret], chiffré. Un secret vide efface le fichier. */
    @Synchronized
    fun save(secret: String) {
        runCatching {
            if (secret.isEmpty()) {
                file.delete()
                return
            }
            val protected = protect(secret.toByteArray(Charsets.UTF_8))
            if (protected == null) {
                log("SecretStore: chiffrement impossible, le jeton n'est pas conservé")
                return
            }
            file.parentFile?.mkdirs()
            file.writeText(Base64.getEncoder().encodeToString(protected))
        }.onFailure { log("SecretStore: écriture impossible\n" + it.stackTraceToString()) }
    }

    /** Relit le secret, ou `null` s'il n'y en a pas ou qu'il est illisible. */
    @Synchronized
    fun load(): String? = runCatching {
        if (!file.exists()) return null
        val raw = Base64.getDecoder().decode(file.readText().trim())
        unprotect(raw)?.toString(Charsets.UTF_8)
    }.onFailure { log("SecretStore: lecture impossible\n" + it.stackTraceToString()) }
        .getOrNull()

    fun clear() = save("")

    private fun protect(data: ByteArray): ByteArray? {
        val kernel = crypt32 ?: return null
        val input = DataBlob()
        val output = DataBlob()
        val memory = Memory(data.size.toLong().coerceAtLeast(1))
        memory.write(0, data, 0, data.size)
        input.cbData = data.size
        input.pbData = memory
        if (!kernel.CryptProtectData(input, "TasksWidget", null, null, null, CRYPTPROTECT_UI_FORBIDDEN, output))
            return null
        return readAndFree(output)
    }

    private fun unprotect(data: ByteArray): ByteArray? {
        val kernel = crypt32 ?: return null
        val input = DataBlob()
        val output = DataBlob()
        val memory = Memory(data.size.toLong().coerceAtLeast(1))
        memory.write(0, data, 0, data.size)
        input.cbData = data.size
        input.pbData = memory
        if (!kernel.CryptUnprotectData(input, null, null, null, null, CRYPTPROTECT_UI_FORBIDDEN, output))
            return null
        return readAndFree(output)
    }

    /** DPAPI alloue le tampon de sortie : à nous de le rendre au système. */
    private fun readAndFree(blob: DataBlob): ByteArray? {
        val pointer = blob.pbData ?: return null
        val bytes = pointer.getByteArray(0, blob.cbData)
        kernel32?.LocalFree(pointer)
        return bytes
    }
}
