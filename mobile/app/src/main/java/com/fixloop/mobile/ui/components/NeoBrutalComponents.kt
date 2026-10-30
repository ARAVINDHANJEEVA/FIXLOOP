package com.fixloop.mobile.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fixloop.mobile.model.CategoryId
import com.fixloop.mobile.model.ComplaintStatus
import com.fixloop.mobile.ui.theme.*

/**
 * Physical Neo-Brutalist Card with 3dp solid Ink border and hard 4dp offset shadow.
 */
@Composable
fun NeoCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = SurfaceCard,
    borderColor: Color = Ink,
    borderWidth: Dp = 3.dp,
    cornerRadius: Dp = 16.dp,
    shadowOffset: Dp = 4.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressOffset by animateDpAsState(
        targetValue = if (isPressed && onClick != null) shadowOffset else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessHigh
        ),
        label = "card_press"
    )

    Box(
        modifier = modifier
            .padding(bottom = shadowOffset, end = shadowOffset)
            .drawBehind {
                if (shadowOffset > 0.dp) {
                    drawRoundRect(
                        color = Ink,
                        topLeft = androidx.compose.ui.geometry.Offset(shadowOffset.toPx(), shadowOffset.toPx()),
                        size = size,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
                    )
                }
            }
            .offset(x = pressOffset, y = pressOffset)
            .clip(RoundedCornerShape(cornerRadius))
            .background(SurfaceCard)
            .background(backgroundColor)
            .border(borderWidth, borderColor, RoundedCornerShape(cornerRadius))
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
                } else Modifier
            ),
        content = content
    )
}

/**
 * Authentic Interactive Neo-Brutalism Button with physical depression on press.
 */
@Composable
fun NeoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = HazardAmber,
    contentColor: Color = Ink,
    borderColor: Color = Ink,
    borderWidth: Dp = 3.dp,
    cornerRadius: Dp = 12.dp,
    shadowOffset: Dp = 4.dp,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressOffset by animateDpAsState(
        targetValue = if (isPressed && enabled) shadowOffset else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessHigh
        ),
        label = "btn_press"
    )

    Box(
        modifier = modifier
            .padding(bottom = shadowOffset, end = shadowOffset)
            .drawBehind {
                if (enabled && shadowOffset > 0.dp) {
                    drawRoundRect(
                        color = Ink,
                        topLeft = androidx.compose.ui.geometry.Offset(shadowOffset.toPx(), shadowOffset.toPx()),
                        size = size,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
                    )
                }
            }
            .offset(x = pressOffset, y = pressOffset)
            .defaultMinSize(minHeight = 52.dp)
            .clip(RoundedCornerShape(cornerRadius))
            .background(if (enabled) backgroundColor else ConcreteGray.copy(alpha = 0.35f))
            .border(borderWidth, if (enabled) borderColor else ConcreteGray, RoundedCornerShape(cornerRadius))
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                icon()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text          = text,
                color         = if (enabled) contentColor else InkMuted,
                fontWeight    = FontWeight.ExtraBold,
                fontSize      = 15.sp,
                letterSpacing = 0.3.sp
            )

        }
    }
}

/**
 * Square Icon Button (§7.5, §7.10) with 3dp Ink Border and 2px Hard Shadow.
 */
@Composable
fun NeoIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = SurfaceCard,
    iconTint: Color = Ink,
    shadowOffset: Dp = 2.5.dp,
    size: Dp = 44.dp,
    badgeCount: Int? = null,
    hasDot: Boolean = false,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressOffset by animateDpAsState(
        targetValue = if (isPressed) shadowOffset else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessHigh
        ),
        label = "icon_btn_press"
    )

    Box(
        modifier = modifier.padding(bottom = shadowOffset, end = shadowOffset)
    ) {
        // Main Button Surface
        Box(
            modifier = Modifier
                .drawBehind {
                    if (shadowOffset > 0.dp) {
                        drawRoundRect(
                            color = Ink,
                            topLeft = androidx.compose.ui.geometry.Offset(shadowOffset.toPx(), shadowOffset.toPx()),
                            size = androidx.compose.ui.geometry.Size(size.toPx(), size.toPx()),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx(), 12.dp.toPx())
                        )
                    }
                }
                .offset(x = pressOffset, y = pressOffset)
                .size(size)
                .clip(RoundedCornerShape(12.dp))
                .background(backgroundColor)
                .border(2.5.dp, Ink, RoundedCornerShape(12.dp))
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(LocalContentColor provides iconTint) {
                content()
            }
        }

        // Intimation Red Dot Badge (Unclipped!)
        if (hasDot || (badgeCount != null && badgeCount > 0)) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(AlertRed)
                    .border(2.dp, Color.White, CircleShape)
            )
        }
    }
}

/**
 * Authentic Category Folder Card with Protruding Physical Folder Tab!
 * Replicates `.category-folder-wrapper`, `.folder-tab`, and `.category-card` with elevated tactile aesthetics.
 */
@Composable
fun NeoCategoryFolderCard(
    category: CategoryId,
    openCount: Int,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressOffset by animateDpAsState(
        targetValue = if (isPressed) 4.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessHigh
        ),
        label = "cat_press"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp, end = 4.dp)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        // Stationary hard shadow behind the card body
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 132.dp)
                .offset(x = 4.dp, y = 4.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Ink)
        )

        // Physical moving folder unit (Tab + Body move together on tactile press)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(x = pressOffset, y = pressOffset)
        ) {
            // 1. Protruding Physical Folder Tab with grip slot
            Box(
                modifier = Modifier
                    .offset(x = 10.dp, y = (-12).dp)
                    .width(70.dp)
                    .height(17.dp)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .background(category.color)
                    .border(3.dp, Ink, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Subtle engraved grip groove
                Box(
                    modifier = Modifier
                        .size(width = 24.dp, height = 2.5.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(Ink.copy(alpha = 0.25f))
                )
            }

            // 2. Main Card Body
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 132.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceCard)
                    .border(3.dp, Ink, RoundedCornerShape(16.dp))
            ) {
                // Left category color accent strip
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(4.5.dp)
                        .fillMaxHeight()
                        .background(category.color)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Row: Icon tile + Status Pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        // Category Icon Tile with layered drop shadow
                        Box(
                            modifier = Modifier
                                .drawBehind {
                                    drawRoundRect(
                                        color = Ink,
                                        topLeft = androidx.compose.ui.geometry.Offset(2.dp.toPx(), 2.dp.toPx()),
                                        size = size,
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx())
                                    )
                                }
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(category.color)
                                .border(2.5.dp, Ink, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = category.displayName,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Open count or All Clear pill badge
                        if (openCount > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(category.color.copy(alpha = 0.14f))
                                    .border(1.5.dp, category.color, RoundedCornerShape(100.dp))
                                    .padding(horizontal = 7.dp, vertical = 2.5.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(category.color)
                                    )
                                    Text(
                                        text = "$openCount open",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = category.color
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(ClearanceGreen.copy(alpha = 0.10f))
                                    .border(1.dp, ClearanceGreen.copy(alpha = 0.4f), RoundedCornerShape(100.dp))
                                    .padding(horizontal = 7.dp, vertical = 2.5.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(ClearanceGreen)
                                    )
                                    Text(
                                        text = "All clear",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ClearanceGreen
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Category Title
                    Text(
                        text = category.displayName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.3).sp,
                        color = Ink,
                        lineHeight = 18.sp,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Subtitle & Action Indicator Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = getCategoryTagline(category),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = InkMuted,
                            maxLines = 1,
                            modifier = Modifier.weight(1f).padding(end = 4.dp)
                        )

                        // Tactile Mini Arrow Launcher Disc
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(category.color.copy(alpha = 0.12f))
                                .border(1.5.dp, Ink, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Ink,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Sensitive Category Card (for the 3-column confidential grid).
 */
@Composable
fun NeoSensitiveCategoryCard(
    category: CategoryId,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressOffset by animateDpAsState(
        targetValue = if (isPressed) 3.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessHigh
        ),
        label = "sens_press"
    )

    Box(
        modifier = modifier
            .padding(top = 10.dp, bottom = 3.dp, end = 3.dp)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        // Stationary shadow behind card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 100.dp)
                .offset(x = 3.dp, y = 3.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Ink)
        )

        // Physical moving folder unit (Tab + Body move TOGETHER on press!)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(x = pressOffset, y = pressOffset)
        ) {
            // Tab with engraved slot
            Box(
                modifier = Modifier
                    .offset(x = 8.dp, y = (-10).dp)
                    .width(48.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                    .background(category.color)
                    .border(2.5.dp, Ink, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 16.dp, height = 2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(Ink.copy(alpha = 0.25f))
                )
            }

            // Card Body
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 100.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceCard)
                    .border(2.5.dp, Ink, RoundedCornerShape(14.dp))
            ) {
                // Left accent strip
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(3.5.dp)
                        .fillMaxHeight()
                        .background(category.color)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        // Icon Tile with shadow
                        Box(
                            modifier = Modifier
                                .drawBehind {
                                    drawRoundRect(
                                        color = Ink,
                                        topLeft = androidx.compose.ui.geometry.Offset(1.5.dp.toPx(), 1.5.dp.toPx()),
                                        size = size,
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(9.dp.toPx())
                                    )
                                }
                                .size(36.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(category.color)
                                .border(2.dp, Ink, RoundedCornerShape(9.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Confidential",
                            tint = InkMuted,
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text          = category.displayName,
                        fontSize      = 12.sp,
                        fontWeight    = FontWeight.ExtraBold,
                        letterSpacing = (-0.1).sp,
                        color         = Ink,
                        lineHeight    = 15.sp,
                        maxLines      = 2
                    )

                    Text(
                        text          = "RESTRICTED",
                        fontSize      = 8.sp,
                        fontWeight    = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp,
                        color         = category.color,
                        modifier      = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}


/**
 * Authentic Physical Neo-Brutalist Status Tag on Ticket Cards.
 */
@Composable
fun TicketRibbonTag(
    status: ComplaintStatus,
    modifier: Modifier = Modifier
) {
    val (ribbonBg, ribbonFg) = when (status) {
        ComplaintStatus.SUBMITTED -> ConcreteGray to Color.White
        ComplaintStatus.UNDER_ADMIN_REVIEW -> IdCardBlue to Color.White
        ComplaintStatus.ASSIGNED -> CircuitViolet to Color.White
        ComplaintStatus.IN_PROGRESS -> HazardAmber to Ink
        ComplaintStatus.AWAITING_STUDENT_INFO -> ConeOrange to Color.White
        ComplaintStatus.VENDOR_ASSIGNED -> StatusVendor to Color.White
        ComplaintStatus.RESOLVED -> ClearanceGreen to Color.White
        ComplaintStatus.REOPENED -> AlertRed to Color.White
        ComplaintStatus.CLOSED -> CharcoalClosed to Color.White
    }

    Box(
        modifier = modifier
            .drawBehind {
                drawRoundRect(
                    color = Ink,
                    topLeft = androidx.compose.ui.geometry.Offset(2.dp.toPx(), 2.dp.toPx()),
                    size = size,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                )
            }
            .clip(RoundedCornerShape(6.dp))
            .background(ribbonBg)
            .border(2.dp, Ink, RoundedCornerShape(6.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // Status dot indicator
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(ribbonFg)
            )
            Text(
                text          = status.label.uppercase(),
                color         = ribbonFg,
                fontSize      = 10.sp,
                fontWeight    = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp,
                maxLines      = 1
            )
        }
    }
}


val CircuitViolet = Color(0xFF6A4C93)
val CharcoalClosed = Color(0xFF33312B)

/**
 * Exact Neo-Brutalist Status Chip with 2px border and 15% opacity background (§2.3).
 */
@Composable
fun NeoStatusChip(
    text: String,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    textColor: Color = Ink,
    fontSize: Int = 11
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(backgroundColor.copy(alpha = 0.16f))
            .border(2.dp, backgroundColor, RoundedCornerShape(100.dp))
            .padding(horizontal = 10.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = backgroundColor,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.3.sp
        )
    }
}

/**
 * 5-Block Animated Segmented Loader (§7.1 / §9).
 */
@Composable
fun SegmentedLoader(
    modifier: Modifier = Modifier,
    blockCount: Int = 5,
    blockWidth: Dp = 14.dp,
    blockHeight: Dp = 8.dp,
    activeColor: Color = Ink
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loader_anim")
    val activeIndex by infiniteTransition.animateValue(
        initialValue = 0,
        targetValue = blockCount,
        typeConverter = Int.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "block_index"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until blockCount) {
            val isCurrent = i <= activeIndex
            Box(
                modifier = Modifier
                    .width(blockWidth)
                    .height(blockHeight)
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (isCurrent) activeColor else Color.Transparent)
                    .border(2.dp, Ink, RoundedCornerShape(1.dp))
            )
        }
    }
}

/**
 * Severity Segmented Control (Low, Medium, High, Critical)
 */
@Composable
fun NeoSegmentedSeverity(
    selectedSeverity: String,
    onSeveritySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf("Low", "Medium", "High", "Critical")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(100.dp))
            .background(SurfaceCard)
            .border(3.dp, Ink, RoundedCornerShape(100.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEach { option ->
            val isSelected = option.equals(selectedSeverity, ignoreCase = true)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(100.dp))
                    .background(if (isSelected) Ink else Color.Transparent)
                    .clickable { onSeveritySelected(option) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = if (isSelected) Color.White else Ink
                )
            }
        }
    }
}

/**
 * Get category icon for any CategoryId
 */
fun getCategoryIcon(category: CategoryId): ImageVector {
    return when (category) {
        CategoryId.HOSTEL -> Icons.Default.Home
        CategoryId.ACADEMIC -> Icons.Default.School
        CategoryId.FOOD -> Icons.Default.Restaurant
        CategoryId.HEALTH -> Icons.Default.MedicalServices
        CategoryId.HYGIENE -> Icons.Default.CleaningServices
        CategoryId.TECH -> Icons.Default.Build
        CategoryId.WELFARE -> Icons.Default.VolunteerActivism
        CategoryId.MISSING_ITEMS -> Icons.Default.FindInPage
        CategoryId.RAGGING -> Icons.Default.Shield
        CategoryId.HARASSMENT -> Icons.Default.Warning
        CategoryId.MALPRACTICE -> Icons.Default.ReportProblem
    }
}

/**
 * Get informative micro-tagline for Category Card
 */
fun getCategoryTagline(category: CategoryId): String {
    return when (category) {
        CategoryId.ACADEMIC -> "Labs, grading & portal"
        CategoryId.HOSTEL -> "Rooms, plumbing & power"
        CategoryId.FOOD -> "Mess halls & hygiene"
        CategoryId.HEALTH -> "First aid & medical"
        CategoryId.HYGIENE -> "Sanitation & coolers"
        CategoryId.TECH -> "Wi-Fi & devices"
        CategoryId.WELFARE -> "Aid & student support"
        CategoryId.MISSING_ITEMS -> "Lost & found property"
        CategoryId.RAGGING -> "Zero tolerance"
        CategoryId.HARASSMENT -> "Confidential escalation"
        CategoryId.MALPRACTICE -> "Integrity & exams"
    }
}
