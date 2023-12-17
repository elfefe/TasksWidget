package com.elfefe.common.controller

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

data class EmojiCategory(
    val name: String,
    val subCategories: List<EmojiSubCategory>
)

data class EmojiSubCategory(
    val name: String,
    val emojis: List<Emoji>
)

data class Emoji(
    val character: String,
    val unicodeName: String,
    val codePoint: String,
    val category: String,
    val subCategory: String,
    val type: String = EmojiApi.DEFAULT_TYPE
)

object EmojiApi {
    const val url = "https://unicode.org/Public/emoji/latest/emoji-test.txt"

    private const val GROUP_HEADER = "# group: "
    private const val SUBGROUP_HEADER = "# subgroup: "
    private const val QUALIFIED_STATUS = "fully-qualified"

    private const val CODE_POINT_SEPARATOR = ";"
    private const val DATA_SEPARATOR = "#"
    private const val INFO_SEPARATOR = " "
    private const val DESCRIPTION_SEPARATOR = ":"

    const val DEFAULT_TYPE = "default"

    private val _emojis = mutableListOf<EmojiCategory>()
    val emojis: List<EmojiCategory>
        get() = _emojis

    private fun emojisParser(emojisData: String, onUpdate: (List<EmojiCategory>) -> Unit) {
        val categories: MutableList<EmojiCategory> = mutableListOf()

        var category = ""
        var subCategory = ""
        var subCategories = mutableListOf<EmojiSubCategory>()
        var emojis: MutableList<Emoji> = mutableListOf()

        for (line in emojisData.split("\n")) {
            if (line.isBlank()) continue

            if (line.startsWith("#")) {
                if (line.startsWith(GROUP_HEADER)) {
                    if (category.isNotBlank()) {
                        categories.add(EmojiCategory(category, subCategories))
                        subCategories = mutableListOf()
                    }
                    category = line.removePrefix(GROUP_HEADER).trim()
                    continue
                }

                if (line.startsWith(SUBGROUP_HEADER)) {
                    if (subCategory.isNotBlank()) {
                        subCategories.add(EmojiSubCategory(subCategory, emojis))
                        emojis = mutableListOf()
                    }
                    subCategory = line.removePrefix(SUBGROUP_HEADER).trim()
                    continue
                }

                continue
            }

            val indexData = line.split(CODE_POINT_SEPARATOR, limit = 2)
            if (indexData.size != 2) continue

            val emojiData = indexData[1].split(DATA_SEPARATOR, limit = 2)
            if (emojiData.size != 2 || emojiData[0].trim() != QUALIFIED_STATUS) continue

            val emojiInfo = emojiData[1].trim().split(INFO_SEPARATOR, limit = 3)
            if (emojiInfo.size != 3) continue


            val emojiDescription = emojiInfo[2].split(DESCRIPTION_SEPARATOR)

            var type = DEFAULT_TYPE
            if (emojiDescription.size == 2)
                type = emojiDescription[1].trim()

            val character: String = emojiInfo[0].trim()
            val unicodeName: String = emojiDescription[0]
            val codePoint: String = indexData[0].trim()

            emojis.add(Emoji(character, unicodeName, codePoint, category, subCategory, type))
        }
        subCategories.add(EmojiSubCategory(subCategory, emojis))
        categories.add(EmojiCategory(category, subCategories))

        onUpdate(categories)
    }

    fun preloadEmojis() {
        queryEmojis {
            _emojis.clear()
            _emojis.addAll(it)
        }
    }
    fun queryEmojis(onGet: (List<EmojiCategory>) -> Unit) {
        HttpClient.newBuilder().build()
            .sendAsync(
                HttpRequest.newBuilder(URI(url)).build(),
                HttpResponse.BodyHandlers.ofString())
            .thenAccept { response ->
                emojisParser(response.body()) { onGet(it) }
            }
    }
}