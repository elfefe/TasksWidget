package com.elfefe.common.ui.theme

import androidx.compose.material.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Polices de l'application.
 *
 * Le widget affichait tout en [FontFamily.Default], c'est-a-dire la police
 * systeme choisie par Skia — Segoe UI sur Windows. Segoe UI est dessinee pour le
 * rendu hinte de ClearType ; Compose Desktop dessine le texte via Skia, en
 * antialiasing gris et sans hinting. Aux tailles utilisees ici (9 a 13 sp), en
 * clair sur un fond sombre translucide, les fûts deviennent maigres et
 * irreguliers. D'ou l'impression de police desagreable.
 *
 * Les familles ci-dessous sont embarquees et dessinees pour l'ecran : elles ne
 * dependent plus de ce que la machine a d'installe et ne comptent pas sur le
 * hinting pour tenir en petit.
 */
enum class AppFont(val id: String, val label: String, val description: String) {
    /** Inter : grande hauteur d'x, formes neutres, taillee pour les interfaces. */
    INTER("inter", "Inter", "Neutre, dessinee pour les interfaces"),

    /** IBM Plex Sans : un peu plus de caractere, meme lisibilite en petit. */
    PLEX("plex", "IBM Plex Sans", "Un peu plus de caractere"),

    /** Atkinson Hyperlegible : formes volontairement dissemblables. */
    ATKINSON("atkinson", "Atkinson Hyperlegible", "Lisibilite maximale, lettres tres distinctes"),

    /** La police du systeme, telle qu'avant. */
    SYSTEM("system", "Systeme", "Police par defaut de Windows");

    companion object {
        fun of(id: String?): AppFont = values().firstOrNull { it.id == id } ?: INTER
    }
}

private fun family(vararg faces: Pair<String, FontWeight>): FontFamily =
    FontFamily(faces.map { (resource, weight) -> Font("fonts/$resource", weight) })

// Chargees une seule fois : construire une FontFamily relit les .ttf embarques.
private val inter by lazy {
    family(
        "Inter-Regular.ttf" to FontWeight.Normal,
        "Inter-Medium.ttf" to FontWeight.Medium,
        "Inter-SemiBold.ttf" to FontWeight.SemiBold,
    )
}

private val plex by lazy {
    family(
        "IBMPlexSans-Regular.ttf" to FontWeight.Normal,
        "IBMPlexSans-Medium.ttf" to FontWeight.Medium,
        "IBMPlexSans-SemiBold.ttf" to FontWeight.SemiBold,
    )
}

private val atkinson by lazy {
    family(
        "AtkinsonHyperlegible-Regular.ttf" to FontWeight.Normal,
        "AtkinsonHyperlegible-Bold.ttf" to FontWeight.Bold,
    )
}

/**
 * Police a chasse fixe du rendu markdown.
 *
 * [FontFamily.Monospace] tombait sur Courier New, la seule chasse fixe que
 * Windows garantit : tres maigre, avec des empattements qui disparaissent a
 * 11 sp. Les extraits de code etaient de loin le plus mal rendu de l'interface.
 */
val monoFontFamily: FontFamily by lazy {
    family(
        "JetBrainsMono-Regular.ttf" to FontWeight.Normal,
        "JetBrainsMono-Bold.ttf" to FontWeight.Bold,
    )
}

fun AppFont.family(): FontFamily = when (this) {
    AppFont.INTER -> inter
    AppFont.PLEX -> plex
    AppFont.ATKINSON -> atkinson
    AppFont.SYSTEM -> FontFamily.Default
}

/**
 * `defaultFontFamily` applique la famille a tous les styles Material, pas au
 * seul `body1` : sans cela, les boutons, les libelles de champ et les titres
 * restaient sur la police systeme.
 */
fun typographyOf(font: AppFont): Typography = Typography(
    defaultFontFamily = font.family(),
    body1 = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        // Interligne laissee aux metriques de la police. Elle etait fixee a
        // 24 sp pour un corps de 16 ; or presque tout le widget ecrit en 9 a
        // 13 sp sans retoucher l'interligne, et heritait donc de lignes deux
        // fois trop espacees — surtout visible dans la fenetre de conversation.
        lineHeight = TextUnit.Unspecified,
        // 0.5 sp d'approche vient du gabarit Material, pense pour Roboto en
        // 16 sp. A 11 sp cela fait 4,5 % de la taille : les mots se delitent.
        letterSpacing = 0.sp
    )
)

/** Typographie livree par defaut, pour les rendus hors configuration. */
val Typography: Typography = typographyOf(AppFont.INTER)
