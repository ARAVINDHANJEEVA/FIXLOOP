package com.fixloop.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fixloop.mobile.data.local.SessionManager
import com.fixloop.mobile.state.SentinelAppState
import com.fixloop.mobile.ui.components.NeoButton
import com.fixloop.mobile.ui.components.NeoCard
import com.fixloop.mobile.ui.components.NeoIconButton
import com.fixloop.mobile.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    appState: SentinelAppState,
    onLoginSuccess: () -> Unit,
    onNavigateToSignup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    var email by remember { mutableStateOf("student@fixloop.edu") }
    var password by remember { mutableStateOf("student123") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Inline server IP editor state
    var showIpEditor by remember { mutableStateOf(false) }
    var editableIp by remember { mutableStateOf(sessionManager.getHotspotIp()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgPaper)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(28.dp))

        // Small Logo
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(HazardAmber)
                .border(2.5.dp, Ink, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = Ink, modifier = Modifier.size(26.dp))
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Welcome back.",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Ink
        )
        Text(
            text = "Sign in with your college email to continue.",
            fontSize = 14.sp,
            color = InkMuted,
            modifier = Modifier.padding(top = 4.dp, bottom = 28.dp)
        )

        // Email Field
        Text(text = "College email", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Ink)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = { Text("yourname@college.edu") },
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
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Password Field
        Text(text = "Password", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Ink)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = { Text("••••••••") },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle password",
                        tint = Ink
                    )
                }
            },
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

        // Forgot password
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = "Forgot password?",
                color = IdCardBlue,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { }
            )
        }
        // ── Inline Server IP Configurator ────────────────────────────────────
        val currentUrl = sessionManager.getBaseUrl()
        val displayUrl = currentUrl.removePrefix("http://").removeSuffix("/")

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(if (showIpEditor) HazardAmber.copy(alpha = 0.12f) else Ink.copy(alpha = 0.04f))
                .border(1.5.dp, if (showIpEditor) HazardAmber else Ink.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Wifi, contentDescription = null, tint = if (showIpEditor) HazardAmber else InkMuted, modifier = Modifier.size(14.dp))
            if (!showIpEditor) {
                Text(text = "Backend: $displayUrl", fontSize = 11.sp, color = InkMuted, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(HazardAmber)
                        .border(1.dp, Ink, RoundedCornerShape(6.dp))
                        .clickable { showIpEditor = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = Ink, modifier = Modifier.size(11.dp))
                        Text("Change", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                    }
                }
            } else {
                OutlinedTextField(
                    value = editableIp,
                    onValueChange = { editableIp = it },
                    placeholder = { Text("192.168.43.x", fontSize = 12.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceCard, unfocusedContainerColor = SurfaceCard,
                        focusedBorderColor = HazardAmber, unfocusedBorderColor = Ink.copy(alpha = 0.3f)
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                        .background(HazardAmber).border(1.5.dp, Ink, RoundedCornerShape(8.dp))
                        .clickable {
                            if (editableIp.isNotBlank()) {
                                sessionManager.setHotspotIp(editableIp.trim())
                                sessionManager.setConnectionMode(SessionManager.ConnectionMode.HOTSPOT)
                            }
                            showIpEditor = false
                            errorMessage = null
                        },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.Check, contentDescription = "Save", tint = Ink, modifier = Modifier.size(18.dp)) }
                Box(
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                        .background(SurfaceCard).border(1.5.dp, Ink.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .clickable { showIpEditor = false },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.Close, contentDescription = "Cancel", tint = InkMuted, modifier = Modifier.size(16.dp)) }
            }
        }
        // ─────────────────────────────────────────────────────────────────────

        Spacer(modifier = Modifier.height(14.dp))

        if (errorMessage != null) {
            Surface(
                color = AlertRed.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, AlertRed),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = AlertRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AlertRed
                    )
                }
            }
        }

        // Log in CTA
        NeoButton(
            text = if (isSubmitting) "Signing in..." else "Log in",
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    errorMessage = "Please enter both email and password"
                    return@NeoButton
                }
                isSubmitting = true
                errorMessage = null
                appState.login(email.trim(), password) { success, err ->
                    isSubmitting = false
                    if (success) {
                        onLoginSuccess()
                    } else {
                        errorMessage = err ?: "Sign in failed"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = HazardAmber,
            enabled = !isSubmitting
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Create Account Link
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = "New here? ", fontSize = 13.sp, color = InkMuted)
            Text(
                text = "Create account",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = IdCardBlue,
                modifier = Modifier.clickable { onNavigateToSignup() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(
    appState: SentinelAppState,
    onSignupSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var rollNo by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("Computer Science & Engineering") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var agreedToConduct by remember { mutableStateOf(true) }
    var showDeptMenu by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val departments = listOf(
        "Computer Science & Engineering",
        "Electrical & Electronics",
        "Mechanical Engineering",
        "Civil Engineering",
        "Biotechnology"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgPaper)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Logo
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(HazardAmber)
                .border(2.5.dp, Ink, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = Ink, modifier = Modifier.size(26.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Create your account.", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text(text = "Sentinel is restricted to verified campus students.", fontSize = 13.sp, color = InkMuted, modifier = Modifier.padding(top = 4.dp, bottom = 20.dp))

        // Full Name
        Text(text = "Full name", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Ink)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = { Text("e.g. Alex Johnson") },
            modifier = Modifier.fillMaxWidth().border(2.dp, Ink, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = SurfaceCard, unfocusedContainerColor = SurfaceCard, focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Roll Number
        Text(text = "Roll number", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Ink)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = rollNo,
            onValueChange = { rollNo = it },
            placeholder = { Text("e.g. 22CS084") },
            modifier = Modifier.fillMaxWidth().border(2.dp, Ink, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = SurfaceCard, unfocusedContainerColor = SurfaceCard, focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // College Email
        Text(text = "College email", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Ink)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = { Text("e.g. alex.j@college.edu") },
            modifier = Modifier.fillMaxWidth().border(2.dp, Ink, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = SurfaceCard, unfocusedContainerColor = SurfaceCard, focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Department
        Text(text = "Department", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Ink)
        Spacer(modifier = Modifier.height(4.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceCard)
                    .border(2.dp, Ink, RoundedCornerShape(12.dp))
                    .clickable { showDeptMenu = true }
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = department, fontSize = 14.sp, color = Ink)
                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = Ink)
            }
            DropdownMenu(
                expanded = showDeptMenu,
                onDismissRequest = { showDeptMenu = false }
            ) {
                departments.forEach { dept ->
                    DropdownMenuItem(
                        text = { Text(dept) },
                        onClick = {
                            department = dept
                            showDeptMenu = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Password
        Text(text = "Password", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Ink)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = { Text("••••••••") },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle password",
                        tint = Ink
                    )
                }
            },
            modifier = Modifier.fillMaxWidth().border(2.dp, Ink, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceCard,
                unfocusedContainerColor = SurfaceCard,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Conduct Checkbox
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable { agreedToConduct = !agreedToConduct }
                .padding(vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (agreedToConduct) HazardAmber else SurfaceCard)
                    .border(2.dp, Ink, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (agreedToConduct) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Ink, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = "I agree to the campus code of conduct.", fontSize = 13.sp, color = Ink)
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (errorMessage != null) {
            Surface(
                color = AlertRed.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, AlertRed),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = AlertRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AlertRed
                    )
                }
            }
        }

        NeoButton(
            text = if (isSubmitting) "Creating account..." else "Create account",
            onClick = {
                if (name.isBlank() || rollNo.isBlank() || email.isBlank() || password.isBlank()) {
                    errorMessage = "Please fill in all required fields"
                    return@NeoButton
                }
                if (!email.contains("@")) {
                    errorMessage = "Please enter a valid college email address"
                    return@NeoButton
                }
                isSubmitting = true
                errorMessage = null
                appState.signup(
                    name = name.trim(),
                    rollNo = rollNo.trim(),
                    email = email.trim(),
                    password = password,
                    department = department
                ) { success, err ->
                    isSubmitting = false
                    if (success) {
                        onSignupSuccess()
                    } else {
                        errorMessage = err ?: "Registration failed"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = HazardAmber,
            enabled = agreedToConduct && !isSubmitting
        )

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = "Already have an account? ", fontSize = 13.sp, color = InkMuted)
            Text(
                text = "Log in",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = IdCardBlue,
                modifier = Modifier.clickable { onNavigateToLogin() }
            )
        }
    }
}

@Composable
fun ProfileSetupScreen(
    appState: SentinelAppState,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedBlock by remember { mutableStateOf("Hostel Block C (Aryabhata)") }
    var roomNo by remember { mutableStateOf("214") }
    var showBlockMenu by remember { mutableStateOf(false) }

    val blocks = listOf(
        "Hostel Block C (Aryabhata)",
        "Hostel Block A (Raman)",
        "Hostel Block B (Bhabha)",
        "Hostel Block D (Kalpana)",
        "Day Scholar (Off-campus)"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgPaper)
            .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ConeOrange)
                .border(2.5.dp, Ink, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Default.Home, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(text = "Where do you stay?", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text(
            text = "This speeds up hostel & hygiene reports — you can change it anytime.",
            fontSize = 14.sp,
            color = InkMuted,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        // Hostel Block
        Text(text = "Hostel block", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Ink)
        Spacer(modifier = Modifier.height(6.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceCard)
                    .border(2.dp, Ink, RoundedCornerShape(12.dp))
                    .clickable { showBlockMenu = true }
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = selectedBlock, fontSize = 14.sp, color = Ink)
                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = Ink)
            }
            DropdownMenu(
                expanded = showBlockMenu,
                onDismissRequest = { showBlockMenu = false }
            ) {
                blocks.forEach { block ->
                    DropdownMenuItem(
                        text = { Text(block) },
                        onClick = {
                            selectedBlock = block
                            showBlockMenu = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Room number
        Text(text = "Room number", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Ink)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = roomNo,
            onValueChange = { roomNo = it },
            placeholder = { Text("e.g. 214") },
            modifier = Modifier.fillMaxWidth().border(2.dp, Ink, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = SurfaceCard, unfocusedContainerColor = SurfaceCard, focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent),
            singleLine = true
        )

        Spacer(modifier = Modifier.weight(1f))

        NeoButton(
            text = "Save and continue",
            onClick = {
                appState.updateUserProfile(appState.user.name, selectedBlock, roomNo)
                onFinished()
            },
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = HazardAmber
        )

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(
            onClick = onFinished,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Skip for now", color = InkMuted, fontWeight = FontWeight.Medium, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
