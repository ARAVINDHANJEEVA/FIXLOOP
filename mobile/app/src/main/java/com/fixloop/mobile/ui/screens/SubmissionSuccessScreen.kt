package com.fixloop.mobile.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.fixloop.mobile.state.SentinelAppState
import com.fixloop.mobile.ui.components.NeoButton
import com.fixloop.mobile.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── Layout Constants (CSS Pixel-Accurate & Cross-Device Responsive) ───────
private val PRINTER_W        = 320.dp
private val PRINTER_H        = 96.dp
private val LID_H            = 48.dp
private val SEAM_Y           = 48.dp

private val SLIT_W           = 276.dp
private val SLIT_H           = 26.dp
private val SLIT_LEFT        = 22.dp

private val LCD_W            = 166.dp
private val LCD_H            = 32.dp
private val LCD_LEFT         = 14.dp
private val LCD_TOP          = 8.dp

private val BTN_W            = 48.dp
private val BTN_H            = 36.dp
private val BTN_RIGHT        = 14.dp
private val BTN_TOP          = 6.dp

private val PAPER_W          = 276.dp
private val PAPER_LEFT       = 22.dp
private val PAPER_EMERGE_TOP = 50.dp

// ── Color Palette ──────────────────────────────────────────────────────────
private val PrinterCream     = Color(0xFFECE8DC)
private val PrinterCreamLid  = Color(0xFFF4F1E8)
private val PaperCream       = Color(0xFFFCFBF7)
private val SlitTopGrad      = Color(0xFF22211D)
private val SlitBotGrad      = Color(0xFF0A0A08)
private val CrtBg            = Color(0xFF071207)
private val CrtGreen         = Color(0xFF39FF14)
private val PaperEdge        = Color(0x221B1B17)
private val ReceiptMuted     = Color(0xFF6F6B60)
private val PerforationCol   = Color(0xFFA6A296)
private val TokenBg          = Color(0xFFF0EDE4)
private val TokenBlue        = Color(0xFF2A4494)
private val SevRed           = Color(0xFFE3483A)
private val FooterBg         = Color(0xFFF2EEE4)

// ════════════════════════════════════════════════════════════════════════════
// Submission Success Screen — Interactive Retro Thermal Receipt Printer
// ════════════════════════════════════════════════════════════════════════════
@Composable
fun SubmissionSuccessScreen(
    appState: SentinelAppState,
    onTrackComplaint: (String) -> Unit,
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context   = LocalContext.current
    val complaint = appState.lastSubmittedComplaint ?: appState.complaints.first()
    val scope     = rememberCoroutineScope()

    val paperReveal    = remember { Animatable(0f) }
    var lcdPhase       by remember { mutableStateOf(0) }
    var visibleLetters by remember { mutableStateOf(0) }
    var copied         by remember { mutableStateOf(false) }
    val printWord = "Printing..."

    fun triggerPrint() {
        scope.launch {
            lcdPhase = 1
            visibleLetters = 0
            paperReveal.snapTo(0f)

            // Step 1: LCD letter-by-letter typing animation (50ms per letter)
            for (i in 1..printWord.length) {
                delay(50L)
                visibleLetters = i
            }

            // Step 2: Mechanical paper feed animation (1.1s cubic-bezier)
            paperReveal.animateTo(
                1f,
                tween(1100, easing = CubicBezierEasing(0.215f, 0.61f, 0.355f, 1.0f))
            )
            // Step 3: Natural mechanical bounce / settle
            paperReveal.animateTo(0.98f, tween(120, easing = LinearEasing))
            paperReveal.animateTo(1.00f, tween(180, easing = FastOutSlowInEasing))

            delay(100)
            lcdPhase = 2
        }
    }

    LaunchedEffect(Unit)   { triggerPrint() }
    LaunchedEffect(copied) { if (copied) { delay(1500); copied = false } }

    val slideOffset by remember { derivedStateOf { (-480f * (1f - paperReveal.value)).dp } }
    val clipTopFrac by remember { derivedStateOf { 0.92f * (1f - paperReveal.value) } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgPaper)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(10.dp))

        // Dispatch Status Pill Badge
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(100.dp))
                .background(ClearanceGreen.copy(alpha = 0.12f))
                .border(1.5.dp, ClearanceGreen, RoundedCornerShape(100.dp))
                .padding(horizontal = 14.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(ClearanceGreen))
            Text(
                text = "DISPATCH LOGGED — SLA: 48 HRS",
                color = ClearanceGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = "Complaint registered.",
            fontSize = 23.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Ink,
            letterSpacing = (-0.3).sp
        )
        Text(
            text = "Collect your official physical grievance slip below.",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = InkMuted,
            modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
        )

        // ════════════════════════════════════════════════════════════════════
        // Hardware Printer & Emerging Thermal Receipt Composite Container
        // ════════════════════════════════════════════════════════════════════
        Box(
            modifier = Modifier
                .width(PRINTER_W)
                .wrapContentHeight(),
            contentAlignment = Alignment.TopStart
        ) {
            // LAYER 1: Emerging Thermal Receipt Paper (emerges out of slit)
            Column(
                modifier = Modifier
                    .zIndex(1f)
                    .padding(top = PAPER_EMERGE_TOP, start = PAPER_LEFT)
                    .width(PAPER_W)
                    .offset(y = slideOffset)
                    .drawWithContent {
                        clipRect(
                            top    = size.height * clipTopFrac,
                            bottom = size.height + 400f,
                            left   = -100f,
                            right  = size.width + 100f
                        ) { this@drawWithContent.drawContent() }
                    }
                    .graphicsLayer {
                        shadowElevation = 20f
                        ambientShadowColor = Color(android.graphics.Color.argb(60, 27, 27, 23))
                        spotShadowColor = Color(android.graphics.Color.argb(40, 27, 27, 23))
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Perforated Triangular Teeth
                ThermalTeeth(isTop = true)

                // Main Receipt Paper Body
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PaperCream)
                        .drawBehind {
                            val s = 1.dp.toPx()
                            drawLine(PaperEdge, Offset(0f, 0f), Offset(0f, size.height), s)
                            drawLine(PaperEdge, Offset(size.width, 0f), Offset(size.width, size.height), s)
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Receipt Brand Lockup
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = HazardAmber,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "SENTINEL DISPATCH",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = Ink,
                            letterSpacing = 0.8.sp
                        )
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = HazardAmber,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Text(
                        text = "CAMPUS VIGILANCE & GRIEVANCE CELL",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = ReceiptMuted,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    // Order Title Badge
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .background(FooterBg)
                            .border(1.5.dp, Ink)
                            .padding(horizontal = 10.dp, vertical = 2.5.dp)
                    ) {
                        Text(
                            text = "OFFICIAL DISPATCH SLIP",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 9.5.sp,
                            color = Ink,
                            letterSpacing = 0.6.sp
                        )
                    }

                    ReceiptPerf()

                    // Reference Token Section (Directly Tap-to-Copy)
                    val tokenInteraction = remember { MutableInteractionSource() }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (copied) ClearanceGreen.copy(alpha = 0.12f) else TokenBg)
                            .border(
                                width = 1.5.dp,
                                color = if (copied) ClearanceGreen else Ink,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable(
                                interactionSource = tokenInteraction,
                                indication = null
                            ) {
                                val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cb.setPrimaryClip(ClipData.newPlainText("Token", complaint.id))
                                copied = true
                                appState.triggerToast("Token copied to clipboard: ${complaint.id}")
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "TICKET REFERENCE TOKEN",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (copied) ClearanceGreen else ReceiptMuted,
                                    letterSpacing = 0.6.sp
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        tint = if (copied) ClearanceGreen else ReceiptMuted,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = if (copied) "COPIED" else "TAP TO COPY",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (copied) ClearanceGreen else ReceiptMuted,
                                        letterSpacing = 0.4.sp
                                    )
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = complaint.id,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.8.sp,
                                color = if (copied) ClearanceGreen else TokenBlue
                            )
                        }
                    }

                    ReceiptPerf()

                    // Tabular Data Rows
                    val sevUp   = complaint.severity.uppercase()
                    val cleanId = complaint.id.replace(Regex("[^0-9]"), "").takeLast(4).ifBlank { "9012" }

                    ReceiptRow("CATEGORY", complaint.categoryId.displayName.uppercase())
                    ReceiptRow("LOCATION", complaint.location)
                    ReceiptRow(
                        label = "SEVERITY",
                        value = "$sevUp ${when(sevUp){ "CRITICAL"->"(L1)"; "HIGH"->"(L2)"; else->"(L3)" }}",
                        valueColor = if (sevUp in listOf("CRITICAL", "HIGH")) SevRed else Ink
                    )
                    ReceiptRow("TIMESTAMP", complaint.timestamp)
                    ReceiptRow("DISPATCH",  "AUTO-ROUTED")
                    ReceiptRow("IDENTITY",  if (complaint.anonymous) "PROTECTED (ANON)" else "STUDENT TICKET")

                    ReceiptPerf()

                    // Procedural Barcode Section
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        ThermalBarcode()
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = "* $cleanId-SNT-ALPHA-VERIFIED *",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.sp,
                            color = ReceiptMuted,
                            letterSpacing = 1.2.sp
                        )
                    }

                    // Official Footer Retention Notice
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(FooterBg)
                            .padding(vertical = 5.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(1.5.dp)
                        ) {
                            Text(
                                text = "OFFICIAL RECORD  •  RETAIN FOR YOUR RECORDS",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.5.sp,
                                color = Ink.copy(alpha = 0.55f),
                                textAlign = TextAlign.Center,
                                letterSpacing = 0.3.sp
                            )
                            Text(
                                text = "Thank you for making campus safer.",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.5.sp,
                                color = ReceiptMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Bottom Perforated Triangular Teeth
                ThermalTeeth(isTop = false)
            }

            // LAYER 2: Hardware Unit (Casing, Seam, Slit, CRT Screen & Feed Button)
            PrinterHardware(
                modifier = Modifier.zIndex(2f),
                lcdPhase = lcdPhase,
                printWord = printWord,
                visibleLetters = visibleLetters,
                onReprint = ::triggerPrint
            )
        }

        Spacer(Modifier.height(16.dp))

        NeoButton(
            text = "Track this complaint",
            onClick = { onTrackComplaint(complaint.id) },
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = HazardAmber
        )
        Spacer(Modifier.height(8.dp))
        NeoButton(
            text = "Back to home",
            onClick = onNavigateHome,
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = SurfaceCard,
            contentColor = Ink
        )
        Spacer(Modifier.height(20.dp))
    }
}

// ════════════════════════════════════════════════════════════════════════════
// Unified Printer Hardware Unit
// ════════════════════════════════════════════════════════════════════════════
@Composable
private fun PrinterHardware(
    modifier: Modifier = Modifier,
    lcdPhase: Int,
    printWord: String,
    visibleLetters: Int,
    onReprint: () -> Unit
) {
    val lcdText = when (lcdPhase) {
        1 -> printWord.take(visibleLetters)
        2 -> "DISPATCH ISSUED"
        else -> "READY / PRINT"
    }
    val lcdPrefix = if (lcdPhase == 1) "▶ " else "● "

    // Outer Casing Box with Single Hard Neo-Brutal Shadow
    Box(
        modifier = modifier
            .padding(bottom = 6.dp, end = 6.dp)
            .width(PRINTER_W)
            .height(PRINTER_H)
            .drawBehind {
                drawRoundRect(
                    color = Ink,
                    topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                    size = size,
                    cornerRadius = CornerRadius(12.dp.toPx())
                )
            }
            .clip(RoundedCornerShape(12.dp))
            .background(PrinterCream)
            .border(3.dp, Ink, RoundedCornerShape(12.dp))
    ) {
        // ── 1. Top Hinged Lid Section ──────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(LID_H)
                .background(PrinterCreamLid)
        )

        // ── 2. Mechanical Seam Divider Line ────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = SEAM_Y)
                .height(3.dp)
                .background(Ink)
        )

        // ── 3. Ventilation Grill Louvers (Lid) ─────────────────────────────
        Row(
            modifier = Modifier
                .padding(start = 16.dp, top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(4) {
                Box(
                    modifier = Modifier
                        .size(width = 18.dp, height = 3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Ink.copy(alpha = 0.15f))
                )
            }
        }

        // ── 4. Deep Recessed Paper Exit Mouth / Slit Cavity ─────────────────
        Box(
            modifier = Modifier
                .padding(top = SEAM_Y, start = SLIT_LEFT)
                .width(SLIT_W)
                .height(SLIT_H)
                .clip(RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
                .background(Brush.verticalGradient(listOf(SlitTopGrad, SlitBotGrad)))
                .border(2.dp, Ink, RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
        ) {
            // Subtle specular edge highlight
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.08f))
            )
        }

        // ── 5. Green CRT / LCD Terminal Status Screen (Lid) ─────────────────
        Box(
            modifier = Modifier
                .absoluteOffset(x = LCD_LEFT, y = LCD_TOP)
                .width(LCD_W)
                .height(LCD_H)
                .drawBehind {
                    drawRoundRect(
                        color = Ink,
                        topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
                        size = size,
                        cornerRadius = CornerRadius(6.dp.toPx())
                    )
                }
                .clip(RoundedCornerShape(6.dp))
                .background(CrtBg)
                .border(2.dp, Ink, RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 2.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            // CRT Scanline Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                var y = 0f
                while (y < size.height) {
                    drawRect(
                        color = CrtGreen.copy(alpha = 0.08f),
                        topLeft = Offset(0f, y),
                        size = Size(size.width, 2.dp.toPx())
                    )
                    y += 4.dp.toPx()
                }
            }

            Text(
                text = "$lcdPrefix$lcdText${if (lcdPhase == 1) "_" else ""}",
                fontFamily = FontFamily.Monospace,
                color = CrtGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                maxLines = 1
            )
        }

        // ── 6. Chunky Tactile Amber Feed / Reprint Button (Lid) ────────────
        val btnInteraction = remember { MutableInteractionSource() }
        val btnPressed by btnInteraction.collectIsPressedAsState()
        val btnOffset by animateDpAsState(
            targetValue = if (btnPressed) 2.5.dp else 0.dp,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness    = Spring.StiffnessHigh
            ),
            label = "feed_btn_press"
        )

        val btnLeft = PRINTER_W - BTN_W - BTN_RIGHT - 3.dp
        Box(
            modifier = Modifier
                .absoluteOffset(x = btnLeft, y = BTN_TOP)
                .size(BTN_W, BTN_H)
                .drawBehind {
                    drawRoundRect(
                        color = Ink,
                        topLeft = Offset(2.5.dp.toPx(), 2.5.dp.toPx()),
                        size = size,
                        cornerRadius = CornerRadius(8.dp.toPx())
                    )
                }
                .offset(x = btnOffset, y = btnOffset)
                .clip(RoundedCornerShape(8.dp))
                .background(HazardAmber)
                .border(2.5.dp, Ink, RoundedCornerShape(8.dp))
                .clickable(
                    interactionSource = btnInteraction,
                    indication = null,
                    onClick = onReprint
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Print,
                contentDescription = "Reprint Ticket",
                tint = Ink,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// Thermal Paper Jagged Perforated Teeth (10dp pitch)
// ════════════════════════════════════════════════════════════════════════════
@Composable
private fun ThermalTeeth(isTop: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.width(PAPER_W).height(10.dp)) {
        val tw = 10.dp.toPx()
        val th = size.height
        val count = (size.width / tw).toInt() + 2
        val path = Path().apply {
            if (isTop) {
                moveTo(0f, th)
                for (i in 0 until count) {
                    val x = i * tw
                    lineTo(x + tw / 2f, 0f)
                    lineTo(x + tw, th)
                }
                lineTo(size.width, th)
                close()
            } else {
                moveTo(0f, 0f)
                for (i in 0 until count) {
                    val x = i * tw
                    lineTo(x + tw / 2f, th)
                    lineTo(x + tw, 0f)
                }
                lineTo(size.width, 0f)
                close()
            }
        }
        drawPath(path, PaperCream)
        drawLine(PaperEdge, Offset(0f, 0f), Offset(0f, th), 1.dp.toPx())
        drawLine(PaperEdge, Offset(size.width, 0f), Offset(size.width, th), 1.dp.toPx())
    }
}

// ════════════════════════════════════════════════════════════════════════════
// Dashed Perforation Line
// ════════════════════════════════════════════════════════════════════════════
@Composable
private fun ReceiptPerf() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .height(1.5.dp)
            .drawWithContent {
                val dash = 6.dp.toPx()
                val gap  = 3.dp.toPx()
                var x = 0f
                while (x < size.width) {
                    drawLine(
                        color = PerforationCol,
                        start = Offset(x, size.height / 2f),
                        end   = Offset(minOf(x + dash, size.width), size.height / 2f),
                        strokeWidth = size.height
                    )
                    x += dash + gap
                }
            }
    )
}

// ════════════════════════════════════════════════════════════════════════════
// Tabular Receipt Data Row
// ════════════════════════════════════════════════════════════════════════════
@Composable
private fun ReceiptRow(label: String, value: String, valueColor: Color = Ink) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = ReceiptMuted,
            modifier = Modifier.weight(0.38f)
        )
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.62f)
        )
    }
}

// ════════════════════════════════════════════════════════════════════════════
// CSS Barcode Canvas — 26px repeating linear cycle
// ════════════════════════════════════════════════════════════════════════════
@Composable
fun ThermalBarcode(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.width(210.dp).height(28.dp)) {
        val segs  = listOf(0f to 2f, 4f to 5f, 8f to 11f, 13f to 14f, 17f to 20f, 21f to 24f)
        val cycle = 26f * density
        var cx    = 0f
        while (cx < size.width) {
            segs.forEach { (s, e) ->
                val sx = cx + s * density
                val ex = cx + e * density
                if (sx < size.width) {
                    drawRect(
                        color   = Ink,
                        topLeft = Offset(sx, 0f),
                        size    = Size(minOf(ex, size.width) - sx, size.height)
                    )
                }
            }
            cx += cycle
        }
    }
}