package com.fixloop.mobile.data.repository

import android.content.Context
import android.util.Log
import com.fixloop.mobile.data.local.FixLoopDatabase
import com.fixloop.mobile.data.local.SessionManager
import com.fixloop.mobile.data.local.entity.CachedCategoryEntity
import com.fixloop.mobile.data.local.entity.CachedComplaintEntity
import com.fixloop.mobile.data.local.entity.CachedNotificationEntity
import com.fixloop.mobile.data.notification.SentinelNotificationHelper
import com.fixloop.mobile.data.remote.NetworkClient
import com.fixloop.mobile.data.remote.model.*
import com.fixloop.mobile.data.websocket.StompWebSocketClient
import com.fixloop.mobile.model.*
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

class FixLoopRepository(private val context: Context) {

    private val tag = "FixLoopRepository"
    private val db = FixLoopDatabase.getInstance(context)
    private val complaintDao = db.complaintDao()
    private val notificationDao = db.notificationDao()
    private val categoryDao = db.categoryDao()
    val sessionManager = SessionManager(context)

    private val api get() = NetworkClient.getApi(context)
    private val gson = NetworkClient.gson
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var stompClient: StompWebSocketClient? = null

    init {
        SentinelNotificationHelper.createNotificationChannels(context)
        initWebSocket()
    }

    private fun initWebSocket() {
        if (sessionManager.isLoggedIn()) {
            stompClient = StompWebSocketClient(context) { destination, body ->
                handleRealtimeUpdate(destination, body)
            }
            stompClient?.connect()
        }
    }

    private fun handleRealtimeUpdate(destination: String, body: String) {
        try {
            val jsonMap = gson.fromJson<Map<String, Any>>(body, object : TypeToken<Map<String, Any>>() {}.type)
            val complaintIdStr = jsonMap["complaintId"]?.toString()?.substringBefore(".") ?: ""
            val token = jsonMap["token"]?.toString() ?: ""
            val newStatus = jsonMap["newStatus"]?.toString() ?: ""
            val title = jsonMap["title"]?.toString() ?: "Status Update"
            val messageBody = jsonMap["body"]?.toString() ?: "Your complaint status has changed."

            Log.i(tag, "Real-time push event for complaint token=$token, newStatus=$newStatus")

            // 1. Optimistic Room update for complaint status
            if (token.isNotBlank() && newStatus.isNotBlank()) {
                repositoryScope.launch {
                    complaintDao.updateStatus(token, newStatus)
                }
            }

            // 2. Insert/update single notification row for this issue (stable ID prevents duplicate recurring cards)
            val notifId = if (token.isNotBlank()) "notif-$token" else "notif-realtime-${System.currentTimeMillis()}"
            val notifEntity = CachedNotificationEntity(
                id = notifId,
                backendId = null,
                complaintId = if (token.isNotBlank()) token else complaintIdStr,
                dateGroup = "Today",
                text = "$title: $messageBody",
                time = "Just now",
                status = newStatus,
                categoryIdString = "hostel",
                isUnread = true,
                type = "STATUS_CHANGE",
                createdAtMillis = System.currentTimeMillis()
            )
            repositoryScope.launch {
                notificationDao.insertOrUpdate(notifEntity)
            }

            // 3. Show heads-up system tray notification
            SentinelNotificationHelper.showNotification(
                context = context,
                notificationId = System.currentTimeMillis().toInt(),
                title = title,
                body = messageBody,
                complaintId = token
            )

            // 4. Background refresh to pull complete fresh state from backend
            repositoryScope.launch {
                if (token.isNotBlank()) {
                    fetchComplaintDetail(token)
                }
                refreshComplaints()
                refreshNotifications()
            }
        } catch (e: Exception) {
            Log.e(tag, "Error parsing real-time message: ${e.message}")
        }
    }

    // ── Reactive Data Streams (SSOT: Room DB -> Flow -> Compose) ───────────

    fun getComplaintsFlow(): Flow<List<Complaint>> {
        return complaintDao.getAllComplaintsFlow()
            .map { list -> list.map { entityToComplaint(it) } }
    }

    fun getNotificationsFlow(): Flow<List<NotificationItem>> {
        return notificationDao.getAllNotificationsFlow()
            .map { list -> list.map { entityToNotificationItem(it) } }
    }

    fun getUnreadNotificationsCountFlow(): Flow<Int> {
        return notificationDao.getUnreadCountFlow()
    }

    fun getUserProfile(): UserProfile {
        return UserProfile(
            name = sessionManager.getName(),
            rollNo = sessionManager.getRollNo(),
            email = sessionManager.getEmail(),
            department = sessionManager.getDepartment(),
            hostelBlock = sessionManager.getHostelBlock(),
            roomNo = sessionManager.getRoomNo(),
            isLoggedIn = sessionManager.isLoggedIn()
        )
    }

    // ── Network Refresh Functions ───────────────────────────────────────────

    suspend fun refreshAll() {
        coroutineScope {
            launch { refreshComplaints() }
            launch { refreshNotifications() }
            launch { refreshCategories() }
            launch { refreshProfile() }
        }
    }

    suspend fun refreshComplaints() {
        if (!sessionManager.isLoggedIn()) return
        try {
            val response = api.getMyComplaints(page = 0, size = 50)
            if (response.isSuccessful && response.body() != null) {
                val page = response.body()!!
                val entities = page.content.map { dto ->
                    val existing = complaintDao.getComplaintById(dto.token)
                    val timelineJson = if (existing != null && existing.timelineJson.length > 50) {
                        existing.timelineJson
                    } else {
                        gson.toJson(listOf(
                            TimelineNode(
                                status = ComplaintStatus.fromString(dto.status),
                                time = formatDateTime(dto.createdAt),
                                note = "Status: ${dto.status}",
                                isCurrent = true
                            )
                        ))
                    }
                    CachedComplaintEntity(
                        id = dto.token,
                        backendId = dto.id,
                        categoryIdString = mapCategoryNameToId(dto.categoryName),
                        categoryName = dto.categoryName,
                        title = dto.locationDescription ?: "${dto.categoryName} Grievance",
                        description = existing?.description ?: "Complaint registered under ${dto.categoryName}",
                        severity = mapSeverityIntToString(dto.severity),
                        location = dto.locationDescription ?: "Campus",
                        status = dto.status,
                        createdAt = formatDateTime(dto.createdAt),
                        timestamp = dto.createdAt ?: "",
                        anonymous = false,
                        photosJson = existing?.photosJson ?: "[]",
                        timelineJson = timelineJson,
                        isAtRisk = dto.isAtRisk ?: false,
                        isBreached = dto.isBreached ?: false,
                        priorityScore = dto.priorityScore
                    )
                }
                complaintDao.insertAll(entities)

                // Background fetch detailed status history for complaints requiring student interaction or active status
                for (dto in page.content) {
                    if (dto.status == "AWAITING_STUDENT_INFO" || dto.status == "IN_PROGRESS" || dto.status == "RESOLVED") {
                        repositoryScope.launch {
                            fetchComplaintDetail(dto.token)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Network error refreshing complaints: ${e.message}")
        }
    }

    suspend fun refreshNotifications() {
        if (!sessionManager.isLoggedIn()) return
        try {
            val response = api.getNotifications(filter = "all", page = 0, size = 50)
            if (response.isSuccessful && response.body() != null) {
                val page = response.body()!!
                // Group by complaintToken to consolidate multiple alerts on the same issue into ONE card
                val grouped = page.content.groupBy {
                    if (!it.complaintToken.isNullOrBlank()) it.complaintToken else "notif-${it.id}"
                }
                val entities = grouped.map { (key, dtoList) ->
                    val actionDto = dtoList.find { it.type == "AWAITING_INFO" || it.statusAtSend == "AWAITING_STUDENT_INFO" }
                    val latestDto = dtoList.maxByOrNull { it.id } ?: dtoList.first()
                    val repDto = actionDto ?: latestDto
                    val anyUnread = dtoList.any { !it.read }
                    val id = if (!repDto.complaintToken.isNullOrBlank()) "notif-${repDto.complaintToken}" else repDto.id.toString()
                    val earliestId = dtoList.minByOrNull { it.id }?.id ?: repDto.id

                    CachedNotificationEntity(
                        id = id,
                        backendId = repDto.id,
                        complaintId = repDto.complaintToken ?: "",
                        dateGroup = "Today",
                        text = "${repDto.title}: ${repDto.body}",
                        time = formatDateTime(repDto.createdAt),
                        status = repDto.statusAtSend ?: "SUBMITTED",
                        categoryIdString = "hostel",
                        isUnread = anyUnread,
                        type = repDto.type,
                        createdAtMillis = earliestId.toLong() * 1000L
                    )
                }
                notificationDao.clearAll()
                notificationDao.insertAll(entities)
            }
        } catch (e: Exception) {
            Log.w(tag, "Network error refreshing notifications: ${e.message}")
        }
    }

    suspend fun refreshCategories() {
        try {
            val response = api.getCategories()
            if (response.isSuccessful && response.body() != null) {
                val categories = response.body()!!
                val entities = categories.map { dto ->
                    CachedCategoryEntity(
                        id = dto.id,
                        name = dto.name,
                        icon = dto.icon,
                        color = dto.color,
                        sensitive = dto.sensitive,
                        defaultSlaHours = dto.defaultSlaHours,
                        filterGroup = dto.filterGroup,
                        formSchemaJson = dto.formSchema
                    )
                }
                categoryDao.insertAll(entities)
            }
        } catch (e: Exception) {
            Log.w(tag, "Network error refreshing categories: ${e.message}")
        }
    }

    suspend fun refreshProfile() {
        if (!sessionManager.isLoggedIn()) return
        try {
            val response = api.getMyProfile()
            if (response.isSuccessful && response.body() != null) {
                sessionManager.updateProfile(response.body()!!)
            }
        } catch (e: Exception) {
            Log.w(tag, "Network error refreshing profile: ${e.message}")
        }
    }

    // ── Auth Actions ────────────────────────────────────────────────────────

    suspend fun login(email: String, pass: String): Result<UserProfile> {
        return try {
            val response = api.login(LoginRequest(email.trim(), pass))
            if (response.isSuccessful && response.body() != null) {
                val loginRes = response.body()!!
                sessionManager.saveAuth(loginRes.token, loginRes.user)

                // Connect WebSocket
                stompClient?.disconnect()
                initWebSocket()

                // Refresh fresh data immediately in parallel
                refreshAll()

                Result.success(getUserProfile())
            } else {
                val errorBody = response.errorBody()?.string() ?: "Invalid credentials"
                Result.failure(Exception(errorBody))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signup(
        name: String,
        rollNo: String,
        email: String,
        pass: String,
        deptId: Int? = null,
        locationId: Int? = null
    ): Result<UserProfile> {
        return try {
            // Clean local cache first so no previous user data leaks into new account
            complaintDao.clearAll()
            notificationDao.clearAll()

            val response = api.signup(SignupRequest(name, rollNo, email, pass, deptId, locationId))
            if (response.isSuccessful && response.body() != null) {
                val loginRes = response.body()!!
                sessionManager.saveAuth(loginRes.token, loginRes.user)

                stompClient?.disconnect()
                initWebSocket()
                refreshAll()

                Result.success(getUserProfile())
            } else {
                val errorBody = response.errorBody()?.string() ?: "Signup failed"
                Result.failure(Exception(errorBody))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signup(
        name: String,
        rollNo: String,
        email: String,
        password: String,
        department: String
    ): Result<UserProfile> {
        val deptId = when {
            department.contains("Computer", ignoreCase = true) -> 1
            department.contains("Electrical", ignoreCase = true) -> 2
            department.contains("Mechanical", ignoreCase = true) -> 3
            department.contains("Civil", ignoreCase = true) -> 4
            department.contains("Biotech", ignoreCase = true) -> 5
            else -> 1
        }
        return signup(
            name = name,
            rollNo = rollNo,
            email = email,
            pass = password,
            deptId = deptId,
            locationId = 2
        )
    }

    fun logout() {
        sessionManager.clearSession()
        stompClient?.disconnect()
        stompClient = null
        repositoryScope.launch {
            complaintDao.clearAll()
            notificationDao.clearAll()
        }
    }

    // ── Complaint Mutations with Optimistic Updates ─────────────────────────

    suspend fun submitComplaint(
        category: CategoryId,
        severity: String,
        location: String,
        anonymous: Boolean,
        photos: List<String>,
        fields: Map<String, String>,
        studentsAffected: Int = 1
    ): Result<Complaint> {
        return try {
            val catIdInt = mapCategoryIdToInt(category)
            val severityInt = when (severity.lowercase()) {
                "low" -> 1
                "medium" -> 2
                "high" -> 3
                else -> 4
            }

            val locId = when {
                location.contains("Hostel Block A", ignoreCase = true) -> 2
                location.contains("Hostel Block B", ignoreCase = true) -> 5
                location.contains("Hostel", ignoreCase = true) -> 2
                location.contains("Academic", ignoreCase = true) || location.contains("CS", ignoreCase = true) || location.contains("Lab", ignoreCase = true) -> 3
                location.contains("Cafeteria", ignoreCase = true) || location.contains("Mess", ignoreCase = true) || location.contains("Dining", ignoreCase = true) -> 4
                location.contains("Health", ignoreCase = true) || location.contains("Medical", ignoreCase = true) -> 1
                category == CategoryId.HOSTEL -> 2
                category == CategoryId.ACADEMIC || category == CategoryId.TECH -> 3
                category == CategoryId.FOOD -> 4
                category == CategoryId.HEALTH -> 1
                else -> 1
            }

            val enrichedFields = HashMap(fields)
            enrichedFields["location"] = location

            val request = ComplaintCreateRequest(
                categoryId = catIdInt,
                locationId = locId,
                severity = severityInt,
                isAnonymous = anonymous,
                studentsAffected = studentsAffected,
                extraData = enrichedFields
            )

            val response = api.createComplaint(request)
            if (response.isSuccessful && response.body() != null) {
                val res = response.body()!!

                val customSpec = fields.entries.firstOrNull { it.key.startsWith("custom_") && it.value.isNotBlank() }?.value
                val resolvedTitle = fields["title"]
                    ?: fields["condition_summary"]
                    ?: customSpec
                    ?: fields["description"]
                    ?: "${category.displayName} Grievance"

                val resolvedDesc = fields["description"]
                    ?: fields["detailed_description"]
                    ?: if (!customSpec.isNullOrBlank()) "Custom issue: $customSpec" else "Grievance submitted by student."

                val newComplaint = Complaint(
                    id = res.token,
                    categoryId = category,
                    title = resolvedTitle,
                    description = resolvedDesc,
                    severity = severity,
                    location = location,
                    status = ComplaintStatus.fromString(res.status),
                    createdAt = "Just now",
                    timestamp = res.createdAt ?: "",
                    anonymous = anonymous,
                    photos = photos,
                    dispatch = null,
                    timeline = listOf(
                        TimelineNode(
                            status = ComplaintStatus.SUBMITTED,
                            time = "Just now",
                            note = "Ticket registered and encrypted into campus grievance audit log.",
                            isCurrent = true
                        )
                    ),
                    review = null
                )

                // Save to Room cache
                complaintDao.insertOrUpdate(complaintToEntity(newComplaint, res.id))

                Result.success(newComplaint)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Submission failed"
                Result.failure(Exception(errorBody))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchComplaintDetail(idOrToken: String): Result<Complaint> {
        return try {
            val response = api.getComplaintDetail(idOrToken)
            if (response.isSuccessful && response.body() != null) {
                val detail = response.body()!!
                val entity = detailToEntity(detail)
                complaintDao.insertOrUpdate(entity)
                Result.success(detailToComplaint(detail))
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Failed to fetch complaint detail"
                Log.w(tag, "fetchComplaintDetail error: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(tag, "fetchComplaintDetail exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun submitReply(complaintToken: String, replyText: String): Result<Complaint> {
        // Optimistic local update
        complaintDao.updateStatus(complaintToken, "IN_PROGRESS")

        return try {
            val response = api.replyToInfoRequest(complaintToken, ReplyRequest(replyText))
            if (response.isSuccessful && response.body() != null) {
                val detail = response.body()!!
                val entity = detailToEntity(detail)
                complaintDao.insertOrUpdate(entity)
                refreshNotifications()
                Result.success(detailToComplaint(detail))
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Failed to submit reply"
                Log.e(tag, "submitReply error: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(tag, "submitReply exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun submitReview(complaintToken: String, stars: Int, comment: String): Result<Unit> {
        // Optimistic local update
        complaintDao.updateStatus(complaintToken, "CLOSED")

        return try {
            val response = api.submitReview(complaintToken, ReviewRequest(stars, comment))
            if (response.isSuccessful && response.body() != null) {
                val detail = response.body()!!
                complaintDao.insertOrUpdate(detailToEntity(detail))
            }
            refreshComplaints()
            refreshNotifications()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reopenComplaint(complaintToken: String, reason: String?): Result<Unit> {
        // Optimistic local update
        complaintDao.updateStatus(complaintToken, "REOPENED")

        return try {
            val response = api.reopenComplaint(complaintToken, ReopenRequest(reason))
            if (response.isSuccessful && response.body() != null) {
                val detail = response.body()!!
                complaintDao.insertOrUpdate(detailToEntity(detail))
            }
            refreshComplaints()
            refreshNotifications()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markNotificationRead(notifId: String) {
        notificationDao.markAsRead(notifId)
        notifId.toIntOrNull()?.let { intId ->
            try {
                api.markNotificationRead(intId)
            } catch (e: Exception) {
                Log.w(tag, "Failed to sync notification read status: ${e.message}")
            }
        }
    }

    suspend fun markAllNotificationsRead() {
        notificationDao.markAllAsRead()
        try {
            api.markAllNotificationsRead()
        } catch (e: Exception) {
            Log.w(tag, "Failed to sync mark-all-read: ${e.message}")
        }
    }

    // ── Entity / Model Conversions ──────────────────────────────────────────

    private fun entityToComplaint(e: CachedComplaintEntity): Complaint {
        val timeline = try {
            gson.fromJson<List<TimelineNode>>(e.timelineJson, object : TypeToken<List<TimelineNode>>() {}.type)
                ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
        val photos = try {
            gson.fromJson<List<String>>(e.photosJson, object : TypeToken<List<String>>() {}.type)
                ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        return Complaint(
            id = e.id,
            categoryId = CategoryId.fromId(e.categoryIdString),
            title = e.title,
            description = e.description,
            severity = e.severity,
            location = e.location,
            status = ComplaintStatus.fromString(e.status),
            createdAt = e.createdAt,
            timestamp = e.timestamp,
            anonymous = e.anonymous,
            photos = photos,
            dispatch = null,
            timeline = timeline,
            review = null
        )
    }

    private suspend fun detailToEntity(dto: ComplaintDetailDto): CachedComplaintEntity {
        val existing = complaintDao.getComplaintById(dto.token)
        val timelineNodes = if (!dto.statusHistory.isNullOrEmpty()) {
            dto.statusHistory.mapIndexed { index, h ->
                val isCurrent = index == dto.statusHistory.lastIndex
                val status = ComplaintStatus.fromString(h.status)
                val staffNote = if (h.status == "AWAITING_STUDENT_INFO" || (!h.note.isNullOrBlank() && (h.actorRole == "STAFF" || h.actorRole == "ADMIN"))) {
                    h.note
                } else {
                    null
                }
                val noteText = when {
                    h.status == "AWAITING_STUDENT_INFO" -> "Information requested by ${h.actorName ?: "Faculty/Staff"}"
                    h.actorRole == "STUDENT" -> h.note ?: "Information provided by student"
                    else -> h.note ?: "Status updated to ${status.label}"
                }
                TimelineNode(
                    status = status,
                    time = formatDateTime(h.createdAt),
                    note = noteText,
                    staffNote = staffNote,
                    isCurrent = isCurrent
                )
            }
        } else {
            listOf(
                TimelineNode(
                    status = ComplaintStatus.fromString(dto.status),
                    time = formatDateTime(dto.createdAt),
                    note = "Status: ${dto.status}",
                    isCurrent = true
                )
            )
        }

        val locationText = dto.location?.let {
            "${it.building}, ${it.floor}, ${it.room}"
        } ?: dto.locationDescription ?: "Campus"

        return CachedComplaintEntity(
            id = dto.token,
            backendId = dto.id,
            categoryIdString = mapCategoryNameToId(dto.categoryName),
            categoryName = dto.categoryName,
            title = dto.locationDescription ?: "${dto.categoryName} Grievance",
            description = dto.description ?: existing?.description ?: "Complaint registered under ${dto.categoryName}",
            severity = mapSeverityIntToString(dto.severity),
            location = locationText,
            status = dto.status,
            createdAt = formatDateTime(dto.createdAt),
            timestamp = dto.createdAt ?: "",
            anonymous = dto.anonymous,
            photosJson = existing?.photosJson ?: "[]",
            timelineJson = gson.toJson(timelineNodes),
            isAtRisk = dto.isAtRisk ?: false,
            isBreached = dto.isBreached ?: false,
            priorityScore = dto.priorityScore
        )
    }

    private suspend fun detailToComplaint(dto: ComplaintDetailDto): Complaint {
        val entity = detailToEntity(dto)
        val baseComplaint = entityToComplaint(entity)
        val review = dto.review?.let {
            Review(stars = it.rating.toInt(), comment = it.comment ?: "")
        }
        return baseComplaint.copy(review = review)
    }

    private fun complaintToEntity(c: Complaint, backendId: Int): CachedComplaintEntity {
        return CachedComplaintEntity(
            id = c.id,
            backendId = backendId,
            categoryIdString = c.categoryId.idString,
            categoryName = c.categoryId.displayName,
            title = c.title,
            description = c.description,
            severity = c.severity,
            location = c.location,
            status = c.status.name,
            createdAt = c.createdAt,
            timestamp = c.timestamp,
            anonymous = c.anonymous,
            photosJson = gson.toJson(c.photos),
            timelineJson = gson.toJson(c.timeline)
        )
    }

    private fun entityToNotificationItem(e: CachedNotificationEntity): NotificationItem {
        return NotificationItem(
            id = e.id,
            complaintId = e.complaintId,
            dateGroup = e.dateGroup,
            text = e.text,
            time = e.time,
            status = ComplaintStatus.fromString(e.status),
            categoryId = CategoryId.fromId(e.categoryIdString),
            isUnread = e.isUnread
        )
    }

    private fun mapCategoryNameToId(name: String): String {
        return when (name.lowercase()) {
            "academic" -> "academic"
            "hostel" -> "hostel"
            "food", "catering & food" -> "food"
            "health" -> "health"
            "hygiene" -> "hygiene"
            "tech support", "it & systems" -> "tech"
            "welfare", "student welfare" -> "welfare"
            "missing items" -> "missing_items"
            "ragging", "anti-ragging" -> "ragging"
            "harassment" -> "harassment"
            "malpractice" -> "malpractice"
            else -> "hostel"
        }
    }

    private fun mapCategoryIdToInt(cat: CategoryId): Int {
        return when (cat) {
            CategoryId.HOSTEL -> 10
            CategoryId.FOOD -> 11
            CategoryId.ACADEMIC -> 12
            CategoryId.HEALTH -> 13
            CategoryId.HYGIENE -> 14
            CategoryId.TECH -> 15
            CategoryId.HARASSMENT -> 16
            CategoryId.MALPRACTICE -> 17
            CategoryId.RAGGING -> 18
            CategoryId.WELFARE -> 10
            CategoryId.MISSING_ITEMS -> 10
        }
    }

    private fun mapSeverityIntToString(s: Int): String {
        return when (s) {
            1 -> "Low"
            2 -> "Medium"
            3 -> "High"
            else -> "Critical"
        }
    }

    private fun formatDateTime(isoString: String?): String {
        if (isoString.isNullOrBlank()) return "Recently"
        return try {
            val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault()).parse(isoString.take(16))
            SimpleDateFormat("MMM d, hh:mm a", Locale.getDefault()).format(date ?: Date())
        } catch (_: Exception) {
            isoString.take(10)
        }
    }
}
