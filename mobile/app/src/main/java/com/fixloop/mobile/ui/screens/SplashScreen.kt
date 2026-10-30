package com.fixloop.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fixloop.mobile.ui.components.SegmentedLoader
import com.fixloop.mobile.ui.theme.HazardAmber
import com.fixloop.mobile.ui.theme.Ink
import com.fixloop.mobile.ui.theme.InkMuted
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        delay(1800)
        onFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HazardAmber)
            .clickable { onFinished() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Shield Logo Box with 3dp Ink Border & Hard Shadow
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Ink)
                    .border(3.dp, Ink, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Sentinel Shield",
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Ink,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Wordmark
            Text(
                text = "SENTINEL",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Ink,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Tagline
            Text(
                text = "Report once. We handle it.",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Ink.copy(alpha = 0.85f)
            )
        }

        // Footer with 5-Block Animated Segmented Loader
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 54.dp)
        ) {
            SegmentedLoader(
                blockCount = 5,
                blockWidth = 18.dp,
                blockHeight = 22.dp,
                activeColor = Color.White
            )
        }
    }
}
