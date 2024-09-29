import androidx.compose.ui.graphics.Color

fun Color.toHexString(): String {
    val red = (red * 255).toInt()
    val green = (green * 255).toInt()
    val blue = (blue * 255).toInt()
    val alpha = (alpha * 255).toInt()

    return String.format("%02X%02X%02X%02X", alpha, red, green, blue)
}

fun String.hexToColor(): Color {
    // Ensure the hex string is 8 characters (RGBA)
    val hexColor = if (startsWith("#")) substring(1) else this

    // Parse the hex string into Red, Green, Blue, and Alpha components
    val alpha = hexColor.substring(0, 2).toInt(16) / 255f
    val red = hexColor.substring(2, 4).toInt(16) / 255f
    val green = hexColor.substring(4, 6).toInt(16) / 255f
    val blue = hexColor.substring(6, 8).toInt(16) / 255f

    // Return the Color object
    return Color(red, green, blue, alpha)
}

fun Color.maxSaturation(): Color {
    val max = maxOf(red, green, blue)
    val lerp = (1 - max).coerceIn(0f, 1f)
    return Color(red + lerp, green + lerp, blue + lerp, alpha)
}

fun Color.grayScale(): Color {
    val gray = (red + green + blue) / 3
    return Color(gray, gray, gray, alpha)
}