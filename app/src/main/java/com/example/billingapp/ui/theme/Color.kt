package com.example.billingapp.ui.theme

import androidx.compose.ui.graphics.Color

// Monochrome palette: white screens, near-black actions, soft grey fills.
val Paper = Color(0xFFFFFFFF)
val Ink = Color(0xFF111111)
val Fill = Color(0xFFF4F5F7)
val Line = Color(0xFFE6E7EB)
val Outline = Color(0xFFB9BCC4)
val Muted = Color(0xFF6F737B)
val Danger = Color(0xFFD03B3B)

// Charts. Categories use the first five slots of a palette checked for colour-blind
// safety (adjacent pairs) on a white surface; "Other" is neutral grey.
val ChartColors = listOf(
    Color(0xFF2A78D6),
    Color(0xFFEB6834),
    Color(0xFF1BAF7A),
    Color(0xFFEDA100),
    Color(0xFFE87BA4)
)
val ChartOther = Color(0xFFB9BCC4)

// Bars that are not selected.
val BarMuted = Color(0xFFDADCE1)

// "Up is good" text, dark enough to read on white.
val DeltaUp = Color(0xFF006300)

// Invoice amounts. Readable on white (5.0:1) and on the grey total box (4.6:1).
val MoneyGreen = Color(0xFF15803D)
