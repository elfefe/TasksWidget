package com.elfefe.common.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elfefe.common.controller.MARKDOWN_URL_TAG
import com.elfefe.common.controller.MarkdownBlock
import com.elfefe.common.controller.MarkdownDocument
import com.elfefe.common.controller.log
import com.elfefe.common.model.ThemeColors

/**
 * Affiche un texte markdown reçu (réponse de Claude) : titres, listes, blocs de
 * code, citations, liens cliquables. Lecture seule — pour l'écriture, c'est
 * `MarkdownVisualTransformation` qui opère, directement dans le champ de saisie.
 */
@Composable
fun MarkdownText(
    markdown: String,
    colors: ThemeColors,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 11.sp,
    maxLines: Int = Int.MAX_VALUE
) {
    val blocks = remember(markdown) { MarkdownDocument.parse(markdown) }

    Column(modifier) {
        blocks.forEach { block -> MarkdownBlockView(block, colors, fontSize, maxLines) }
    }
}

@Composable
private fun MarkdownBlockView(
    block: MarkdownBlock,
    colors: ThemeColors,
    fontSize: TextUnit,
    maxLines: Int
) {
    when (block) {
        is MarkdownBlock.Paragraph -> LinkableText(
            text = block.text,
            colors = colors,
            style = TextStyle(color = colors.onBackground, fontSize = fontSize),
            maxLines = maxLines,
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
        )

        is MarkdownBlock.Heading -> LinkableText(
            text = block.text,
            colors = colors,
            style = TextStyle(
                color = colors.onBackground,
                // Un titre reste un titre, mais dans une colonne de 340 dp les
                // tailles d'un document imprimé n'ont aucun sens : on se
                // contente d'un écart marqué au-dessus du corps de texte.
                fontSize = fontSize * when (block.level) {
                    1 -> 1.45f
                    2 -> 1.3f
                    3 -> 1.15f
                    else -> 1.05f
                },
                fontWeight = FontWeight.Bold
            ),
            maxLines = maxLines,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp)
        )

        is MarkdownBlock.ListEntry -> Row(
            Modifier.fillMaxWidth().padding(start = (block.depth * 12).dp, top = 1.dp, bottom = 1.dp)
        ) {
            // Marqueur et texte alignés par la ligne de base : cadrés par le
            // haut, la puce flotte au-dessus ou au-dessous de sa propre ligne,
            // les deux textes n'ayant ni la même hauteur ni le même glyphe.
            Text(
                block.marker,
                color = colors.onBackground.copy(alpha = 0.7f),
                fontSize = fontSize,
                modifier = Modifier
                    .alignByBaseline()
                    .width(if (block.marker.length > 1) 18.dp else 12.dp)
            )
            LinkableText(
                text = block.text,
                colors = colors,
                style = TextStyle(color = colors.onBackground, fontSize = fontSize),
                maxLines = maxLines,
                modifier = Modifier.alignByBaseline().weight(1f)
            )
        }

        is MarkdownBlock.CodeBlock -> Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp)
                .background(colors.onBackground.copy(alpha = 0.07f), RoundedCornerShape(4.dp))
                .padding(6.dp)
        ) {
            if (block.language.isNotBlank()) {
                Text(
                    block.language,
                    color = colors.onBackground.copy(alpha = 0.45f),
                    fontSize = fontSize * 0.8f,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.height(2.dp))
            }
            // Le code ne se replie pas : on le fait défiler plutôt que de casser
            // ses lignes n'importe où.
            Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                Text(
                    block.code,
                    color = colors.onBackground,
                    fontSize = fontSize * 0.95f,
                    fontFamily = FontFamily.Monospace,
                    softWrap = false
                )
            }
        }

        is MarkdownBlock.Quote -> Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
            Text(
                "❝",
                color = colors.onBackground.copy(alpha = 0.35f),
                fontSize = fontSize,
                modifier = Modifier.alignByBaseline().width(14.dp)
            )
            LinkableText(
                text = block.text,
                colors = colors,
                style = TextStyle(
                    color = colors.onBackground.copy(alpha = 0.75f),
                    fontSize = fontSize
                ),
                maxLines = maxLines,
                modifier = Modifier.alignByBaseline().weight(1f)
            )
        }

        MarkdownBlock.Rule -> Box(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp)
                .height(1.dp)
                .background(colors.onBackground.copy(alpha = 0.2f))
        )
    }
}

/** Texte dont les portions annotées d'une URL s'ouvrent au clic. */
@Composable
private fun LinkableText(
    text: AnnotatedString,
    colors: ThemeColors,
    style: TextStyle,
    maxLines: Int,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    val links = remember(text) { text.getStringAnnotations(MARKDOWN_URL_TAG, 0, text.length) }

    if (links.isEmpty()) {
        Text(text, style = style, maxLines = maxLines, overflow = TextOverflow.Ellipsis, modifier = modifier)
        return
    }

    // Les liens sont teintés ici et non à l'analyse : la couleur appartient au
    // thème, pas au document.
    val tinted = remember(text, colors.primary) {
        AnnotatedString.Builder(text).apply {
            links.forEach { addStyle(style.toSpanStyle().copy(color = colors.primary), it.start, it.end) }
        }.toAnnotatedString()
    }

    ClickableText(
        text = tinted,
        style = style,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
        onClick = { offset ->
            tinted.getStringAnnotations(MARKDOWN_URL_TAG, offset, offset).firstOrNull()?.let { link ->
                runCatching { uriHandler.openUri(link.item) }.onFailure { error ->
                    // `log` est une extension de Any : elle nomme son journal
                    // d'apres le receveur, d'ou l'objet du domaine markdown.
                    MarkdownDocument.log("Lien impossible a ouvrir (${link.item})\n" + error.stackTraceToString())
                }
            }
        }
    )
}
