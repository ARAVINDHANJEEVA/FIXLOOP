package com.fixloop.mobile.state

import androidx.compose.runtime.*
import com.fixloop.mobile.SentinelApp
import com.fixloop.mobile.data.repository.FixLoopRepository
import com.fixloop.mobile.model.*
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class SentinelAppState {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Repository access via SentinelApp Application instance
    val repository: FixLoopRepository by lazy {
        FixLoopRepository(SentinelApp.instance)
    }

    var user by mutableStateOf(UserProfile(isLoggedIn = false))
        private set

    var complaints by mutableStateOf(emptyList<Complaint>())
        private set

    var drafts by mutableStateOf(emptyList<Draft>())
        private set

    var notifications by mutableStateOf(emptyList<NotificationItem>())
        private set

    // Active working form state
    var formCategory by mutableStateOf(CategoryId.HOSTEL)
    var formFields = mutableStateMapOf<String, String>()
    var formSeverity by mutableStateOf("Medium")
    var formStudentsAffected by mutableStateOf(1)
    var formLocation by mutableStateOf("Hostel Block C (Aryabhata), Room 214")
    var formAnonymous by mutableStateOf(false)
    var formPhotos = mutableStateListOf<String>()

    // Last submitted complaint for Receipt display
    var lastSubmittedComplaint by mutableStateOf<Complaint?>(null)

    // Draft toast trigger
    var showDraftToast by mutableStateOf(false)
    var toastMessage by mutableStateOf("Draft saved.")

    var isSyncing by mutableStateOf(false)
        private set

    val unreadNotificationsCount: Int
        get() = notifications.count { it.isUnread }

    val needsAttentionCount: Int
        get() = complaints.count { it.status == ComplaintStatus.AWAITING_STUDENT_INFO }

    init {
        // 1. Restore cached session if valid
        try {
            val cachedUser = repository.getUserProfile()
            if (cachedUser.isLoggedIn) {
                user = cachedUser
            }
        } catch (_: Exception) {}

        // 2. Observe Room DB cache for Complaints (Flow -> Compose State)
        scope.launch {
            try {
                repository.getComplaintsFlow().collect { cachedList ->
                    complaints = cachedList
                }
            } catch (_: Exception) {}
        }

        // 3. Observe Room DB cache for Notifications (Flow -> Compose State)
        scope.launch {
            try {
                repository.getNotificationsFlow().collect { cachedList ->
                    notifications = cachedList
                }
            } catch (_: Exception) {}
        }

        // 4. Background refresh if logged in
        if (user.isLoggedIn) {
            refreshData()
        }
    }

    fun refreshData() {
        scope.launch {
            isSyncing = true
            try {
                repository.refreshAll()
                user = repository.getUserProfile()
            } catch (_: Exception) {
            } finally {
                isSyncing = false
            }
        }
    }

    fun loadComplaintDetail(complaintId: String) {
        scope.launch {
            try {
                val result = repository.fetchComplaintDetail(complaintId)
                result.onSuccess { updatedComplaint ->
                    complaints = complaints.map {
                        if (it.id == updatedComplaint.id) updatedComplaint else it
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun getCategoryOpenTicketsCount(categoryId: CategoryId): Int {
        return complaints.count {
            it.categoryId == categoryId && it.status != ComplaintStatus.RESOLVED && it.status != ComplaintStatus.CLOSED
        }
    }

    fun startNewComplaint(category: CategoryId) {
        formCategory = category
        formFields.clear()
        formSeverity = "Medium"
        formStudentsAffected = 1
        formLocation = when (category) {
            CategoryId.HOSTEL -> if (user.hostelBlock.isNotBlank()) "${user.hostelBlock}, Room ${user.roomNo.ifBlank { "214" }}" else "Hostel Block C (Aryabhata), Room 214"
            CategoryId.ACADEMIC -> "Academic Block 1 (Main), Classroom / Lecture Hall"
            CategoryId.FOOD -> "Central Dining Hall / Mess, Ground Floor"
            CategoryId.HEALTH -> "Campus Health Center, Ground Floor"
            CategoryId.HYGIENE -> if (user.hostelBlock.isNotBlank()) "${user.hostelBlock}, Washroom Wing" else "Hostel Block C (Aryabhata), Washroom Wing"
            CategoryId.TECH -> "Academic Block 2 (CSE / IT), Computer Lab"
            CategoryId.RAGGING -> "Hostel Corridor / Campus Grounds"
            CategoryId.HARASSMENT -> "Campus Zone, Main Campus"
            CategoryId.MALPRACTICE -> "Academic Block 1 (Main), Exam Hall"
            CategoryId.WELFARE -> "Student Activity Center, Counseling Room"
            CategoryId.MISSING_ITEMS -> "Central Library & Reading Hall, Desk"
        }
        formAnonymous = false
        formPhotos.clear()
    }

    fun resumeDraft(draft: Draft) {
        formCategory = draft.categoryId
        formFields.clear()
        formFields["title"] = draft.title
        formFields["description"] = draft.description
        formSeverity = draft.severity
        formLocation = draft.location
        formAnonymous = draft.anonymous
        formPhotos.clear()
    }

    fun triggerToast(msg: String = "Draft saved.") {
        toastMessage = msg
        showDraftToast = true
    }

    fun submitComplaint(): Complaint {
        val randomNum = Random.nextInt(100, 999)
        val token = "SNT-2026-000$randomNum"
        val timeNow = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()).format(Date())

        val title = formFields["title"]?.ifBlank { null }
            ?: formFields["condition_summary"]?.ifBlank { null }
            ?: "${formCategory.displayName} Issue Report"

        val description = formFields["description"]?.ifBlank { null }
            ?: formFields["detailed_description"]?.ifBlank { null }
            ?: "Grievance submitted by student."

        // Optimistic local ticket
        val newComplaint = Complaint(
            id = token,
            categoryId = formCategory,
            title = title,
            description = description,
            severity = formSeverity,
            location = formLocation,
            status = ComplaintStatus.SUBMITTED,
            createdAt = "Just now",
            timestamp = timeNow,
            anonymous = formAnonymous,
            photos = formPhotos.toList(),
            dispatch = null,
            timeline = listOf(
                TimelineNode(
                    status = ComplaintStatus.SUBMITTED,
                    time = "Just now",
                    note = "Ticket registered and encrypted into campus grievance audit log.",
                    isCurrent = true
                ),
                TimelineNode(
                    status = ComplaintStatus.UNDER_ADMIN_REVIEW,
                    time = "Pending",
                    isFuture = true
                ),
                TimelineNode(
                    status = ComplaintStatus.RESOLVED,
                    time = "Pending",
                    isFuture = true
                )
            ),
            review = null
        )

        complaints = listOf(newComplaint) + complaints
        lastSubmittedComplaint = newComplaint

        // Background network sync
        val categorySnapshot = formCategory
        val severitySnapshot = formSeverity
        val locationSnapshot = formLocation
        val anonymousSnapshot = formAnonymous
        val photosSnapshot = formPhotos.toList()
        val fieldsSnapshot = HashMap(formFields)
        val studentsAffectedSnapshot = formStudentsAffected

        scope.launch {
            try {
                val result = repository.submitComplaint(
                    category = categorySnapshot,
                    severity = severitySnapshot,
                    location = locationSnapshot,
                    anonymous = anonymousSnapshot,
                    photos = photosSnapshot,
                    fields = fieldsSnapshot,
                    studentsAffected = studentsAffectedSnapshot
                )
                result.onSuccess { serverCreated ->
                    lastSubmittedComplaint = serverCreated
                    // Replace optimistic complaint with server created complaint
                    complaints = listOf(serverCreated) + complaints.filter { it.id != token }
                    triggerToast("Ticket ${serverCreated.id} submitted to campus queue!")
                }
                result.onFailure { err ->
                    android.util.Log.e("SentinelAppState", "Submit complaint failed: ${err.message}", err)
                    triggerToast("Server sync error: ${err.message}")
                }
            } catch (e: Exception) {
                android.util.Log.e("SentinelAppState", "Exception submitting complaint", e)
            }
        }

        return newComplaint
    }

    fun saveCurrentDraft() {
        val title = formFields["title"] ?: ""
        val desc = formFields["description"] ?: ""
        val draftId = "draft-${System.currentTimeMillis()}"
        val newDraft = Draft(
            id = draftId,
            categoryId = formCategory,
            title = if (title.isBlank()) "${formCategory.displayName} Draft" else title,
            description = desc,
            severity = formSeverity,
            location = formLocation,
            anonymous = formAnonymous,
            updatedAt = "Just now"
        )
        drafts = listOf(newDraft) + drafts
        triggerToast("Draft saved.")
    }

    fun deleteDraft(draftId: String) {
        drafts = drafts.filter { it.id != draftId }
    }

    fun submitReply(complaintId: String, replyText: String) {
        // Optimistic UI update
        complaints = complaints.map { complaint ->
            if (complaint.id == complaintId) {
                val updatedTimeline = complaint.timeline.map { node ->
                    if (node.status == ComplaintStatus.AWAITING_STUDENT_INFO) {
                        node.copy(isCurrent = false, note = "Student replied: \"$replyText\"")
                    } else node
                } + listOf(
                    TimelineNode(
                        status = ComplaintStatus.IN_PROGRESS,
                        time = "Just now",
                        note = "Student info received. Technician resumed investigation.",
                        isCurrent = true
                    )
                )
                complaint.copy(
                    status = ComplaintStatus.IN_PROGRESS,
                    timeline = updatedTimeline
                )
            } else complaint
        }

        // Background network call
        scope.launch {
            try {
                val result = repository.submitReply(complaintId, replyText)
                result.onSuccess { updatedComplaint ->
                    complaints = complaints.map {
                        if (it.id == updatedComplaint.id) updatedComplaint else it
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun submitReview(complaintId: String, stars: Int, comment: String) {
        // Optimistic UI update
        complaints = complaints.map { complaint ->
            if (complaint.id == complaintId) {
                val updatedTimeline = complaint.timeline.map { it.copy(isCurrent = false) } + listOf(
                    TimelineNode(
                        status = ComplaintStatus.CLOSED,
                        time = "Just now",
                        note = "Ticket closed following student review ($stars/5).",
                        isCurrent = true
                    )
                )
                complaint.copy(
                    status = ComplaintStatus.CLOSED,
                    review = Review(stars, comment),
                    timeline = updatedTimeline
                )
            } else complaint
        }

        // Background network call
        scope.launch {
            try {
                repository.submitReview(complaintId, stars, comment)
            } catch (_: Exception) {}
        }
    }

    fun reopenComplaint(complaintId: String) {
        // Optimistic UI update
        complaints = complaints.map { complaint ->
            if (complaint.id == complaintId) {
                val updatedTimeline = complaint.timeline.map { it.copy(isCurrent = false) } + listOf(
                    TimelineNode(
                        status = ComplaintStatus.REOPENED,
                        time = "Just now",
                        note = "Reopened by student. Priority escalated for follow-up.",
                        isCurrent = true
                    )
                )
                complaint.copy(
                    status = ComplaintStatus.REOPENED,
                    timeline = updatedTimeline
                )
            } else complaint
        }

        // Background network call
        scope.launch {
            try {
                repository.reopenComplaint(complaintId, null)
            } catch (_: Exception) {}
        }
    }

    fun markNotificationRead(notifId: String) {
        notifications = notifications.map {
            if (it.id == notifId) it.copy(isUnread = false) else it
        }
        scope.launch {
            try {
                repository.markNotificationRead(notifId)
            } catch (_: Exception) {}
        }
    }

    fun updateLocation(building: String, floor: String, room: String) {
        formLocation = "$building, $floor, $room"
    }

    fun updateUserProfile(name: String, hostelBlock: String, roomNo: String) {
        user = user.copy(name = name, hostelBlock = hostelBlock, roomNo = roomNo)
    }

    fun logout() {
        user = user.copy(isLoggedIn = false)
        complaints = emptyList()
        drafts = emptyList()
        notifications = emptyList()
        scope.launch {
            try {
                repository.logout()
            } catch (_: Exception) {}
        }
    }

    fun login(
        email: String,
        password: String = "student123",
        onResult: ((Boolean, String?) -> Unit)? = null
    ) {
        scope.launch {
            try {
                val res = repository.login(email, password)
                res.onSuccess { profile ->
                    user = profile
                    refreshData()
                    onResult?.invoke(true, null)
                }
                res.onFailure { err ->
                    onResult?.invoke(false, err.message ?: "Invalid credentials")
                }
            } catch (e: Exception) {
                onResult?.invoke(false, e.message ?: "Login failed")
            }
        }
    }

    fun signup(
        name: String,
        rollNo: String,
        email: String,
        password: String = "student123",
        department: String = "Computer Science and Engineering",
        onResult: (Boolean, String?) -> Unit
    ) {
        scope.launch {
            try {
                // Ensure state is clean for new account
                complaints = emptyList()
                drafts = emptyList()
                notifications = emptyList()
                val res = repository.signup(
                    name = name,
                    rollNo = rollNo,
                    email = email,
                    password = password,
                    department = department
                )
                res.onSuccess { profile ->
                    user = profile
                    onResult(true, null)
                }
                res.onFailure { err ->
                    onResult(false, err.message ?: "Registration failed")
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "Registration failed")
            }
        }
    }

    fun resetDemoData() {
        complaints = emptyList()
        drafts = emptyList()
        notifications = emptyList()
        user = UserProfile(isLoggedIn = false)
        formFields.clear()
        formPhotos.clear()
    }

    companion object {
        private fun getSeedComplaints(): List<Complaint> = listOf(
            Complaint(
                id = "SNT-2026-000482",
                categoryId = CategoryId.HOSTEL,
                title = "Bathroom tap leaking continuously in Wing B",
                description = "The main basin tap in Room 214 washroom is stripped and leaking non-stop. Water is accumulating on the bathroom floor.",
                severity = "High",
                location = "Hostel Block C, Room 214",
                status = ComplaintStatus.AWAITING_STUDENT_INFO,
                createdAt = "3 hours ago",
                timestamp = "2026-09-08 09:15 AM",
                anonymous = false,
                photos = listOf("https://images.unsplash.com/photo-1584622650111-993a426fbf0a?auto=format&fit=crop&w=300&q=80"),
                dispatch = DispatchInfo("internal", "Assigned to Plumbing Wing — K. Murugan", "Scheduled for inspection"),
                timeline = listOf(
                    TimelineNode(ComplaintStatus.SUBMITTED, "Today, 09:15 AM", note = "Ticket created and hashed into campus grievance log."),
                    TimelineNode(ComplaintStatus.UNDER_ADMIN_REVIEW, "Today, 10:00 AM", note = "Triaged by Hostel Warden Office."),
                    TimelineNode(ComplaintStatus.ASSIGNED, "Today, 10:45 AM", note = "Assigned to Plumbing Technician K. Murugan."),
                    TimelineNode(
                        ComplaintStatus.AWAITING_STUDENT_INFO,
                        "Today, 11:30 AM",
                        staffNote = "Plumber visited Room 214 at 11:15 AM but room was locked. Are you available between 3:00 PM and 5:00 PM today?",
                        isCurrent = true
                    ),
                    TimelineNode(ComplaintStatus.RESOLVED, "Pending", isFuture = true),
                    TimelineNode(ComplaintStatus.CLOSED, "Pending", isFuture = true)
                ),
                review = null
            ),
            Complaint(
                id = "SNT-2026-000419",
                categoryId = CategoryId.FOOD,
                title = "Foreign particle found in mess lunch sambar",
                description = "During lunch service at counter 2, metal wire scrubber piece was discovered in the serving vessel.",
                severity = "Critical",
                location = "Central Dining Hall / Mess",
                status = ComplaintStatus.RESOLVED,
                createdAt = "Yesterday",
                timestamp = "2026-09-07 01:20 PM",
                anonymous = false,
                photos = listOf("https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=300&q=80"),
                dispatch = DispatchInfo("vendor", "Food Safety Committee — Notice Issued to Caterer", "Kitchen audit completed"),
                timeline = listOf(
                    TimelineNode(ComplaintStatus.SUBMITTED, "Yesterday, 01:20 PM"),
                    TimelineNode(ComplaintStatus.UNDER_ADMIN_REVIEW, "Yesterday, 01:35 PM"),
                    TimelineNode(ComplaintStatus.IN_PROGRESS, "Yesterday, 02:00 PM", note = "Kitchen inspection initiated by Food Committee."),
                    TimelineNode(
                        ComplaintStatus.RESOLVED,
                        "Yesterday, 05:45 PM",
                        staffNote = "Batch #4 discarded. Caterer penalized ₹15,000 and steel wire scrubbers replaced with food-grade nylon sponges. Sanitization verified.",
                        isCurrent = true
                    ),
                    TimelineNode(ComplaintStatus.CLOSED, "Pending student rating", isFuture = true)
                ),
                review = null
            ),
            Complaint(
                id = "SNT-2026-000392",
                categoryId = CategoryId.TECH,
                title = "Wi-Fi AP offline in Library 2nd Floor Reading Room",
                description = "Access point AP-LIB-04 has been displaying red status LED. No devices able to connect to CampusNet.",
                severity = "Medium",
                location = "Central Library, 2nd Floor",
                status = ComplaintStatus.IN_PROGRESS,
                createdAt = "2 days ago",
                timestamp = "2026-09-06 04:30 PM",
                anonymous = false,
                photos = emptyList(),
                dispatch = DispatchInfo("internal", "Network Operations Center (NOC)", "Fiber switch replacement in progress"),
                timeline = listOf(
                    TimelineNode(ComplaintStatus.SUBMITTED, "Sep 6, 04:30 PM"),
                    TimelineNode(ComplaintStatus.UNDER_ADMIN_REVIEW, "Sep 6, 05:00 PM"),
                    TimelineNode(
                        ComplaintStatus.IN_PROGRESS,
                        "Sep 7, 10:00 AM",
                        note = "PoE switch port faulty. Replacement Gigabit PoE switch requisitioned from central store.",
                        isCurrent = true
                    ),
                    TimelineNode(ComplaintStatus.RESOLVED, "Pending", isFuture = true),
                    TimelineNode(ComplaintStatus.CLOSED, "Pending", isFuture = true)
                ),
                review = null
            ),
            Complaint(
                id = "SNT-2026-000311",
                categoryId = CategoryId.ACADEMIC,
                title = "CS302 Projector bulb dead in Hall B-12",
                description = "The ceiling-mounted Epson projector in Seminar Hall B-12 has a blown lamp. PPT slides cannot be projected for lecture.",
                severity = "Low",
                location = "Academic Block B, Room 12",
                status = ComplaintStatus.CLOSED,
                createdAt = "5 days ago",
                timestamp = "2026-09-03 11:00 AM",
                anonymous = false,
                photos = emptyList(),
                dispatch = DispatchInfo("internal", "AV & Multimedia Cell", "Completed"),
                timeline = listOf(
                    TimelineNode(ComplaintStatus.SUBMITTED, "Sep 3, 11:00 AM"),
                    TimelineNode(ComplaintStatus.UNDER_ADMIN_REVIEW, "Sep 3, 11:30 AM"),
                    TimelineNode(ComplaintStatus.IN_PROGRESS, "Sep 3, 02:00 PM"),
                    TimelineNode(ComplaintStatus.RESOLVED, "Sep 4, 10:30 AM", note = "Replacement OEM lamp installed. Display tested."),
                    TimelineNode(ComplaintStatus.CLOSED, "Sep 4, 01:00 PM", note = "Student rated 5 stars: \"Fixed very fast, lecture resumed without issue.\"", isCurrent = true)
                ),
                review = Review(5, "Fixed very fast, lecture resumed without issue.")
            )
        )

        private fun getSeedDrafts(): List<Draft> = listOf(
            Draft(
                id = "draft-1",
                categoryId = CategoryId.HYGIENE,
                title = "Dustbins overflowing near Badminton Court",
                description = "Waste has accumulated since Saturday evening...",
                severity = "Low",
                location = "Sports Complex, Court 2",
                anonymous = false,
                updatedAt = "2 hours ago"
            ),
            Draft(
                id = "draft-2",
                categoryId = CategoryId.TECH,
                title = "Lab 4 Workstation #12 GPU driver crash",
                description = "CUDA out of memory error on restart...",
                severity = "Medium",
                location = "CS Block, Lab 4",
                anonymous = false,
                updatedAt = "Yesterday"
            )
        )

        private fun getSeedNotifications(): List<NotificationItem> = listOf(
            NotificationItem(
                id = "notif-4",
                complaintId = "SNT-2026-000392",
                dateGroup = "Earlier",
                text = "Investigation initiated for Wi-Fi AP-LIB-04 failure",
                time = "Sep 7",
                status = ComplaintStatus.IN_PROGRESS,
                categoryId = CategoryId.TECH,
                isUnread = false
            ),
            NotificationItem(
                id = "notif-3",
                complaintId = "SNT-2026-000419",
                dateGroup = "Earlier",
                text = "Ticket SNT-2026-000419 marked RESOLVED. Please submit your review.",
                time = "Yesterday",
                status = ComplaintStatus.RESOLVED,
                categoryId = CategoryId.FOOD,
                isUnread = false
            ),
            NotificationItem(
                id = "notif-1",
                complaintId = "SNT-2026-000482",
                dateGroup = "Today",
                text = "Technician K. Murugan requested info: \"Are you in room between 3–5 PM?\"",
                time = "11:30 AM",
                status = ComplaintStatus.AWAITING_STUDENT_INFO,
                categoryId = CategoryId.HOSTEL,
                isUnread = true
            )
        )
    }
}
