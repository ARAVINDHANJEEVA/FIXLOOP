package com.fixloop.mobile.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.fixloop.mobile.data.local.SessionManager
import com.fixloop.mobile.model.Draft
import com.fixloop.mobile.state.SentinelAppState
import com.fixloop.mobile.ui.components.NeoButton
import com.fixloop.mobile.ui.components.NeoCard
import com.fixloop.mobile.ui.components.NeoIconButton
import com.fixloop.mobile.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReplyBottomSheet(
    complaintId: String,
    appState: SentinelAppState,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    var replyText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(text = "Add more information", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text(
                text = "Staff requested additional details to proceed with this complaint.",
                fontSize = 12.sp,
                color = InkMuted,
                modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
            )

            Text(text = "Your message", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = replyText,
                onValueChange = { replyText = it },
                placeholder = { Text("Explain details or answer staff question...", fontSize = 13.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .border(2.dp, Ink, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = BgPaper,
                    unfocusedContainerColor = BgPaper,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(18.dp))

            NeoButton(
                text = "Send information",
                onClick = {
                    if (replyText.isNotBlank()) {
                        appState.submitReply(complaintId, replyText)
                        onSubmit()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = HazardAmber,
                enabled = replyText.isNotBlank()
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RateBottomSheet(
    complaintId: String,
    appState: SentinelAppState,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    var selectedStars by remember { mutableStateOf(5) }
    var commentText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "How was this resolved?", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text(
                text = "Your feedback helps maintain campus maintenance standards.",
                fontSize = 12.sp,
                color = InkMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
            )

            // 5 Blocky Neo-Brutalist Stars (§7.12)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 18.dp)
            ) {
                for (star in 1..5) {
                    val isFilled = star <= selectedStars
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isFilled) HazardAmber else BgPaper)
                            .border(2.dp, Ink, RoundedCornerShape(8.dp))
                            .clickable { selectedStars = star },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Star $star",
                            tint = if (isFilled) Ink else InkMuted,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Feedback Note
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = "Feedback note (optional)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ink)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = { Text("Was the issue fixed properly? Any remarks for staff...", fontSize = 13.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .border(2.dp, Ink, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = BgPaper,
                        unfocusedContainerColor = BgPaper,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    )
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            NeoButton(
                text = "Submit review",
                onClick = {
                    appState.submitReview(complaintId, selectedStars, commentText)
                    onSubmit()
                },
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = HazardAmber
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ReopenDialog(
    complaintId: String,
    appState: SentinelAppState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        NeoCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = SurfaceCard,
            cornerRadius = 16.dp,
            shadowOffset = 5.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(text = "Reopen this complaint?", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "It'll go back into the queue and staff will be notified that the issue was not fully resolved.",
                    fontSize = 13.sp,
                    color = InkMuted,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NeoButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        backgroundColor = SurfaceCard
                    )
                    NeoButton(
                        text = "Reopen",
                        onClick = {
                            appState.reopenComplaint(complaintId)
                            onConfirm()
                        },
                        modifier = Modifier.weight(1f),
                        backgroundColor = AlertRed,
                        contentColor = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun LogoutDialog(
    appState: SentinelAppState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        NeoCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = SurfaceCard,
            cornerRadius = 16.dp,
            shadowOffset = 5.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(text = "Log out of Sentinel?", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "You will need to sign in again with your college credentials to submit or track tickets.",
                    fontSize = 13.sp,
                    color = InkMuted,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NeoButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        backgroundColor = SurfaceCard
                    )
                    NeoButton(
                        text = "Log out",
                        onClick = {
                            appState.logout()
                            onConfirm()
                        },
                        modifier = Modifier.weight(1f),
                        backgroundColor = AlertRed,
                        contentColor = Color.White
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerBottomSheet(
    appState: SentinelAppState,
    onDismiss: () -> Unit
) {
    val category = appState.formCategory
    val buildings = remember(category) {
        when (category) {
            com.fixloop.mobile.model.CategoryId.HOSTEL,
            com.fixloop.mobile.model.CategoryId.HYGIENE -> listOf(
                "Hostel Block A (Raman)",
                "Hostel Block B (Bhabha)",
                "Hostel Block C (Aryabhata)",
                "Hostel Block D (Kalpana)",
                "Hostel Block E (Visvesvaraya)",
                "PG & Research Scholars Hostel",
                "Other Hostel Building (Specify below)"
            )
            com.fixloop.mobile.model.CategoryId.ACADEMIC,
            com.fixloop.mobile.model.CategoryId.MALPRACTICE -> listOf(
                "Academic Block 1 (Main & Admin)",
                "Academic Block 2 (CSE / IT)",
                "Academic Block 3 (Mech / Civil)",
                "Academic Block 4 (ECE / EEE)",
                "Central Science & Biotech Block",
                "Central Library & Media Wing",
                "Examination Hall Complex",
                "Other Academic Building (Specify below)"
            )
            com.fixloop.mobile.model.CategoryId.FOOD -> listOf(
                "Central Dining Hall / Mess (Ground Floor)",
                "Central Dining Hall / Mess (First Floor)",
                "Annapurna Mess (Hostel Zone)",
                "North Food Court",
                "South Food Court",
                "Night Canteen (Hostel C)",
                "Campus Bakery & Juice Bar",
                "Other Canteen / Mess (Specify below)"
            )
            com.fixloop.mobile.model.CategoryId.HEALTH -> listOf(
                "Campus Health Center (OPD & Pharmacy)",
                "Health Center Inpatient Ward",
                "Ambulance Bay & Emergency Clinic",
                "Dental & Physiotherapy Unit",
                "Other Health Facility (Specify below)"
            )
            com.fixloop.mobile.model.CategoryId.TECH -> listOf(
                "Academic Block 2 (CSE / IT Labs)",
                "Central Computer Center (CCC)",
                "Server & Networking Operations Room",
                "Central Library Wi-Fi Zone",
                "Hostel Network Point",
                "Other Tech Facility (Specify below)"
            )
            else -> listOf(
                "Student Activity Center (SAC)",
                "Central Library & Reading Hall",
                "Sports Complex & Gymnasium",
                "Hostel Quadrangle & Common Grounds",
                "Academic Walkway & Campus Main Gate",
                "Campus Auditorium & Seminar Hall",
                "Other Location (Specify below)"
            )
        }
    }

    val floors = listOf("Ground Floor", "1st Floor", "2nd Floor", "3rd Floor", "4th Floor", "Basement", "Open Area / Outdoor")

    var selectedBuilding by remember(category) {
        mutableStateOf(buildings.firstOrNull() ?: "Campus Building")
    }
    var customBuildingText by remember { mutableStateOf("") }
    var selectedFloor by remember { mutableStateOf(floors[0]) }
    var roomText by remember { mutableStateOf("") }

    var showBuildingMenu by remember { mutableStateOf(false) }
    var showFloorMenu by remember { mutableStateOf(false) }

    val isCustomBuilding = selectedBuilding.contains("Other", ignoreCase = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(text = "Select location", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text(
                text = "${category.displayName} location options",
                fontSize = 12.sp,
                color = InkMuted,
                modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
            )

            // Building Dropdown
            Text(text = "Campus Building / Facility", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(BgPaper)
                        .border(1.5.dp, Ink, RoundedCornerShape(10.dp))
                        .clickable { showBuildingMenu = true }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = selectedBuilding, fontSize = 13.sp, color = Ink)
                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = Ink)
                }
                DropdownMenu(expanded = showBuildingMenu, onDismissRequest = { showBuildingMenu = false }) {
                    buildings.forEach { b ->
                        DropdownMenuItem(
                            text = { Text(b) },
                            onClick = {
                                selectedBuilding = b
                                showBuildingMenu = false
                            }
                        )
                    }
                }
            }

            // Custom Building Input if "Other" is chosen
            if (isCustomBuilding) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Specify Custom Building / Facility Name", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ink)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = customBuildingText,
                    onValueChange = { customBuildingText = it },
                    placeholder = { Text("e.g. Mechanical Workshop, Biotech Annex...", fontSize = 13.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, Ink, RoundedCornerShape(10.dp)),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = BgPaper,
                        unfocusedContainerColor = BgPaper,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Floor
            Text(text = "Floor / Level", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(BgPaper)
                        .border(1.5.dp, Ink, RoundedCornerShape(10.dp))
                        .clickable { showFloorMenu = true }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = selectedFloor, fontSize = 13.sp, color = Ink)
                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = Ink)
                }
                DropdownMenu(expanded = showFloorMenu, onDismissRequest = { showFloorMenu = false }) {
                    floors.forEach { f ->
                        DropdownMenuItem(
                            text = { Text(f) },
                            onClick = {
                                selectedFloor = f
                                showFloorMenu = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Room / Hall / Spot
            Text(text = "Room / Hall / Spot Number", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = roomText,
                onValueChange = { roomText = it },
                placeholder = { Text("e.g. Room 214, Lab 3, Dining Counter 2...", fontSize = 13.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, Ink, RoundedCornerShape(10.dp)),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = BgPaper,
                    unfocusedContainerColor = BgPaper,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(18.dp))

            NeoButton(
                text = "Save location",
                onClick = {
                    val finalBuilding = if (isCustomBuilding && customBuildingText.isNotBlank()) {
                        customBuildingText.trim()
                    } else {
                        selectedBuilding
                    }
                    val finalRoom = roomText.ifBlank { "General Area" }
                    appState.updateLocation(finalBuilding, selectedFloor, finalRoom)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = HazardAmber
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DraftsBottomSheet(
    appState: SentinelAppState,
    onResumeDraft: (Draft) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(text = "Saved Drafts", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text(text = "Unsent reports stored in your offline device storage.", fontSize = 12.sp, color = InkMuted, modifier = Modifier.padding(top = 2.dp, bottom = 14.dp))

            if (appState.drafts.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(text = "No saved drafts.", color = InkMuted, fontSize = 13.sp)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(appState.drafts, key = { it.id }) { draft ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(BgPaper)
                                .border(1.5.dp, Ink, RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = draft.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ink)
                                Text(text = "${draft.categoryId.displayName} • ${draft.updatedAt}", fontSize = 11.sp, color = InkMuted)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(HazardAmber)
                                        .border(1.dp, Ink, RoundedCornerShape(6.dp))
                                        .clickable {
                                            onResumeDraft(draft)
                                            onDismiss()
                                        }
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Text(text = "Resume", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Ink)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(AlertRed)
                                        .clickable { appState.deleteDraft(draft.id) }
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(15.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            NeoButton(
                text = "Close",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = SurfaceCard
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileBottomSheet(
    appState: SentinelAppState,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(appState.user.name) }
    var hostel by remember { mutableStateOf(appState.user.hostelBlock) }
    var room by remember { mutableStateOf(appState.user.roomNo) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(text = "Edit Profile & Room", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(modifier = Modifier.height(14.dp))

            Text(text = "Full name", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth().border(1.5.dp, Ink, RoundedCornerShape(10.dp)),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = BgPaper, unfocusedContainerColor = BgPaper, focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(text = "Hostel block", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = hostel,
                onValueChange = { hostel = it },
                modifier = Modifier.fillMaxWidth().border(1.5.dp, Ink, RoundedCornerShape(10.dp)),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = BgPaper, unfocusedContainerColor = BgPaper, focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(text = "Room number", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = room,
                onValueChange = { room = it },
                modifier = Modifier.fillMaxWidth().border(1.5.dp, Ink, RoundedCornerShape(10.dp)),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = BgPaper, unfocusedContainerColor = BgPaper, focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(18.dp))

            NeoButton(
                text = "Save profile",
                onClick = {
                    appState.updateUserProfile(name, hostel, room)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = HazardAmber
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// =============================================================================
// Network / Connection Config Bottom Sheet
// Allows switching between: Emulator | USB | Hotspot (Wi-Fi) | mDNS | Custom
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkConfigBottomSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    var selectedMode by remember { mutableStateOf(sessionManager.getConnectionMode()) }
    var hotspotIpText by remember { mutableStateOf(sessionManager.getHotspotIp()) }
    var customUrlText by remember { mutableStateOf(sessionManager.getCustomUrl()) }

    val modes = listOf(
        Triple(SessionManager.ConnectionMode.EMULATOR, Icons.Default.PhoneAndroid, "Emulator"),
        Triple(SessionManager.ConnectionMode.USB, Icons.Default.Usb, "USB Cable"),
        Triple(SessionManager.ConnectionMode.HOTSPOT, Icons.Default.Wifi, "Wi-Fi Hotspot"),
        Triple(SessionManager.ConnectionMode.MDNS, Icons.Default.Router, "fixloop.local"),
        Triple(SessionManager.ConnectionMode.CUSTOM, Icons.Default.Edit, "Custom URL")
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(HazardAmber)
                        .border(1.5.dp, Ink, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.SettingsEthernet, contentDescription = null, tint = Ink, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Connection Settings", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                    Text("How the app reaches your backend", fontSize = 11.sp, color = InkMuted)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ---- Mode Selector ----
            Text("Select connection mode", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(modifier = Modifier.height(8.dp))

            modes.forEach { (mode, icon, label) ->
                val isSelected = selectedMode == mode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) HazardAmber else BgPaper)
                        .border(
                            width = if (isSelected) 2.dp else 1.5.dp,
                            color = Ink,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { selectedMode = mode }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(icon, contentDescription = null, tint = Ink, modifier = Modifier.size(18.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink)
                        val subtitle = when (mode) {
                            SessionManager.ConnectionMode.EMULATOR -> "Android Studio emulator → 10.0.2.2:8080"
                            SessionManager.ConnectionMode.USB      -> "USB cable + adb reverse → 127.0.0.1:8080"
                            SessionManager.ConnectionMode.HOTSPOT  -> "Phone is hotspot, laptop is client"
                            SessionManager.ConnectionMode.MDNS     -> "IP-resilient — survives IP changes automatically"
                            SessionManager.ConnectionMode.CUSTOM   -> "Enter any URL manually"
                        }
                        Text(subtitle, fontSize = 10.sp, color = if (isSelected) Ink.copy(0.7f) else InkMuted, lineHeight = 13.sp)
                    }
                    if (isSelected) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Ink, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ---- Mode-specific configuration panels ----
            when (selectedMode) {
                SessionManager.ConnectionMode.HOTSPOT -> {
                    // Version 1: Wi-Fi / LAN — user enters laptop IP
                    NeoCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = BgPaper,
                        cornerRadius = 10.dp,
                        shadowOffset = 3.dp
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Wifi, contentDescription = null, tint = IdCardBlue, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Version 1 — Wi-Fi Hotspot", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = IdCardBlue)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Enter your laptop's IP on this hotspot network.\n" +
                                "On laptop: open CMD → type  ipconfig  → look for\n" +
                                "\"Wireless LAN\" section → copy the IPv4 address.",
                                fontSize = 10.sp,
                                color = InkMuted,
                                lineHeight = 14.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Laptop IP address", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ink)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = hotspotIpText,
                                onValueChange = { hotspotIpText = it },
                                placeholder = { Text("e.g. 192.168.43.100", fontSize = 13.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(2.dp, Ink, RoundedCornerShape(10.dp)),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = SurfaceCard,
                                    unfocusedContainerColor = SurfaceCard,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Typical Android hotspot assigns: 192.168.43.x",
                                fontSize = 9.sp,
                                color = InkMuted
                            )
                        }
                    }
                }

                SessionManager.ConnectionMode.MDNS -> {
                    // Version 2: mDNS — IP-resilient, zero-config after setup
                    NeoCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = BgPaper,
                        cornerRadius = 10.dp,
                        shadowOffset = 3.dp
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Router, contentDescription = null, tint = ClearanceGreen, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Version 2 — IP-Resilient (mDNS)", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = ClearanceGreen)
                            }
                            Spacer(modifier = Modifier.height(6.dp))

                            // URL display chip
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ClearanceGreen.copy(alpha = 0.12f))
                                    .border(1.dp, ClearanceGreen, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Link, contentDescription = null, tint = ClearanceGreen, modifier = Modifier.size(12.dp))
                                Text("http://fixloop.local:8080/", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = ClearanceGreen)
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Laptop publishes itself as fixloop.local via mDNS.\n" +
                                "The app finds the laptop even when its IP changes.\n\n" +
                                "One-time setup on laptop:\n" +
                                "  1. Run  start-mdns.bat  (in the project root)\n" +
                                "  2. Keep the terminal window open while testing",
                                fontSize = 10.sp,
                                color = InkMuted,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }

                SessionManager.ConnectionMode.CUSTOM -> {
                    Text("Custom base URL", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = customUrlText,
                        onValueChange = { customUrlText = it },
                        placeholder = { Text("http://192.168.43.100:8080/", fontSize = 13.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, Ink, RoundedCornerShape(10.dp)),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = BgPaper,
                            unfocusedContainerColor = BgPaper,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                }

                SessionManager.ConnectionMode.EMULATOR -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(IdCardBlue.copy(alpha = 0.08f))
                            .border(1.dp, IdCardBlue.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = IdCardBlue, modifier = Modifier.size(14.dp))
                        Text("Will connect to http://10.0.2.2:8080/ (Android Studio emulator only)", fontSize = 10.sp, color = IdCardBlue, lineHeight = 14.sp)
                    }
                }

                SessionManager.ConnectionMode.USB -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(IdCardBlue.copy(alpha = 0.08f))
                            .border(1.dp, IdCardBlue.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = IdCardBlue, modifier = Modifier.size(14.dp))
                        Text("Requires: run  adb reverse tcp:8080 tcp:8080  on your PC while phone is plugged in via USB.", fontSize = 10.sp, color = IdCardBlue, lineHeight = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ---- Save button ----
            NeoButton(
                text = "Save & Apply",
                onClick = {
                    when (selectedMode) {
                        SessionManager.ConnectionMode.HOTSPOT -> sessionManager.setHotspotIp(hotspotIpText)
                        SessionManager.ConnectionMode.CUSTOM  -> sessionManager.setBaseUrl(customUrlText)
                        else -> { /* URL derived from mode constant */ }
                    }
                    sessionManager.setConnectionMode(selectedMode)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = HazardAmber
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
