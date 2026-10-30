package com.fixloop.mobile.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fixloop.mobile.model.CategoryId
import com.fixloop.mobile.state.SentinelAppState
import com.fixloop.mobile.ui.components.*
import com.fixloop.mobile.ui.theme.*

@Composable
fun HomeScreen(
    appState: SentinelAppState,
    onCategoryClick: (CategoryId) -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenDrafts: () -> Unit,
    onComplaintClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterIndex by remember { mutableStateOf(0) }
    val filterPills = listOf("All (8)", "Facilities", "Academic & Tech", "Welfare & Missing")

    val standardCategories = listOf(
        CategoryId.ACADEMIC,
        CategoryId.HOSTEL,
        CategoryId.FOOD,
        CategoryId.HEALTH,
        CategoryId.HYGIENE,
        CategoryId.TECH,
        CategoryId.WELFARE,
        CategoryId.MISSING_ITEMS
    )

    val sensitiveCategories = listOf(
        CategoryId.RAGGING,
        CategoryId.HARASSMENT,
        CategoryId.MALPRACTICE
    )

    val filteredStandard = standardCategories.filter {
        it.displayName.contains(searchQuery, ignoreCase = true)
    }
    val filteredSensitive = sensitiveCategories.filter {
        it.displayName.contains(searchQuery, ignoreCase = true)
    }

    val displayedStandard = filteredStandard.filter { cat ->
        when (selectedFilterIndex) {
            1 -> cat in listOf(CategoryId.HOSTEL, CategoryId.FOOD, CategoryId.HYGIENE)
            2 -> cat in listOf(CategoryId.ACADEMIC, CategoryId.TECH)
            3 -> cat in listOf(CategoryId.WELFARE, CategoryId.MISSING_ITEMS, CategoryId.HEALTH)
            else -> true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgPaper)
    ) {
        // ====================================================================
        // Top App Bar (§7.5)
        // ====================================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home Brand Lockup
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(Ink)
                        .border(2.dp, Ink, RoundedCornerShape(9.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Ink,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
                Text(
                    text          = "SENTINEL",
                    fontSize      = 19.sp,
                    fontWeight    = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    color         = Ink
                )

            }

            // Top Bar Right: Notifications & Drafts
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NeoIconButton(
                    onClick = onOpenNotifications,
                    hasDot = appState.unreadNotificationsCount > 0,
                    size = 44.dp
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = Ink,
                        modifier = Modifier.size(20.dp)
                    )
                }

                NeoIconButton(
                    onClick = onOpenDrafts,
                    hasDot = appState.drafts.isNotEmpty(),
                    size = 44.dp
                ) {
                    Icon(
                        imageVector = Icons.Default.Drafts,
                        contentDescription = "Drafts",
                        tint = Ink,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // ====================================================================
        // Scrollable Home Content
        // ====================================================================
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Headline Block
            item {
                Column(modifier = Modifier.padding(top = 10.dp, bottom = 18.dp)) {
                    Text(
                        text          = "Report it.",
                        fontSize      = 34.sp,
                        fontWeight    = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp,
                        color         = Ink,
                        lineHeight    = 38.sp
                    )
                    Text(
                        text          = "We'll get it fixed.",
                        fontSize      = 34.sp,
                        fontWeight    = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp,
                        color         = Ink,
                        lineHeight    = 38.sp
                    )
                }
            }

            // Home Search Row & Search Button
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Search Bar
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceCard)
                            .border(3.dp, Ink, RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Ink,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = Ink,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            decorationBox = { inner ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search complaints",
                                        color = InkMuted,
                                        fontSize = 14.sp
                                    )
                                }
                                inner()
                            }
                        )
                        if (searchQuery.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = InkMuted,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { searchQuery = "" }
                            )
                        }
                    }

                    // Search Button
                    Box(
                        modifier = Modifier
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(HazardAmber)
                            .border(3.dp, Ink, RoundedCornerShape(16.dp))
                            .clickable { }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Ink,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Search",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Ink
                            )
                        }
                    }
                }
            }

            // Categories Header Label & Fast Filter Pills
            item {
                Column(modifier = Modifier.padding(bottom = 12.dp)) {
                    Text(
                        text          = "Categories",
                        fontSize      = 17.sp,
                        fontWeight    = FontWeight.ExtraBold,
                        letterSpacing = 0.1.sp,
                        color         = Ink,
                        modifier      = Modifier.padding(bottom = 10.dp)
                    )

                    // Interactive Category Filter Pill Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filterPills.forEachIndexed { index, title ->
                            val isSelected = selectedFilterIndex == index
                            val pillBg by animateColorAsState(
                                targetValue = if (isSelected) Ink else SurfaceCard,
                                animationSpec = tween(150),
                                label = "pill_bg_$index"
                            )
                            val pillTextColor by animateColorAsState(
                                targetValue = if (isSelected) Color.White else Ink,
                                animationSpec = tween(150),
                                label = "pill_text_$index"
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(pillBg)
                                    .border(2.dp, Ink, RoundedCornerShape(100.dp))
                                    .clickable { selectedFilterIndex = index }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = title,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = pillTextColor
                                )
                            }
                        }
                    }
                }
            }

            // Standard Categories in 2-Column Pairs
            items(displayedStandard.chunked(2).size) { rowIndex ->
                val pair = displayedStandard.chunked(2)[rowIndex]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    pair.forEach { category ->
                        val openCount = appState.getCategoryOpenTicketsCount(category)
                        NeoCategoryFolderCard(
                            category = category,
                            openCount = openCount,
                            icon = getCategoryIcon(category),
                            onClick = { onCategoryClick(category) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // Confidential Divider Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f).height(2.dp).background(ConcreteGray))
                    Text(
                        text = "Confidential — routed to a restricted team",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = InkMuted,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )
                    Box(modifier = Modifier.weight(1f).height(2.dp).background(ConcreteGray))
                }
            }

            // 3 Sensitive Categories in 3-Column Grid!
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    filteredSensitive.forEach { category ->
                        NeoSensitiveCategoryCard(
                            category = category,
                            icon = getCategoryIcon(category),
                            onClick = { onCategoryClick(category) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}
