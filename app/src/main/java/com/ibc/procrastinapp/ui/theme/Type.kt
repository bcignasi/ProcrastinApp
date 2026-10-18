package com.ibc.procrastinapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ibc.procrastinapp.R

private val DmSans = FontFamily(
    Font(R.font.dm_sans_regular, FontWeight.Normal),
    Font(R.font.dm_sans_medium, FontWeight.Medium),
    Font(R.font.dm_sans_semibold, FontWeight.SemiBold),
    Font(R.font.dm_sans_bold, FontWeight.Bold)
)

private val DefaultTypography = Typography()

// Keep the existing text metrics; only replace the font family.
val Typography = Typography(
    displayLarge = DefaultTypography.displayLarge.copy(fontFamily = DmSans),
    displayMedium = DefaultTypography.displayMedium.copy(fontFamily = DmSans),
    displaySmall = DefaultTypography.displaySmall.copy(fontFamily = DmSans),
    headlineLarge = DefaultTypography.headlineLarge.copy(fontFamily = DmSans),
    headlineMedium = DefaultTypography.headlineMedium.copy(fontFamily = DmSans),
    headlineSmall = DefaultTypography.headlineSmall.copy(fontFamily = DmSans),
    bodyLarge = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = DefaultTypography.bodyMedium.copy(fontFamily = DmSans),
    bodySmall = DefaultTypography.bodySmall.copy(fontFamily = DmSans),
    titleLarge = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = DefaultTypography.titleSmall.copy(fontFamily = DmSans),
    labelLarge = DefaultTypography.labelLarge.copy(fontFamily = DmSans),
    labelMedium = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = DefaultTypography.labelSmall.copy(fontFamily = DmSans)
)
