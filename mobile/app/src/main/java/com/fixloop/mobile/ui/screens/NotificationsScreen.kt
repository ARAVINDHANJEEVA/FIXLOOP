package com.fixloop.mobile.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fixloop.mobile.model.ComplaintStatus
import com.fixloop.mobile.model.NotificationItem
import com.fixloop.mobile.state.SentinelAppState
import com.fixloop.mobile.ui.components.NeoCard
import com.fixloop.mobile.ui.components.getCategoryIcon
import com.fixloop.mobile.ui.theme.*

@Composable
fun NotificationsScreen(
    appState: SentinelAppState,
    onNotificationClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("ALL") }

    // 1. Deduplicate by issue: exactly ONE card per complaint (no duplicate/recurring cards on the same issue)
    val deduplicatedNotifs = remember(appState.notifications, appState.complaints) {
        val grouped = appState.notifications.groupBy {
            if (it.complaintId.isNotBlank()) it.complaintId else it.id
        }
        grouped.values.map { itemsForIssue ->
            // Prioritize actionable staff inquiry (AWAITING_STUDENT_INFO) if active, otherwise latest update
            val actionItem = itemsForIssue.find { it.status == ComplaintStatus.AWAITING_STUDENT_INFO }
            val latestItem = itemsForIssue.last()
            val rep = actionItem ?: latestItem
            val hasUnread = itemsForIssue.any { it.isUnread }
            rep.copy(isUnread = hasUnread)
        }
    }

    // 2. Sort in First-Come, First-Served (FCFS) order: earliest submitted issues first
    val fcfsSortedNotifs = remember(deduplicatedNotifs, appState.complaints) {
        deduplicatedNotifs.sortedWith { a, b ->
            val compA = appState.complaints.find { it.id == a.complaintId }
            val compB = appState.complaints.find { it.id == b.complaintId }
            val timeA = compA?.timestamp?.ifBlank { null } ?: compA?.createdAt ?: a.time
            val timeB = compB?.timestamp?.ifBlank { null } ?: compB?.createdAt ?: b.time
            timeA.compareTo(timeB)
        }
    }

    val filteredNotifs = remember(fcfsSortedNotifs, selectedFilter) {
        fcfsSortedNotifs.filter {
            when (selectedFilter) {
                "ALL" -> true
                "UNREAD" -> it.isUnread
                "ACTION" -> it.status == ComplaintStatus.AWAITING_STUDENT_INFO || it.status == ComplaintStatus.RESOLVED
                else -> true
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgPaper)
    ) {
        // Top App Bar (§7.14)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgPaper)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = "Alerts", fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.3).sp, color = Ink)


            val unreadCount = appState.unreadNotificationsCount
            if (unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(HazardAmber)
                        .border(1.5.dp, Ink, RoundedCornerShape(100.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "$unreadCount UNREAD",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Ink
                    )
                }
            }
        }

        // Horizontally Scrollable Notification Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val chips = listOf("ALL" to "All Alerts", "UNREAD" to "Unread", "ACTION" to "Action Needed")
            chips.forEach { (key, label) ->
                val isSelected = selectedFilter == key
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(if (isSelected) Ink else SurfaceCard)
                        .border(1.5.dp, Ink, RoundedCornerShape(100.dp))
                        .clickable { selectedFilter = key }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else Ink,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Notifications List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (filteredNotifs.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceCard)
                                .border(2.dp, Ink, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.NotificationsNone, contentDescription = null, tint = Ink, modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(text = "All caught up.", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink)
                        Text(text = "No notifications match the selected filter.", fontSize = 13.sp, color = InkMuted, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }

            if (filteredNotifs.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "FIRST-COME, FIRST-SERVED QUEUE",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = InkMuted,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "${filteredNotifs.size} ${if (filteredNotifs.size == 1) "ISSUE" else "ISSUES"}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = IdCardBlue
                        )
                    }
                }

                itemsIndexed(filteredNotifs, key = { _, item -> item.id }) { index, item ->
                    AlertNotificationCard(
                        item = item,
                        queuePosition = index + 1,
                        appState = appState,
                        onClick = {
                            appState.markNotificationRead(item.id)
                            onNotificationClick(item.complaintId)
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(90.dp))
            }
        }
    }
}

/**
 * Authentic Neo-Brutalism Alert Card with 6px colored left border, "NEW" ribbon badge,
 * inline quick reply for staff inquiries, and inline star rating for resolved complaints.
 */
@Composable
fun AlertNotificationCard(
    item: NotificationItem,
    appState: SentinelAppState,
    onClick: () -> Unit,
    queuePosition: Int = 1
) {
    val ticket = appState.complaints.find { it.id == item.complaintId }
    var quickReplyText by remember { mutableStateOf("") }
    var replySent by remember { mutableStateOf(false) }

    // Left border accent color & background tint based on status
    val (leftBorderColor, cardBg) = when (item.status) {
        ComplaintStatus.AWAITING_STUDENT_INFO -> ConeOrange to Color(0xFFFFFDF8)
        ComplaintStatus.RESOLVED -> ClearanceGreen to Color(0xFFF8FFF9)
        ComplaintStatus.IN_PROGRESS, ComplaintStatus.VENDOR_ASSIGNED -> IdCardBlue to Color(0xFFF9FAFF)
        else -> (if (item.isUnread) IdCardBlue else ConcreteGray) to SurfaceCard
    }

    // Live pulsing dot animation for urgent action tag
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
    ) {
        // Main Neo Card Container
        NeoCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = cardBg,
            cornerRadius = 14.dp,
            shadowOffset = 4.dp,
            onClick = onClick
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                // 6px Solid Colored Left Border (.alert-card.status-*)
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .fillMaxHeight()
                        .background(leftBorderColor)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(14.dp)
                ) {
                    // Top Row: Category Icon Tile + Category Name + Time + Status Badge
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
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(item.categoryId.color)
                                    .border(1.5.dp, Ink, RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getCategoryIcon(item.categoryId),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = item.categoryId.displayName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Ink
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Ink.copy(alpha = 0.08f))
                                            .border(1.dp, Ink.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "FCFS #$queuePosition",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Ink
                                        )
                                    }
                                }
                                Text(
                                    text = item.time,
                                    fontSize = 10.5.sp,
                                    color = InkMuted
                                )
                            }
                        }

                        // Status Badge on the right
                        when (item.status) {
                            ComplaintStatus.AWAITING_STUDENT_INFO -> {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(ConeOrange.copy(alpha = 0.15f))
                                        .border(1.5.dp, ConeOrange, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 7.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(ConeOrange.copy(alpha = dotAlpha))
                                    )
                                    Text(
                                        text = "ACTION REQUIRED",
                                        color = ConeOrange,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                            ComplaintStatus.RESOLVED -> {
                                Text(
                                    text = "★ RESOLUTION VERIFIED",
                                    color = ClearanceGreen,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(ClearanceGreen.copy(alpha = 0.15f))
                                        .border(1.5.dp, ClearanceGreen, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                            else -> {
                                Text(
                                    text = item.status.label.uppercase(),
                                    color = item.status.color,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(item.status.color.copy(alpha = 0.15f))
                                        .border(1.dp, item.status.color, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Headline
                    val headline = when (item.status) {
                        ComplaintStatus.AWAITING_STUDENT_INFO -> "Staff Inquiry Received"
                        ComplaintStatus.RESOLVED -> "Issue Marked Resolved"
                        else -> "System Update"
                    }
                    Text(
                        text = headline,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Quote Snippet in Speech Bubble (.alert-quote-box)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(BgPaper)
                            .border(1.5.dp, Ink, RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = item.text,
                            fontSize = 12.5.sp,
                            color = Ink,
                            lineHeight = 17.sp
                        )
                    }

                    // --- Interactive In-Card Controls ---
                    // 1. Quick Reply for AWAITING_STUDENT_INFO
                    if (item.status == ComplaintStatus.AWAITING_STUDENT_INFO) {
                        Spacer(modifier = Modifier.height(8.dp))
                        if (replySent) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ClearanceGreen.copy(alpha = 0.15f))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = ClearanceGreen, modifier = Modifier.size(16.dp))
                                Text(text = "Reply sent to staff.", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ClearanceGreen)
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SurfaceCard)
                                        .border(1.5.dp, Ink, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    androidx.compose.foundation.text.BasicTextField(
                                        value = quickReplyText,
                                        onValueChange = { quickReplyText = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            color = Ink,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        decorationBox = { innerTextField ->
                                            if (quickReplyText.isEmpty()) {
                                                Text(
                                                    text = "Quick reply to staff...",
                                                    fontSize = 12.5.sp,
                                                    color = InkMuted
                                                )
                                            }
                                            innerTextField()
                                        }
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ConeOrange)
                                        .border(1.5.dp, Ink, RoundedCornerShape(8.dp))
                                        .clickable {
                                            if (quickReplyText.isNotBlank()) {
                                                appState.submitReply(item.complaintId, quickReplyText)
                                                replySent = true
                                                quickReplyText = ""
                                            }
                                        }
                                        .padding(horizontal = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(13.dp))
                                        Text(text = "Send", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // 2. Inline Star Rating for RESOLVED complaints
                    if (item.status == ComplaintStatus.RESOLVED) {
                        Spacer(modifier = Modifier.height(8.dp))
                        if (ticket?.review != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ClearanceGreen.copy(alpha = 0.1f))
                                    .border(1.dp, ClearanceGreen, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "★ Rated ${ticket.review.stars}/5 Stars: ${ticket.review.comment.ifBlank { "Verified" }}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ClearanceGreen
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ClearanceGreen.copy(alpha = 0.08f))
                                    .border(1.5.dp, ClearanceGreen.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "RATE RESOLUTION:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ClearanceGreen
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    (1..5).forEach { star ->
                                        Text(
                                            text = "★",
                                            fontSize = 20.sp,
                                            color = HazardAmber,
                                            modifier = Modifier
                                                .clickable {
                                                    appState.submitReview(item.complaintId, star, "Verified via notification")
                                                }
                                                .padding(2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Action Link Text
                    val actionText = when (item.status) {
                        ComplaintStatus.AWAITING_STUDENT_INFO -> "Reply to Staff →"
                        ComplaintStatus.RESOLVED -> if (ticket?.review != null) "View Record →" else "Rate Resolution ★"
                        else -> "View Details →"
                    }
                    Text(
                        text = actionText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = IdCardBlue,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }

        // "NEW" Ribbon Badge at Top-Right on Unread Alerts (.alert-new-badge)
        if (item.isUnread) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-14).dp, y = (-5).dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(HazardAmber)
                    .border(1.5.dp, Ink, RoundedCornerShape(4.dp))
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(AlertRed)
                    )
                    Text(
                        text = "NEW",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Ink,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}
