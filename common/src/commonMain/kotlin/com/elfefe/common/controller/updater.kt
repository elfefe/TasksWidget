package com.elfefe.common.controller

/**
 * Comparaison de versions, partagée par la mise à jour automatique
 * ([AutoUpdater]) et ses tests.
 *
 * Le téléchargement et l'installation vivaient aussi ici, declenches par un clic
 * sur un bandeau. Ils ont ete retires avec ce bandeau : plus rien ne les
 * appelait, et ils deposaient l'installeur dans le dossier d'installation —
 * celui-la meme que `msiexec` efface avant de poser la nouvelle version.
 */

/** Découpe "v1.4.4" / "1.4.4-rc1" en [1, 4, 4] pour comparer proprement. */
fun versionParts(version: String): List<Int> =
    version.trim().removePrefix("v").removePrefix("V")
        .split('.', '-', '_', '+')
        .mapNotNull { part -> part.takeWhile { it.isDigit() }.toIntOrNull() }

/**
 * Vrai si la release distante est strictement plus récente que la version
 * embarquée. Le tag GitHub porte un "v" que la version locale n'a pas : sans
 * cette normalisation, "v1.4.4" et "1.4.4" étaient jugés différents et l'appli
 * se croyait perpétuellement périmée.
 */
fun isRemoteNewer(remoteTag: String?, localVersion: String?): Boolean {
    if (remoteTag == null || localVersion == null) return false
    val remote = versionParts(remoteTag)
    val local = versionParts(localVersion)
    if (remote.isEmpty()) return false
    for (i in 0 until maxOf(remote.size, local.size)) {
        val r = remote.getOrElse(i) { 0 }
        val l = local.getOrElse(i) { 0 }
        if (r != l) return r > l
    }
    return false
}
