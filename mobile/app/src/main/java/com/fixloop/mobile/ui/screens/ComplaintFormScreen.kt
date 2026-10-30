package com.fixloop.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fixloop.mobile.model.FieldType
import com.fixloop.mobile.model.FormSchemas
import com.fixloop.mobile.state.SentinelAppState
import com.fixloop.mobile.ui.components.NeoButton
import com.fixloop.mobile.ui.components.NeoIconButton
import com.fixloop.mobile.ui.components.NeoSegmentedSeverity
import com.fixloop.mobile.ui.theme.*

@Composable
fun ComplaintFormScreen(
    appState: SentinelAppState,
    onNavigateBack: () -> Unit,
    onNavigateToReview: () -> Unit,
    onOpenLocationPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val category = appState.formCategory
    val schema = FormSchemas.getSchema(category)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgPaper)
    ) {
        // Top App Bar (§7.6)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgPaper)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NeoIconButton(
                    onClick = {
                        appState.saveCurrentDraft()
                        onNavigateBack()
                    },
                    size = 40.dp
                ) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp))
                }
                Text(
                    text          = "${category.displayName} Issue",
                    fontSize      = 18.sp,
                    fontWeight    = FontWeight.ExtraBold,
                    letterSpacing = (-0.2).sp,
                    color         = Ink
                )

            }
            Text(
                text = "Autosaving",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = InkMuted
            )
        }

        // Persistent 4dp Category Color Anchor Strip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(category.color)
        )

        // Scrollable Form Body
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(18.dp)
        ) {
            // Confidentiality Banner (Harassment / Malpractice / Ragging only)
            if (category.isSensitive) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceCard)
                        .border(2.5.dp, Ink, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Ink),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        Column {
                            Text(text = "Submit anonymously", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Ink)
                            Text(text = "Your identity stays hidden from staff.", fontSize = 11.sp, color = InkMuted)
                        }
                    }

                    // Chunky Switch Toggle
                    Box(
                        modifier = Modifier
                            .width(50.dp)
                            .height(28.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (appState.formAnonymous) HazardAmber else ConcreteGray.copy(alpha = 0.35f))
                            .border(2.dp, Ink, RoundedCornerShape(14.dp))
                            .clickable { appState.formAnonymous = !appState.formAnonymous }
                            .padding(2.dp),
                        contentAlignment = if (appState.formAnonymous) Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Ink)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Dynamic Schema Fields
            schema.forEach { field ->
                Text(text = field.label, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = Ink)
                Spacer(modifier = Modifier.height(6.dp))

                when (field.type) {
                    FieldType.SELECT -> {
                        var expanded by remember { mutableStateOf(false) }
                        val currentValue = appState.formFields[field.key] ?: field.options.firstOrNull() ?: ""
                        if (!appState.formFields.containsKey(field.key) && field.options.isNotEmpty()) {
                            appState.formFields[field.key] = field.options.first()
                        }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceCard)
                                    .border(2.5.dp, Ink, RoundedCornerShape(12.dp))
                                    .clickable { expanded = true }
                                    .padding(horizontal = 14.dp, vertical = 13.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = currentValue, fontSize = 14.sp, color = Ink)
                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = Ink)
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                field.options.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option, fontWeight = FontWeight.Medium) },
                                        onClick = {
                                            appState.formFields[field.key] = option
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        if (currentValue.contains("Other", ignoreCase = true)) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val customKey = "custom_${field.key}"
                            val customVal = appState.formFields[customKey] ?: ""
                            OutlinedTextField(
                                value = customVal,
                                onValueChange = { appState.formFields[customKey] = it },
                                placeholder = { Text("Specify your custom issue type...", fontSize = 13.sp, color = InkMuted) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(2.dp, Ink, RoundedCornerShape(12.dp)),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = SurfaceCard,
                                    unfocusedContainerColor = SurfaceCard,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                singleLine = true
                            )
                        }
                    }
                    FieldType.TEXT -> {
                        val value = appState.formFields[field.key] ?: ""
                        OutlinedTextField(
                            value = value,
                            onValueChange = { appState.formFields[field.key] = it },
                            placeholder = { Text(field.placeholder, fontSize = 13.sp, color = InkMuted) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(2.5.dp, Ink, RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SurfaceCard,
                                unfocusedContainerColor = SurfaceCard,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            singleLine = true
                        )
                    }
                    FieldType.TEXTAREA -> {
                        val value = appState.formFields[field.key] ?: ""
                        OutlinedTextField(
                            value = value,
                            onValueChange = { appState.formFields[field.key] = it },
                            placeholder = { Text(field.placeholder, fontSize = 13.sp, color = InkMuted) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(115.dp)
                                .border(2.5.dp, Ink, RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SurfaceCard,
                                unfocusedContainerColor = SurfaceCard,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            maxLines = 5
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Severity Level Segmented Control
            Text(text = "Severity level", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = Ink)
            Spacer(modifier = Modifier.height(6.dp))
            NeoSegmentedSeverity(
                selectedSeverity = appState.formSeverity,
                onSeveritySelected = { appState.formSeverity = it }
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Number of students affected
            Text(text = "Students affected", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = Ink)
            Text(text = "Helps prioritize tickets impacting multiple students.", fontSize = 11.5.sp, color = InkMuted)
            Spacer(modifier = Modifier.height(8.dp))

            val affectedOptions = listOf(
                1 to "1 (Just me)",
                3 to "2-5 (Room)",
                12 to "6-20 (Floor)",
                25 to "20+ (Block)"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                affectedOptions.forEach { (count, label) ->
                    val isSelected = when (count) {
                        1 -> appState.formStudentsAffected == 1
                        3 -> appState.formStudentsAffected in 2..5
                        12 -> appState.formStudentsAffected in 6..20
                        else -> appState.formStudentsAffected > 20
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) HazardAmber else SurfaceCard)
                            .border(2.dp, Ink, RoundedCornerShape(8.dp))
                            .clickable { appState.formStudentsAffected = count }
                            .padding(vertical = 10.dp, horizontal = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            color = Ink,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Stepper/Fine-tune Count
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceCard)
                    .border(2.dp, Ink, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Exact count: ${appState.formStudentsAffected} student${if (appState.formStudentsAffected > 1) "s" else ""}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (appState.formStudentsAffected > 1) BgPaper else ConcreteGray.copy(alpha = 0.2f))
                            .border(1.5.dp, Ink, RoundedCornerShape(6.dp))
                            .clickable(enabled = appState.formStudentsAffected > 1) {
                                if (appState.formStudentsAffected > 1) appState.formStudentsAffected--
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease", tint = Ink, modifier = Modifier.size(16.dp))
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(BgPaper)
                            .border(1.5.dp, Ink, RoundedCornerShape(6.dp))
                            .clickable {
                                appState.formStudentsAffected++
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Increase", tint = Ink, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Incident Location Card
            Text(text = "Incident location", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = Ink)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceCard)
                    .border(2.5.dp, Ink, RoundedCornerShape(12.dp))
                    .clickable { onOpenLocationPicker() }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Ink),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text(text = appState.formLocation, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = Ink)
                        Text(text = "Tap to modify building/room", fontSize = 11.sp, color = InkMuted)
                    }
                }
                NeoIconButton(onClick = onOpenLocationPicker, size = 34.dp, shadowOffset = 0.dp) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Location", tint = Ink, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Attachments Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Add photos (optional)", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = Ink)
                Text(text = "Up to 5 photos", fontSize = 11.sp, color = InkMuted)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Add Photo Tile
                item {
                    Column(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceCard)
                            .border(2.dp, Ink, RoundedCornerShape(10.dp))
                            .clickable {
                                if (appState.formPhotos.size < 5) {
                                    appState.formPhotos.add("photo_${appState.formPhotos.size + 1}")
                                }
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Photo", tint = Ink, modifier = Modifier.size(24.dp))
                        Text(text = "Add Photo", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Ink)
                    }
                }

                itemsIndexed(appState.formPhotos) { index, _ ->
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(HazardAmber.copy(alpha = 0.3f))
                            .border(2.dp, Ink, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = Ink, modifier = Modifier.size(30.dp))
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(20.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(AlertRed)
                                .clickable { appState.formPhotos.removeAt(index) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Sticky Bottom Bar with 3dp solid ink top border (.sticky-bottom-bar)
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
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                NeoButton(
                    text = "Review complaint",
                    onClick = onNavigateToReview,
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = HazardAmber
                )
            }
        }
    }
}
