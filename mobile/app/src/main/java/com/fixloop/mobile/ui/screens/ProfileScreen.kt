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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fixloop.mobile.state.SentinelAppState
import com.fixloop.mobile.ui.components.NeoCard
import com.fixloop.mobile.ui.components.NeoIconButton
import com.fixloop.mobile.ui.theme.*

@Composable
fun ProfileScreen(
    appState: SentinelAppState,
    onOpenEditProfile: () -> Unit,
    onOpenDrafts: () -> Unit,
    onOpenNetworkConfig: () -> Unit,
    onOpenLogoutDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val user = appState.user
    val initials = user.name.split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2)
        .joinToString("")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgPaper)
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {
        // Top Title (§7.15)
        Text(
            text          = "Profile",
            fontSize      = 24.sp,
            fontWeight    = FontWeight.ExtraBold,
            letterSpacing = (-0.3).sp,
            color         = Ink,
            modifier      = Modifier.padding(vertical = 10.dp)
        )

        // Profile Card at Top
        NeoCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = SurfaceCard,
            cornerRadius = 16.dp,
            shadowOffset = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Avatar with initials
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(HazardAmber)
                        .border(2.5.dp, Ink, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials.ifBlank { "ST" },
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = Ink
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text          = user.name,
                        fontSize      = 17.sp,
                        fontWeight    = FontWeight.ExtraBold,
                        letterSpacing = (-0.2).sp,
                        color         = Ink
                    )
                    Text(
                        text = "${user.rollNo} • ${user.department}",
                        fontSize = 12.sp,
                        color = InkMuted
                    )
                    Text(
                        text = "${user.hostelBlock}, Room ${user.roomNo}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = IdCardBlue,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                NeoIconButton(
                    onClick = onOpenEditProfile,
                    size = 36.dp,
                    shadowOffset = 0.dp
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Profile", modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Preferences & Records",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Ink,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Menu Items Card
        NeoCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = SurfaceCard,
            cornerRadius = 14.dp
        ) {
            Column {
                ProfileMenuItem(
                    icon = Icons.Default.Person,
                    title = "Edit profile & room",
                    onClick = onOpenEditProfile
                )
                Divider(color = Ink.copy(alpha = 0.1f), thickness = 1.dp)

                ProfileMenuItem(
                    icon = Icons.Default.Drafts,
                    title = "My drafts (${appState.drafts.size})",
                    onClick = onOpenDrafts
                )
                Divider(color = Ink.copy(alpha = 0.1f), thickness = 1.dp)

                ProfileMenuItem(
                    icon = Icons.Default.Wifi,
                    title = "Connection settings",
                    onClick = onOpenNetworkConfig,
                    accentColor = HazardAmber
                )
                Divider(color = Ink.copy(alpha = 0.1f), thickness = 1.dp)

                ProfileMenuItem(
                    icon = Icons.Default.Notifications,
                    title = "Notification preferences",
                    onClick = { }
                )
                Divider(color = Ink.copy(alpha = 0.1f), thickness = 1.dp)

                ProfileMenuItem(
                    icon = Icons.Default.HelpOutline,
                    title = "Help & FAQ",
                    onClick = { }
                )
                Divider(color = Ink.copy(alpha = 0.1f), thickness = 1.dp)

                // Logout Action Item (Alert Red)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenLogoutDialog() }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AlertRed.copy(alpha = 0.15f))
                            .border(1.5.dp, AlertRed, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.ExitToApp, contentDescription = null, tint = AlertRed, modifier = Modifier.size(16.dp))
                    }
                    Text(
                        text = "Log out",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = AlertRed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Campus Policy Footer Note
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Sentinel Student Mobile v1.0.0", fontSize = 12.sp, color = InkMuted, fontWeight = FontWeight.SemiBold)
            Text(text = "Campus Incident & Grievance Protocol", fontSize = 11.sp, color = InkMuted)
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    accentColor: Color = BgPaper
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(accentColor)
                .border(1.5.dp, Ink, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Ink, modifier = Modifier.size(16.dp))
        }

        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Ink,
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Ink,
            modifier = Modifier.size(18.dp)
        )
    }
}
