package com.example.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

object SoyTubeMotion {
    /**
     * High-energy spring physics using Spring.DampingRatioMediumBouncy and Spring.StiffnessLow
     * specified for card interactions, tab selections, and bottom sheets.
     */
    val HighEnergySpring: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    fun <T> bouncySpring(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
}

