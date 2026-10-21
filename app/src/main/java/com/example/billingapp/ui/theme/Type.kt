package com.example.billingapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.billingapp.R

// Serif for page titles and big numbers, system sans for everything else.
// Instrument Serif only ships one weight, so never ask for bold on it.
val SerifFamily = FontFamily(
    Font(R.font.instrument_serif_regular, FontWeight.Normal)
)

private val Sans = FontFamily.Default

val Typography = Typography(
    // Big figures (totals, headline amounts) stay in the sans so digits and the ₹ sign match.
    displaySmall = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Medium,
        fontSize = 32.sp,
        lineHeight = 38.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = SerifFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 44.sp,
        lineHeight = 48.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = SerifFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 42.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = SerifFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 30.sp,
        lineHeight = 36.sp
    ),
    titleLarge = TextStyle(
        fontFamily = SerifFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 26.sp,
        lineHeight = 32.sp
    ),
    titleMedium = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    // Small spaced-out caps: "MENU", "OVERDUE", "+ NEW".
    labelMedium = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.2.sp
    )
)
