package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val QurovaDemoBold = FontFamily(
    Font(R.font.qurova_demo_bold, FontWeight.Bold)
)

val LogoDisplayTextStyle = TextStyle(
    fontFamily = QurovaDemoBold,
    fontWeight = FontWeight.Bold,
    letterSpacing = (-0.5).sp
)

// Set of Material typography styles to start with
val Typography =
    Typography(
        displayLarge = TextStyle(
            fontFamily = QurovaDemoBold,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 38.sp,
            letterSpacing = (-0.5).sp
        ),
        displayMedium = TextStyle(
            fontFamily = QurovaDemoBold,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            lineHeight = 32.sp,
            letterSpacing = (-0.5).sp
        ),
        titleLarge = TextStyle(
            fontFamily = QurovaDemoBold,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            lineHeight = 26.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp
        )
    )
