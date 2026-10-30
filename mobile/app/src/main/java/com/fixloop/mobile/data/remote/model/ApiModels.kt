package com.fixloop.mobile.data.remote.model

import com.google.gson.annotations.SerializedName

// ── Auth Models ─────────────────────────────────────────────────────────────

data class LoginRequest(
    val email: String,
    val password: String
)

data class SignupRequest(
    val name: String,
    val rollNo: String,
    val email: String,
    val password: String,
    val departmentId: Int? = null,
    val defaultLocationId: Int? = null
)

data class LoginResponse(
    val token: String,
    val type: String,
    val expiresIn: Long,
    val user: AuthUserDto
)

data class AuthUserDto(
    val id: Int,
    val name: String,
    val email: String,
    val role: String,
    val restrictedAccess: Boolean,
    val avatarInitials: String?
)

// ── Student Profile Models ──────────────────────────────────────────────────

data class StudentProfileDto(
    val userId: Int,
    val name: String?,
    val email: String?,
    val rollNo: String?,
    val avatarInitials: String?,
    val departmentId: Int?,
    val departmentName: String?,
    val defaultLocationId: Int?,
    val defaultLocationBuilding: String?,
    val defaultLocationRoom: String?,
    val createdAt: String?
)

data class UpdateProfileRequest(
    val name: String? = null,
    val defaultLocationId: Int? = null
)

// ── Category & Location Models ──────────────────────────────────────────────

data class CategoryDto(
    val id: Int,
    val name: String,
    val icon: String?,
    val color: String?,
    val sensitive: Boolean,
    val baseSeverityWeight: Double?,
    val defaultSlaHours: Int?,
    val departmentId: Int?,
    val formSchema: String?,
    val requiredSkillIds: List<Int>?,
    val requiredSkillNames: List<String>?,
    val filterGroup: String?
)

data class LocationDto(
    val id: Int,
    val building: String,
    val floor: String,
    val room: String,
    val campusZone: String?
)

// ── Complaint Models ────────────────────────────────────────────────────────

data class ComplaintCreateRequest(
    val categoryId: Int,
    val locationId: Int,
    val severity: Int, // 1 to 4
    val isAnonymous: Boolean,
    val studentsAffected: Int? = 1,
    val extraData: Map<String, Any?>? = null
)

data class ComplaintCreatedDto(
    val id: Int,
    val token: String,
    val categoryId: Int,
    val categoryName: String,
    val status: String,
    val priorityScore: Double?,
    val slaDeadline: String?,
    val createdAt: String?
)

data class ComplaintSummaryDto(
    val id: Int,
    val token: String,
    val categoryName: String,
    val categoryIcon: String?,
    val categoryColor: String?,
    val status: String,
    val severity: Int,
    val locationDescription: String?,
    val priorityScore: Double?,
    val isAtRisk: Boolean?,
    val isBreached: Boolean?,
    val createdAt: String?,
    val updatedAt: String?
)

data class ComplaintDetailDto(
    val id: Int,
    val token: String,
    val categoryName: String,
    val categoryIcon: String?,
    val categoryColor: String?,
    val status: String,
    val severity: Int,
    val locationDescription: String?,
    val priorityScore: Double?,
    val isAtRisk: Boolean?,
    val isBreached: Boolean?,
    val createdAt: String?,
    val updatedAt: String?,
    val description: String?,
    @SerializedName("isAnonymous", alternate = ["anonymous"])
    val anonymous: Boolean = false,
    val extraData: String?,
    val student: StudentSummaryDto?,
    val location: LocationDto?,
    @SerializedName("assignedToStaff", alternate = ["assignedStaff"])
    val assignedStaff: StaffSummaryDto?,
    val dispatchType: String?,
    val statusHistory: List<StatusHistoryEntryDto>?,
    val attachments: List<AttachmentDto>?,
    val review: ReviewDto? = null
)

data class ReviewDto(
    val id: Int,
    val rating: Short,
    val comment: String?,
    val createdAt: String?
)

data class StudentSummaryDto(
    val id: Int,
    val name: String?,
    val email: String?,
    val avatarInitials: String?
)

data class StaffSummaryDto(
    val id: Int,
    val name: String?,
    val department: String?,
    val jobTitle: String?
)

data class StatusHistoryEntryDto(
    val id: Int,
    val status: String,
    @SerializedName("changedBy", alternate = ["actorName"])
    val actorName: String? = null,
    @SerializedName("changedByRole", alternate = ["actorRole"])
    val actorRole: String? = null,
    val note: String? = null,
    @SerializedName("timestamp", alternate = ["createdAt"])
    val createdAt: String? = null
)

data class AttachmentDto(
    val id: Int,
    val fileName: String,
    val fileType: String?,
    val fileSize: Long?,
    val uploadedAt: String?
)

data class PageResponse<T>(
    val content: List<T>,
    val totalElements: Long,
    val totalPages: Int,
    val size: Int,
    val number: Int
)

data class ReplyRequest(
    val replyText: String
)

data class ReviewRequest(
    val rating: Int,
    val comment: String?
)

data class ReopenRequest(
    val reason: String?
)

// ── Notification & Device Models ────────────────────────────────────────────

data class NotificationDto(
    val id: Int,
    val complaintId: Int?,
    val complaintToken: String?,
    val type: String,
    val title: String,
    val body: String,
    val statusAtSend: String?,
    val read: Boolean,
    val createdAt: String?
)

data class DeviceRegisterRequest(
    val fcmToken: String,
    val platform: String = "ANDROID"
)
