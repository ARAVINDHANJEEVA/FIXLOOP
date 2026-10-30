package com.fixloop.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fixloop.mobile.state.SentinelAppState
import com.fixloop.mobile.ui.components.NeoButton
import com.fixloop.mobile.ui.components.NeoCard
import com.fixloop.mobile.ui.components.NeoIconButton
import com.fixloop.mobile.ui.components.NeoStatusChip
import com.fixloop.mobile.ui.theme.*

@Composable
fun ReviewSubmitScreen(
    appState: SentinelAppState,
    onNavigateBack: () -> Unit,
    onSubmitSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val category = appState.formCategory
    val title = appState.formFields["title"]?.ifBlank { null }
        ?: appState.formFields["condition_summary"]?.ifBlank { null }
        ?: "${category.displayName} Issue Report"
    val description = appState.formFields["description"]?.ifBlank { null }
        ?: appState.formFields["detailed_description"]?.ifBlank { null }
        ?: "Grievance submitted by student."

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgPaper)
    ) {
        // Top App Bar (§7.7)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgPaper)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NeoIconButton(onClick = onNavigateBack, size = 40.dp) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp))
            }
            Text(
                text          = "Review your complaint.",
                fontSize      = 18.sp,
                fontWeight    = FontWeight.ExtraBold,
                letterSpacing = (-0.2).sp,
                color         = Ink
            )
        }

        // Category Color Anchor Strip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(category.color)
        )

        // Scrollable Summary Content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Anonymous Tag if enabled
            if (appState.formAnonymous) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Ink)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Text(text = "Anonymous submission", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Summary Card: Category
            NeoCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = SurfaceCard,
                cornerRadius = 16.dp,
                shadowOffset = 4.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "CATEGORY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = InkMuted, letterSpacing = 0.5.sp)
                        Text(
                            text = "Edit",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = IdCardBlue,
                            modifier = Modifier.clickable { onNavigateBack() }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(category.color)
                        )
                        Text(text = category.displayName, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                    }
                }
            }

            // Summary Card: Issue Details
            NeoCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = SurfaceCard,
                cornerRadius = 16.dp,
                shadowOffset = 4.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "ISSUE DETAILS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = InkMuted, letterSpacing = 0.5.sp)
                        Text(
                            text = "Edit",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = IdCardBlue,
                            modifier = Modifier.clickable { onNavigateBack() }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = description, fontSize = 13.5.sp, color = InkMuted, lineHeight = 19.sp)
                    appState.formFields.filter { it.key.startsWith("custom_") && it.value.isNotBlank() }.forEach { (_, v) ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Specific type: $v", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = IdCardBlue)
                    }
                }
            }

            // Summary Card: Severity & Location
            NeoCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = SurfaceCard,
                cornerRadius = 16.dp,
                shadowOffset = 4.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "SEVERITY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = InkMuted, letterSpacing = 0.5.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            val sevColor = when (appState.formSeverity) {
                                "Low" -> ConcreteGray
                                "Medium" -> HazardAmber
                                "High" -> ConeOrange
                                "Critical" -> AlertRed
                                else -> HazardAmber
                            }
                            NeoStatusChip(
                                text = appState.formSeverity,
                                backgroundColor = sevColor,
                                textColor = if (appState.formSeverity == "Critical") Color.White else Ink
                            )
                        }

                        Column(modifier = Modifier.weight(0.9f)) {
                            Text(text = "AFFECTED", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = InkMuted, letterSpacing = 0.5.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${appState.formStudentsAffected} student${if (appState.formStudentsAffected > 1) "s" else ""}",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Ink
                            )
                        }

                        Column(modifier = Modifier.weight(1.3f)) {
                            Text(text = "LOCATION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = InkMuted, letterSpacing = 0.5.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = appState.formLocation, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                        }
                    }
                }
            }

            // Summary Card: Photos
            if (appState.formPhotos.isNotEmpty()) {
                NeoCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = SurfaceCard,
                    cornerRadius = 16.dp,
                    shadowOffset = 4.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "ATTACHMENTS (${appState.formPhotos.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = InkMuted, letterSpacing = 0.5.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            appState.formPhotos.forEach { _ ->
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(HazardAmber.copy(alpha = 0.3f))
                                        .border(2.dp, Ink, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = Ink, modifier = Modifier.size(22.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Sticky Bottom Bar with 3dp top border (.sticky-bottom-bar)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceCard)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Ink)
            )
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                NeoButton(
                    text = "Submit complaint",
                    onClick = {
                        appState.submitComplaint()
                        onSubmitSuccess()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = HazardAmber
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "You'll get a tracking token immediately after this.",
                    fontSize = 11.sp,
                    color = InkMuted
                )
            }
        }
    }
}
