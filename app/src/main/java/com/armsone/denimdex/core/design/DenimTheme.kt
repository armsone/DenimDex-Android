package com.armsone.denimdex.core.design

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object DenimColors {
    val signalRed = Color(0xFFE41E25)
    val indigo = Color(0xFF0B2940)
    val indigoBright = Color(0xFF1A5780)
    val coolBlue = Color(0xFF1A5780)
    val indigoDeep = Color(0xFF050D14)
    val washedDenim = Color(0xFFB3C9D1)
    val fadedDenim = Color(0xFFE8ECEC)
    val charcoal = Color(0xFF141617)
    val inkSoft = Color(0xFF4A4D4F)
    val offWhite = Color(0xFFF2F1EC)
    val canvas = Color(0xFFFAF9F6)
    val brass = Color(0xFF997340)
    val leather = Color(0xFF5C3D29)
    val successGreen = Color(0xFF29704A)
    val warningAmber = Color(0xFFB37821)
    val cardSurface = Color(0xFFFFFFFF)
    val hairline = Color(0x1A141A1C) // 10% opacity
    val softShadow = Color(0x0E080F14) // 5.5% opacity
    val accentColor = Color(0xFF24305C)

    val canvasGradient = Brush.verticalGradient(
        colors = listOf(canvas, Color(0xFFF3F3F1))
    )

    val indigoGradient = Brush.linearGradient(
        colors = listOf(indigoDeep, indigo, Color(0xFF0F3D57))
    )
}

/**
 * Shared corner-radius tokens mirroring the iOS continuous-corner treatment
 * so Scan/Archive/Guide/Settings stay visually coherent.
 */
object DenimShapes {
    val hero = RoundedCornerShape(26.dp)
    val card = RoundedCornerShape(20.dp)
    val tile = RoundedCornerShape(14.dp)
    val button = RoundedCornerShape(16.dp)
    val pill = RoundedCornerShape(50)
}

object DenimTypography {
    val largeTitle = TextStyle(
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.8).sp,
        color = DenimColors.charcoal
    )
    val title1 = TextStyle(
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.6).sp,
        color = DenimColors.charcoal
    )
    val title2 = TextStyle(
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.4).sp,
        color = DenimColors.charcoal
    )
    val title3 = TextStyle(
        fontSize = 19.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.2).sp,
        color = DenimColors.charcoal
    )
    val headline = TextStyle(
        fontSize = 17.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.2).sp,
        color = DenimColors.charcoal
    )
    val body = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        color = DenimColors.inkSoft
    )
    val subheadline = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = DenimColors.inkSoft
    )
    val caption = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        color = DenimColors.inkSoft
    )
    val captionBold = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = DenimColors.charcoal
    )
    val caption2 = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.8.sp,
        color = DenimColors.brass
    )
    val eyebrow = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 2.1.sp,
        color = DenimColors.brass
    )
}

@Composable
fun DenimEyebrow(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = DenimColors.brass
) {
    Text(
        text = text.uppercase(),
        style = DenimTypography.eyebrow,
        color = color,
        modifier = modifier
    )
}

@Composable
fun DenimSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    detail: String? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = DenimTypography.headline,
            color = DenimColors.charcoal
        )
        if (detail != null) {
            Text(
                text = detail,
                style = DenimTypography.caption,
                color = DenimColors.inkSoft
            )
        }
    }
}

/**
 * Modifier mimicking iOS .denimCard:
 * - background: cardSurface (#FFFFFF)
 * - corner radius: 20dp continuous (DenimShapes.card)
 * - border: hairline (0.75dp)
 * - shadow: softShadow, radius 10dp
 * - D-pad focus highlight support for Google TV
 */
fun Modifier.denimCard(
    padding: Dp = 18.dp,
    shape: Shape = DenimShapes.card,
    backgroundColor: Color = DenimColors.cardSurface,
    borderColor: Color = DenimColors.hairline,
    borderWidth: Dp = 0.75.dp
): Modifier = this
    .shadow(
        elevation = 10.dp,
        shape = shape,
        ambientColor = DenimColors.softShadow,
        spotColor = DenimColors.softShadow
    )
    .background(backgroundColor, shape)
    .border(borderWidth, borderColor, shape)
    .padding(padding)

@Composable
fun DenimPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.985f
            isFocused -> 1.03f
            else -> 1.0f
        },
        label = "buttonScale"
    )

    val shape = DenimShapes.button
    val backgroundBrush = if (enabled) {
        DenimColors.indigoGradient
    } else {
        Brush.linearGradient(listOf(Color.Gray.copy(alpha = 0.38f), Color.Gray.copy(alpha = 0.38f)))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .scale(scale)
            .then(
                if (isFocused) {
                    Modifier.border(3.dp, DenimColors.brass, shape)
                } else Modifier
            )
            .shadow(
                elevation = if (enabled) 6.dp else 0.dp,
                shape = shape,
                ambientColor = DenimColors.indigo.copy(alpha = 0.18f),
                spotColor = DenimColors.indigo.copy(alpha = 0.18f)
            )
            .background(brush = backgroundBrush, shape = shape)
            .clip(shape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = DenimTypography.headline.copy(
                    fontSize = 17.sp,
                    color = Color.White
                )
            )
        }
    }
}

@Composable
fun DenimSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.985f
            isFocused -> 1.03f
            else -> 1.0f
        },
        label = "secondaryButtonScale"
    )

    val shape = DenimShapes.button

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .scale(scale)
            .then(
                if (isFocused) {
                    Modifier.border(3.dp, DenimColors.indigoBright, shape)
                } else {
                    Modifier.border(1.dp, DenimColors.hairline, shape)
                }
            )
            .background(DenimColors.cardSurface, shape)
            .clip(shape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = DenimTypography.subheadline.copy(
                    fontSize = 15.sp,
                    color = DenimColors.indigoDeep
                )
            )
        }
    }
}

/**
 * Forced light theme wrapper for DenimDex.
 */
@Composable
fun DenimTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = lightColorScheme(
        primary = DenimColors.indigo,
        onPrimary = Color.White,
        secondary = DenimColors.indigoBright,
        onSecondary = Color.White,
        tertiary = DenimColors.brass,
        background = DenimColors.canvas,
        onBackground = DenimColors.charcoal,
        surface = DenimColors.cardSurface,
        onSurface = DenimColors.charcoal,
        error = DenimColors.signalRed,
        onError = Color.White
    )

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
