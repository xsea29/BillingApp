package com.example.billingapp.ui.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.billingapp.R
import kotlinx.coroutines.delay

/** How long the "Monginis" start-up screen stays up. */
private const val SPLASH_MILLIS = 2000L

/**
 * Shows the "Monginis" start-up screen for two seconds, then [content].
 *
 * Android only keeps its own start-up screen until the app draws its first frame, which is a
 * blink. This picks up where it leaves off: the same white and the same wordmark in the same
 * place, so the two look like one screen. Turning the phone while it is up doesn't start the
 * two seconds over once they have passed.
 */
@Composable
fun SplashGate(content: @Composable () -> Unit) {
    var showing by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        if (showing) {
            delay(SPLASH_MILLIS)
            showing = false
        }
    }

    if (showing) {
        SplashScreen()
    } else {
        content()
    }
}

@Composable
private fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        // 288dp is the canvas the wordmark is drawn on, the same size Android shows it at.
        Image(
            painter = painterResource(R.drawable.splash_wordmark),
            contentDescription = "Monginis",
            modifier = Modifier.size(288.dp)
        )
    }
}
