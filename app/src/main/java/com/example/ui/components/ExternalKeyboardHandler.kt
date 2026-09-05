package com.example.ui.components

import android.view.KeyEvent
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import com.example.ui.MainViewModel

/**
 * Attaches keyboard shortcut listener for external Bluetooth keyboards:
 * - Space: Play / Pause toggle
 * - J: Seek -10s
 * - K: Pause / Play toggle
 * - L: Seek +10s
 * - Ctrl + W: Close active tab
 * - Ctrl + T: New tab
 */
fun Modifier.handleExternalKeyboard(viewModel: MainViewModel): Modifier = this.onKeyEvent { keyEvent ->
    if (keyEvent.type != KeyEventType.KeyDown) return@onKeyEvent false

    val isCtrl = keyEvent.isCtrlPressed

    when {
        // Ctrl + W: Close active tab
        isCtrl && keyEvent.key == Key.W -> {
            viewModel.closeActiveTabShortcut()
            true
        }
        // Ctrl + T: New tab
        isCtrl && keyEvent.key == Key.T -> {
            viewModel.createNewTabShortcut()
            true
        }
        // Space: Play / Pause
        keyEvent.key == Key.Spacebar -> {
            viewModel.togglePlayPause()
            true
        }
        // J: Seek -10s
        keyEvent.key == Key.J -> {
            viewModel.seekRelative(-10000L)
            true
        }
        // K: Pause / Play
        keyEvent.key == Key.K -> {
            viewModel.togglePlayPause()
            true
        }
        // L: Seek +10s
        keyEvent.key == Key.L -> {
            viewModel.seekRelative(10000L)
            true
        }
        else -> false
    }
}
