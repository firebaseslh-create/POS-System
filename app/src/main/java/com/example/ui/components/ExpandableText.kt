package com.example.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit

/**
 * A composable that displays text with a single line (or specified max lines) and ellipsis (...)
 * when overflowing. Clicking on the text toggles expansion to show the full content.
 */
@Composable
fun ExpandableText(
    text: String,
    modifier: Modifier = Modifier,
    collapsedMaxLines: Int = 1,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    style: TextStyle = TextStyle.Default,
    textAlign: TextAlign? = null,
    minScaleFactor: Float = 0.78f
) {
    var isExpanded by remember(text) { mutableStateOf(false) }
    var isOverflowing by remember(text) { mutableStateOf(false) }
    var scaleFactor by remember(text, fontSize, style) { mutableStateOf(1f) }

    val baseFontSize = if (fontSize != TextUnit.Unspecified) fontSize else style.fontSize
    val scaledFontSize = if (!isExpanded && baseFontSize != TextUnit.Unspecified) {
        baseFontSize * scaleFactor
    } else {
        fontSize
    }

    val textModifier = modifier
        .animateContentSize()
        .clickable(
            enabled = isOverflowing || isExpanded || scaleFactor < 1f,
            onClick = { isExpanded = !isExpanded }
        )

    Text(
        text = text,
        modifier = textModifier,
        color = color,
        fontSize = scaledFontSize,
        fontWeight = fontWeight,
        style = style,
        textAlign = textAlign,
        maxLines = if (isExpanded) Int.MAX_VALUE else collapsedMaxLines,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { textLayoutResult ->
            if (!isExpanded) {
                val overflowed = textLayoutResult.hasVisualOverflow ||
                        textLayoutResult.didOverflowHeight ||
                        textLayoutResult.didOverflowWidth ||
                        textLayoutResult.lineCount > collapsedMaxLines
                if (overflowed && baseFontSize != TextUnit.Unspecified && scaleFactor > minScaleFactor) {
                    scaleFactor = (scaleFactor * 0.92f).coerceAtLeast(minScaleFactor)
                } else {
                    isOverflowing = overflowed
                }
            }
        }
    )
}
