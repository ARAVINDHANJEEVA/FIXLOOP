package com.fixloop.mobile.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fixloop.mobile.model.ComplaintStatus
import com.fixloop.mobile.model.TimelineNode
import com.fixloop.mobile.state.SentinelAppState
import com.fixloop.mobile.ui.components.NeoButton
import com.fixloop.mobile.ui.components.NeoCard
import com.fixloop.mobile.ui.components.NeoIconButton
import com.fixloop.mobile.ui.components.NeoStatusChip
import com.fixloop.mobile.ui.theme.*

@Composable
fun ComplaintDetailScreen(
    complaintId: String,
    appState: SentinelAppState,
    onNavigateBack: () -> Unit,
    onOpenReplySheet: () -> Unit,
    onOpenRateSheet: () -> Unit,
    onOpenReopenDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LaunchedEffect(complaintId) {
        appState.loadComplaintDetail(complaintId)
    }

    val complaint = appState.complaints.find { it.id == complaintId }

    if (complaint == null) {
        Box(modifier = modifier.fillMaxSize().background(BgPaper), contentAlignment = Alignment.Center) {
            Text("Complaint not found.", fontWeight = FontWeight.Bold, color = Ink)
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgPaper)
    ) {
        // Top App Bar (§7.10)
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
                NeoIconButton(onClick = onNavigateBack, size = 40.dp) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp))
                }
                Text(
                    text = complaint.id,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = IdCardBlue
                )
            }

            NeoIconButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Complaint Token", complaint.id))
                    appState.triggerToast("Token copied to clipboard.")
                },
                size = 40.dp
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = "Copy / Share", modifier = Modifier.size(18.dp))
            }
        }

        // Category Color Anchor Strip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(complaint.categoryId.color)
        )

        // Main Scrollable Content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Card
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(complaint.categoryId.color))
                            Text(
                                text = complaint.categoryId.displayName.uppercase(),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.5.sp,
                                color = complaint.categoryId.color,
                                letterSpacing = 0.5.sp
                            )
                        }

                        NeoStatusChip(
                            text = complaint.status.label,
                            backgroundColor = complaint.status.color,
                            textColor = complaint.status.textColor
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = complaint.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.3).sp,
                        color = Ink
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(text = complaint.description, fontSize = 14.sp, color = InkMuted, lineHeight = 20.sp)


                    Spacer(modifier = Modifier.height(14.dp))

                    // Metadata Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "SEVERITY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = InkMuted)
                            Text(text = complaint.severity, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ink)
                        }
                        Column {
                            Text(text = "LOCATION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = InkMuted)
                            Text(text = complaint.location, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                        }
                        Column {
                            Text(text = "LOGGED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = InkMuted)
                            Text(text = complaint.createdAt, fontSize = 12.sp, color = InkMuted)
                        }
                    }

                    if (complaint.anonymous) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Ink)
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                            Text(text = "Protected Anonymous Whistleblower Report", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Dispatch / Vendor Card (if assigned)
            if (complaint.dispatch != null) {
                NeoCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = if (complaint.dispatch.type == "vendor") Color(0xFFE8F7F6) else SurfaceCard,
                    cornerRadius = 16.dp,
                    shadowOffset = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (complaint.dispatch.type == "vendor") Color(0xFF1FA8A0) else Ink)
                                .border(1.5.dp, Ink, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (complaint.dispatch.type == "vendor") Icons.Default.LocalShipping else Icons.Default.Build,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(text = complaint.dispatch.text, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Ink)
                            Text(
                                text = complaint.dispatch.subText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (complaint.dispatch.type == "vendor") Color(0xFF137A74) else InkMuted
                            )
                        }
                    }
                }
            }

            // Status Timeline Stepper (.timeline-card)
            NeoCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = SurfaceCard,
                cornerRadius = 16.dp,
                shadowOffset = 4.dp
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text          = "Status timeline",
                        fontSize      = 16.sp,
                        fontWeight    = FontWeight.ExtraBold,
                        letterSpacing = (-0.2).sp,
                        color         = Ink
                    )
                    Spacer(modifier = Modifier.height(18.dp))

                    complaint.timeline.forEachIndexed { index, node ->
                        TimelineNodeItem(
                            node = node,
                            isLast = index == complaint.timeline.lastIndex
                        )
                    }
                }
            }

            // Review / Feedback Card (if rated)
            if (complaint.review != null) {
                NeoCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = SurfaceCard,
                    borderColor = Ink,
                    borderWidth = 2.5.dp,
                    cornerRadius = 14.dp,
                    shadowOffset = 4.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(ClearanceGreen)
                                )
                                Text(
                                    text = "Your Resolution Rating",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Ink
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                repeat(5) { starIndex ->
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = if (starIndex < complaint.review.stars) HazardAmber else ConcreteGray.copy(alpha = 0.35f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                        if (complaint.review.comment.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ClearanceGreen.copy(alpha = 0.1f))
                                    .border(1.5.dp, ClearanceGreen.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "\"${complaint.review.comment}\"",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Ink
                                )
                            }
                        }
                    }
                }
            }

            // Submitted Photos Row
            if (complaint.photos.isNotEmpty()) {
                NeoCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = SurfaceCard,
                    cornerRadius = 14.dp,
                    shadowOffset = 4.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Submitted photos", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ink)
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(complaint.photos) { _ ->
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(HazardAmber.copy(alpha = 0.25f))
                                        .border(2.dp, Ink, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = Ink, modifier = Modifier.size(26.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // ====================================================================
        // Contextual Sticky Bottom Actions Bar (§7.10)
        // ====================================================================
        when {
            // State 1: Awaiting student info -> Orange bar with Reply CTA
            complaint.status == ComplaintStatus.AWAITING_STUDENT_INFO -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFF6F0))
                        .border(width = 2.dp, color = ConeOrange)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Action required", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ConeOrange)
                            Text(text = "Staff needs your response", fontSize = 12.sp, color = Ink)
                        }
                        NeoButton(
                            text = "Reply",
                            onClick = onOpenReplySheet,
                            backgroundColor = ConeOrange,
                            contentColor = Color.White
                        )
                    }
                }
            }

            // State 2: Resolved & Unreviewed -> Green bar with Rate CTA
            complaint.status == ComplaintStatus.RESOLVED && complaint.review == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF3FAF5))
                        .border(width = 2.dp, color = ClearanceGreen)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    NeoButton(
                        text = "Rate this resolution",
                        onClick = onOpenRateSheet,
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = ClearanceGreen,
                        contentColor = Color.White
                    )
                }
            }

            // State 3: Resolved & Reviewed / Closed -> Reopen action
            complaint.status == ComplaintStatus.CLOSED || (complaint.status == ComplaintStatus.RESOLVED && complaint.review != null) -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceCard)
                        .border(width = 2.dp, color = Ink)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    NeoButton(
                        text = "Not satisfied? Reopen complaint",
                        onClick = onOpenReopenDialog,
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = SurfaceCard,
                        contentColor = AlertRed,
                        borderColor = AlertRed
                    )
                }
            }
        }
    }
}

/**
 * Single Node in the Status Timeline Stepper with continuous vertical ink connecting line.
 */
@Composable
fun TimelineNodeItem(
    node: TimelineNode,
    isLast: Boolean
) {
    // Outer pulsating ring animation for active node (.node-circle.current::after)
    val infiniteTransition = rememberInfiniteTransition(label = "pulseRing")
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_scale"
    )
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_alpha"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Vertical Step Indicator + Connecting Line
        Box(
            modifier = Modifier.width(32.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            // Connecting vertical line between nodes (width: 3dp solid ink)
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .padding(top = 22.dp)
                        .width(3.dp)
                        .height(60.dp)
                        .background(Ink)
                )
            }

            // Node Circle
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                if (node.isCurrent) {
                    // Pulsating hazard amber outer ring
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .graphicsLayer {
                                scaleX = ringScale
                                scaleY = ringScale
                                alpha = ringAlpha
                            }
                            .border(2.dp, HazardAmber, CircleShape)
                    )
                }

                // Core Circle
                Box(
                    modifier = Modifier
                        .size(if (node.isCurrent) 22.dp else 18.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                node.isCurrent -> HazardAmber
                                node.isFuture -> SurfaceCard
                                else -> ClearanceGreen
                            }
                        )
                        .border(
                            width = if (node.isFuture) 2.dp else 2.5.dp,
                            color = if (node.isFuture) ConcreteGray else Ink,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (!node.isFuture && !node.isCurrent) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Node Content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 4.dp else 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = node.status.label,
                    fontSize = 13.5.sp,
                    fontWeight = if (node.isCurrent) FontWeight.ExtraBold else FontWeight.Bold,
                    color = if (node.isFuture) InkMuted else Ink
                )
                Text(
                    text = node.time,
                    fontSize = 11.sp,
                    color = InkMuted
                )
            }

            if (node.note != null) {
                Text(
                    text = node.note,
                    fontSize = 12.sp,
                    color = InkMuted,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Staff Speech Bubble Note (.staff-note-bubble)
            if (node.staffNote != null) {
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(BgPaper)
                        .border(2.dp, Ink, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "STAFF NOTE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = IdCardBlue,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = node.staffNote,
                            fontSize = 12.5.sp,
                            color = Ink,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }
    }
}
