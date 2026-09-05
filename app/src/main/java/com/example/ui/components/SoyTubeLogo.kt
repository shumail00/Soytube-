package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.QurovaDemoBold

/**
 * Brand logo displaying the reference 'st' mark in qurova-demo.bold,
 * optionally with the full 'SoyTube' display title.
 */
@Composable
fun SoyTubeLogoBadge(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .testTag("soytube_st_logo_badge"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "st",
            fontFamily = QurovaDemoBold,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.58f).sp,
            color = contentColor,
            lineHeight = (size.value * 0.58f).sp,
            modifier = Modifier.padding(bottom = 2.dp)
        )
    }
}

@Composable
fun SoyTubeHeaderBrand(
    modifier: Modifier = Modifier,
    showFullTitle: Boolean = true
) {
    Row(
        modifier = modifier.testTag("soytube_header_brand"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SoyTubeLogoBadge(
            size = 28.dp,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
        if (showFullTitle) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "soytube",
                fontFamily = QurovaDemoBold,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = (-0.5).sp
            )
        }
    }
}
