package com.elfefe.common.controller

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import com.elfefe.common.ui.theme.monoFontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser

/**
 * Lecture du markdown **reçu** (les réponses de Claude), par opposition au
 * markdown qu'on écrit — celui-ci passe par `MarkdownVisualTransformation`, qui
 * stylise un champ de saisie sans jamais toucher au texte.
 *
 * Ici le besoin est l'inverse : afficher proprement un document qu'on ne modifie
 * pas. On s'appuie donc sur l'analyseur de JetBrains (déjà en dépendance), qui
 * comprend ce que la transformation ne sait pas faire — listes, liens, blocs de
 * code, citations — et on en tire une suite de blocs que l'interface compose
 * chacun à sa façon. Un bloc de code veut un cadre, une puce veut un retrait :
 * une seule chaîne stylée n'y suffirait pas.
 */
sealed interface MarkdownBlock {
    data class Paragraph(val text: AnnotatedString) : MarkdownBlock
    data class Heading(val level: Int, val text: AnnotatedString) : MarkdownBlock

    /** [marker] est déjà rendu (« • » ou « 3. ») ; [depth] donne le retrait. */
    data class ListEntry(val marker: String, val text: AnnotatedString, val depth: Int) : MarkdownBlock
    data class CodeBlock(val code: String, val language: String) : MarkdownBlock
    data class Quote(val text: AnnotatedString) : MarkdownBlock
    object Rule : MarkdownBlock
}

/** Étiquette des annotations portant l'URL d'un lien cliquable. */
const val MARKDOWN_URL_TAG = "url"

object MarkdownDocument {

    private val flavour = GFMFlavourDescriptor()

    /**
     * Découpe [source] en blocs affichables. Toute défaillance de l'analyseur
     * retombe sur un simple paragraphe : mieux vaut du markdown brut qu'une
     * conversation vide.
     */
    fun parse(source: String): List<MarkdownBlock> = runCatching {
        val tree = MarkdownParser(flavour).buildMarkdownTreeFromString(source)
        val blocks = mutableListOf<MarkdownBlock>()
        tree.children.forEach { node -> blocks += blocksOf(node, source, depth = 0) }
        blocks.ifEmpty { fallback(source) }
    }.getOrElse {
        log("MarkdownDocument: analyse impossible\n" + it.stackTraceToString())
        fallback(source)
    }

    private fun fallback(source: String): List<MarkdownBlock> =
        if (source.isBlank()) emptyList()
        else listOf(MarkdownBlock.Paragraph(AnnotatedString(source.trim())))

    private fun blocksOf(node: ASTNode, source: String, depth: Int): List<MarkdownBlock> =
        when (node.type) {
            MarkdownElementTypes.ATX_1 -> heading(node, source, 1)
            MarkdownElementTypes.ATX_2 -> heading(node, source, 2)
            MarkdownElementTypes.ATX_3 -> heading(node, source, 3)
            MarkdownElementTypes.ATX_4 -> heading(node, source, 4)
            MarkdownElementTypes.ATX_5 -> heading(node, source, 5)
            MarkdownElementTypes.ATX_6 -> heading(node, source, 6)
            MarkdownElementTypes.SETEXT_1 -> heading(node, source, 1)
            MarkdownElementTypes.SETEXT_2 -> heading(node, source, 2)

            MarkdownElementTypes.PARAGRAPH -> {
                val text = inline(node, source)
                if (text.isBlank()) emptyList() else listOf(MarkdownBlock.Paragraph(text))
            }

            MarkdownElementTypes.UNORDERED_LIST,
            MarkdownElementTypes.ORDERED_LIST -> listEntries(node, source, depth)

            MarkdownElementTypes.CODE_FENCE -> listOf(codeFence(node, source))
            MarkdownElementTypes.CODE_BLOCK -> listOf(
                MarkdownBlock.CodeBlock(node.getTextInNode(source).toString().trimEnd(), "")
            )

            MarkdownElementTypes.BLOCK_QUOTE -> {
                val inner = node.children.flatMap { blocksOf(it, source, depth) }
                val text = inner.filterIsInstance<MarkdownBlock.Paragraph>()
                    .map { it.text }
                    .reduceOrNull { a, b -> AnnotatedString.Builder(a).apply { append("\n"); append(b) }.toAnnotatedString() }
                if (text == null) inner else listOf(MarkdownBlock.Quote(text))
            }

            MarkdownTokenTypes.HORIZONTAL_RULE -> listOf(MarkdownBlock.Rule)

            // Les nœuds restants (fichier, sauts de ligne, HTML) n'ont pas de
            // rendu propre : on descend chercher ce qu'ils contiennent.
            else -> node.children.flatMap { blocksOf(it, source, depth) }
        }

    private fun heading(node: ASTNode, source: String, level: Int): List<MarkdownBlock> {
        val content = node.children.firstOrNull { it.type == MarkdownTokenTypes.ATX_CONTENT }
            ?: node.children.firstOrNull { it.type == MarkdownTokenTypes.SETEXT_CONTENT }
        val text = if (content != null) inline(content, source) else inline(node, source)
        return if (text.isBlank()) emptyList() else listOf(MarkdownBlock.Heading(level, text))
    }

    private fun listEntries(list: ASTNode, source: String, depth: Int): List<MarkdownBlock> {
        val ordered = list.type == MarkdownElementTypes.ORDERED_LIST
        var index = 1
        return list.children
            .filter { it.type == MarkdownElementTypes.LIST_ITEM }
            .flatMap { item ->
                val marker = if (ordered) "${index++}." else "•"
                val blocks = mutableListOf<MarkdownBlock>()
                var first = true
                item.children.forEach { child ->
                    when (child.type) {
                        // La puce d'origine est remplacée par la nôtre.
                        MarkdownTokenTypes.LIST_BULLET, MarkdownTokenTypes.LIST_NUMBER -> {}

                        MarkdownElementTypes.PARAGRAPH -> {
                            val text = inline(child, source)
                            if (text.isNotBlank()) {
                                blocks += MarkdownBlock.ListEntry(
                                    marker = if (first) marker else "",
                                    text = text,
                                    depth = depth
                                )
                                first = false
                            }
                        }

                        // Sous-liste : même traitement, un cran plus à droite.
                        MarkdownElementTypes.UNORDERED_LIST,
                        MarkdownElementTypes.ORDERED_LIST ->
                            blocks += listEntries(child, source, depth + 1)

                        else -> blocks += blocksOf(child, source, depth)
                    }
                }
                blocks
            }
    }

    private fun codeFence(node: ASTNode, source: String): MarkdownBlock.CodeBlock {
        val language = node.children.firstOrNull { it.type == MarkdownTokenTypes.FENCE_LANG }
            ?.getTextInNode(source)?.toString()?.trim().orEmpty()
        val code = buildString {
            node.children.forEach { child ->
                when (child.type) {
                    MarkdownTokenTypes.CODE_FENCE_CONTENT -> append(child.getTextInNode(source))
                    MarkdownTokenTypes.EOL -> append('\n')
                    else -> {}
                }
            }
        }
        return MarkdownBlock.CodeBlock(code.trim('\n'), language)
    }

    /**
     * Contenu d'un bloc, avec ses styles de caractère. Les marqueurs (`**`, `` ` ``,
     * crochets d'un lien) ne sont pas repris : c'est du balisage, pas du texte.
     */
    private fun inline(node: ASTNode, source: String): AnnotatedString {
        val builder = AnnotatedString.Builder()
        appendInline(node, source, builder)
        return builder.toAnnotatedString().trimmed()
    }

    /**
     * Équivalent de `trim` pour un texte stylé : le contenu d'un titre commence
     * après l'espace qui suit les dièses, et les fins de ligne deviennent des
     * espaces — sans quoi chaque bloc traînerait des blancs à ses extrémités.
     * `subSequence` est indispensable ici : il décale les styles et les
     * annotations avec le texte, ce qu'un `trim` sur la chaîne nue perdrait.
     */
    private fun AnnotatedString.trimmed(): AnnotatedString {
        var start = 0
        var end = length
        while (start < end && this[start].isWhitespace()) start++
        while (end > start && this[end - 1].isWhitespace()) end--
        return if (start == 0 && end == length) this else subSequence(start, end)
    }

    private fun appendInline(node: ASTNode, source: String, builder: AnnotatedString.Builder) {
        when (node.type) {
            MarkdownElementTypes.STRONG ->
                styled(node, source, builder, SpanStyle(fontWeight = FontWeight.Bold))

            MarkdownElementTypes.EMPH ->
                styled(node, source, builder, SpanStyle(fontStyle = FontStyle.Italic))

            GFMElementTypes.STRIKETHROUGH ->
                styled(node, source, builder, SpanStyle(textDecoration = TextDecoration.LineThrough))

            MarkdownElementTypes.CODE_SPAN -> {
                val start = builder.length
                node.children.forEach { child ->
                    if (child.type != MarkdownTokenTypes.BACKTICK) builder.append(child.getTextInNode(source))
                }
                builder.addStyle(SpanStyle(fontFamily = monoFontFamily), start, builder.length)
            }

            MarkdownElementTypes.INLINE_LINK,
            MarkdownElementTypes.FULL_REFERENCE_LINK,
            MarkdownElementTypes.SHORT_REFERENCE_LINK -> link(node, source, builder)

            MarkdownTokenTypes.AUTOLINK,
            MarkdownTokenTypes.EMAIL_AUTOLINK -> {
                val url = node.getTextInNode(source).toString().trim('<', '>')
                appendLink(builder, url, url)
            }

            // Une image se réduit à son texte de remplacement : le widget est
            // trop étroit pour en afficher une, et taire la ligne serait pire.
            MarkdownElementTypes.IMAGE -> {
                val label = node.findChild(MarkdownElementTypes.LINK_TEXT)
                    ?.getTextInNode(source)?.toString()?.trim('[', ']').orEmpty()
                if (label.isNotBlank()) builder.append("🖼 $label")
            }

            MarkdownTokenTypes.EOL -> builder.append(' ')
            MarkdownTokenTypes.HARD_LINE_BREAK -> builder.append('\n')

            MarkdownTokenTypes.TEXT,
            MarkdownTokenTypes.WHITE_SPACE,
            MarkdownTokenTypes.COLON,
            MarkdownTokenTypes.SINGLE_QUOTE,
            MarkdownTokenTypes.DOUBLE_QUOTE,
            MarkdownTokenTypes.LPAREN,
            MarkdownTokenTypes.RPAREN,
            MarkdownTokenTypes.LBRACKET,
            MarkdownTokenTypes.RBRACKET,
            MarkdownTokenTypes.LT,
            MarkdownTokenTypes.GT,
            MarkdownTokenTypes.EXCLAMATION_MARK -> builder.append(node.getTextInNode(source))

            else ->
                if (node.children.isEmpty()) builder.append(node.getTextInNode(source))
                else node.children.forEach { appendInline(it, source, builder) }
        }
    }

    private fun styled(
        node: ASTNode,
        source: String,
        builder: AnnotatedString.Builder,
        style: SpanStyle
    ) {
        val start = builder.length
        // Les deux premiers et deux derniers enfants d'un STRONG sont ses
        // marqueurs ; ceux d'un EMPH, un de chaque côté.
        val markers = if (node.type == MarkdownElementTypes.STRONG) 2 else 1
        node.children
            .drop(markers)
            .dropLast(markers)
            .forEach { appendInline(it, source, builder) }
        builder.addStyle(style, start, builder.length)
    }

    private fun link(node: ASTNode, source: String, builder: AnnotatedString.Builder) {
        val label = node.findChild(MarkdownElementTypes.LINK_TEXT)
            ?.let { text ->
                val inner = AnnotatedString.Builder()
                text.children
                    .filter { it.type != MarkdownTokenTypes.LBRACKET && it.type != MarkdownTokenTypes.RBRACKET }
                    .forEach { appendInline(it, source, inner) }
                inner.toAnnotatedString()
            }
            ?: AnnotatedString(node.getTextInNode(source).toString())
        val url = node.findChild(MarkdownElementTypes.LINK_DESTINATION)
            ?.getTextInNode(source)?.toString()?.trim('<', '>').orEmpty()
        appendLink(builder, label, url)
    }

    private fun appendLink(builder: AnnotatedString.Builder, label: AnnotatedString, url: String) {
        val start = builder.length
        builder.append(label)
        builder.addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, builder.length)
        if (url.isNotBlank()) {
            builder.addStringAnnotation(MARKDOWN_URL_TAG, url, start, builder.length)
        }
    }

    private fun appendLink(builder: AnnotatedString.Builder, label: String, url: String) =
        appendLink(builder, AnnotatedString(label), url)

    private fun ASTNode.findChild(type: org.intellij.markdown.IElementType): ASTNode? =
        children.firstOrNull { it.type == type }
}
