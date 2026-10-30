package com.fixloop.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fixloop.mobile.model.Complaint
import com.fixloop.mobile.model.ComplaintStatus
import com.fixloop.mobile.state.SentinelAppState
import com.fixloop.mobile.ui.components.*
import com.fixloop.mobile.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MyComplaintsScreen(
    appState: SentinelAppState,
    onComplaintClick: (String) -> Unit,
    onQuickReplyClick: (String) -> Unit,
    onQuickRateClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val filterOptions = listOf(
        "ALL" to "All",
        "SUBMITTED" to "Submitted",
        "IN_PROGRESS" to "In progress",
        "NEEDS_YOU" to "Needs you",
        "RESOLVED" to "Resolved",
        "CLOSED" to "Closed"
    )

    val filteredComplaints = appState.complaints.filter { complaint ->
        val matchesFilter = when (selectedFilter) {
            "ALL" -> true
            "SUBMITTED" -> complaint.status == ComplaintStatus.SUBMITTED || complaint.status == ComplaintStatus.UNDER_ADMIN_REVIEW
            "IN_PROGRESS" -> complaint.status == ComplaintStatus.IN_PROGRESS || complaint.status == ComplaintStatus.ASSIGNED || complaint.status == ComplaintStatus.VENDOR_ASSIGNED
            "NEEDS_YOU" -> complaint.status == ComplaintStatus.AWAITING_STUDENT_INFO
            "RESOLVED" -> complaint.status == ComplaintStatus.RESOLVED
            "CLOSED" -> complaint.status == ComplaintStatus.CLOSED
            else -> true
        }

        val matchesSearch = if (searchQuery.isBlank()) true else {
            complaint.title.contains(searchQuery, ignoreCase = true) ||
            complaint.id.contains(searchQuery, ignoreCase = true) ||
            complaint.description.contains(searchQuery, ignoreCase = true) ||
            complaint.location.contains(searchQuery, ignoreCase = true)
        }

        matchesFilter && matchesSearch
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgPaper)
    ) {
        // ====================================================================
        // Top App Bar (§7.9)
        // ====================================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!searchOpen) {
                Text(
                    text          = "My Complaints",
                    fontSize      = 24.sp,
                    fontWeight    = FontWeight.ExtraBold,
                    letterSpacing = (-0.3).sp,
                    color         = Ink
                )

                NeoIconButton(
                    onClick = { searchOpen = true },
                    size = 44.dp
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Ink,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceCard)
                            .border(2.5.dp, Ink, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = InkMuted, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = Ink,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            decorationBox = { inner ->
                                if (searchQuery.isEmpty()) Text("Search complaints", color = InkMuted, fontSize = 13.sp)
                                inner()
                            }
                        )
                    }
                    NeoIconButton(
                        onClick = {
                            searchQuery = ""
                            searchOpen = false
                        },
                        size = 44.dp
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Ink, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // Pull to refresh simulated block loader
        if (isRefreshing) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                SegmentedLoader(blockCount = 5, blockWidth = 14.dp, blockHeight = 8.dp)
            }
        }

        // Horizontally Scrollable Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filterOptions.forEach { (key, label) ->
                val isSelected = selectedFilter == key
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(if (isSelected) Ink else SurfaceCard)
                        .border(2.dp, Ink, RoundedCornerShape(100.dp))
                        .clickable {
                            selectedFilter = key
                            coroutineScope.launch {
                                isRefreshing = true
                                delay(350)
                                isRefreshing = false
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else Ink,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Complaints List
        if (filteredComplaints.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(HazardAmber)
                            .border(3.dp, Ink, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.ConfirmationNumber, contentDescription = null, tint = Ink, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "No complaints found.",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink
                    )
                    Text(
                        text = "Try clearing search or switching filter tabs.",
                        fontSize = 13.sp,
                        color = InkMuted,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(filteredComplaints, key = { it.id }) { complaint ->
                    ComplaintTicketCard(
                        complaint = complaint,
                        onClick = { onComplaintClick(complaint.id) },
                        onReplyClick = { onQuickReplyClick(complaint.id) },
                        onRateClick = { onQuickRateClick(complaint.id) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(90.dp))
                }
            }
        }
    }
}

/**
 * Authentic Ticket Card with Bookmark Ribbon hanging from the top right!
 */
@Composable
fun ComplaintTicketCard(
    complaint: Complaint,
    onClick: () -> Unit,
    onReplyClick: () -> Unit,
    onRateClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
    ) {
        // Main Neo-Card
        NeoCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = SurfaceCard,
            cornerRadius = 16.dp,
            shadowOffset = 4.dp,
            onClick = onClick
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Top Row: Category Icon Tile + Category Name + Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(complaint.categoryId.color)
                            .border(2.5.dp, Ink, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(complaint.categoryId),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = complaint.categoryId.displayName,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Ink
                        )
                        Text(
                            text = " · ${complaint.createdAt}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = InkMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Complaint title
                Text(
                    text = complaint.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                    lineHeight = 21.sp,
                    modifier = Modifier.padding(end = 110.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Token badge — monospace, clearly a code
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(BgPaper)
                        .border(1.5.dp, Ink.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = complaint.id,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkMuted,
                        letterSpacing = 0.3.sp
                    )
                }


                // Urgent Callout for Awaiting Info
                if (complaint.status == ComplaintStatus.AWAITING_STUDENT_INFO) {
                    val staffNote = complaint.timeline.find { it.status == ComplaintStatus.AWAITING_STUDENT_INFO }?.staffNote
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(ConeOrange.copy(alpha = 0.12f))
                            .border(2.dp, ConeOrange, RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "STAFF REQUESTED INFO",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ConeOrange
                            )
                            Text(
                                text = staffNote ?: "Additional information required.",
                                fontSize = 12.sp,
                                color = Ink,
                                lineHeight = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(ConeOrange)
                                .border(1.5.dp, Ink, RoundedCornerShape(8.dp))
                                .clickable { onReplyClick() }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Reply",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Rate Prompt if Resolved & Unrated
                if (complaint.status == ComplaintStatus.RESOLVED && complaint.review == null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(ClearanceGreen.copy(alpha = 0.12f))
                            .border(2.dp, ClearanceGreen, RoundedCornerShape(10.dp))
                            .clickable { onRateClick() }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Marked resolved — Tap to rate quality",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ClearanceGreen
                        )
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = ClearanceGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Footer Row: Location
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = InkMuted, modifier = Modifier.size(14.dp))
                        Text(text = complaint.location, fontSize = 12.sp, color = InkMuted, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // Hanging Bookmark Ribbon Tag on Top Right
        TicketRibbonTag(
            status = complaint.status,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-18).dp, y = (-6).dp)
        )
    }
}
