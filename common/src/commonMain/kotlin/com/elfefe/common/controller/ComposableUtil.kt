package com.elfefe.common.controller

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import java.awt.Dimension
import java.awt.MouseInfo
import java.awt.Point
import java.awt.Rectangle

fun WindowPosition.toPoint() = Point(
    x.value.toInt(),
    y.value.toInt()
)

fun DpSize.toDimension() = Dimension(
    width.value.toInt(),
    height.value.toInt()
)

fun WindowState.getRectangle(): Rectangle {
    return Rectangle(
        position.toPoint(),
        size.toDimension()
    )
}

fun isMouseNearWindow(windowLocation: Point, windowSize: Dimension, threshold: Int = 20): Boolean {
    val mousePos = MouseInfo.getPointerInfo().location
    val windowLeft = windowLocation.x
    val windowRight = windowLocation.x + windowSize.width
    val windowTop = windowLocation.y
    val windowBottom = windowLocation.y + windowSize.height

    val withinX = mousePos.x in (windowLeft - threshold)..(windowRight + threshold)
    val withinY = mousePos.y in (windowTop - threshold)..(windowBottom + threshold)

    return withinX && withinY
}
