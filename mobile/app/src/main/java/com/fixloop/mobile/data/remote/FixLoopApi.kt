package com.fixloop.mobile.data.remote

import com.fixloop.mobile.data.remote.model.*
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface FixLoopApi {

    // ── Auth Endpoints ──────────────────────────────────────────────────────

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/auth/signup")
    suspend fun signup(@Body request: SignupRequest): Response<LoginResponse>

    @GET("api/auth/health")
    suspend fun health(): Response<ResponseBody>

    // ── Student Profile Endpoints ───────────────────────────────────────────

    @GET("api/students/me")
    suspend fun getMyProfile(): Response<StudentProfileDto>

    @PATCH("api/students/me")
    suspend fun updateMyProfile(@Body request: UpdateProfileRequest): Response<StudentProfileDto>

    @GET("api/students/me/complaints")
    suspend fun getMyComplaints(
        @Query("status") status: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): Response<PageResponse<ComplaintSummaryDto>>

    // ── Categories & Locations ──────────────────────────────────────────────

    @GET("api/categories")
    suspend fun getCategories(): Response<List<CategoryDto>>

    @GET("api/locations/buildings")
    suspend fun getBuildings(): Response<List<String>>

    @GET("api/locations")
    suspend fun getLocations(
        @Query("building") building: String? = null,
        @Query("floor") floor: String? = null
    ): Response<List<LocationDto>>

    // ── Complaints CRUD & Lifecycle ─────────────────────────────────────────

    @POST("api/complaints")
    suspend fun createComplaint(@Body request: ComplaintCreateRequest): Response<ComplaintCreatedDto>

    @GET("api/complaints/{idOrToken}")
    suspend fun getComplaintDetail(@Path("idOrToken") idOrToken: String): Response<ComplaintDetailDto>

    @GET("api/complaints/{id}/history")
    suspend fun getComplaintHistory(@Path("id") id: Int): Response<List<StatusHistoryEntryDto>>

    @Multipart
    @POST("api/complaints/{id}/attachments")
    suspend fun uploadAttachment(
        @Path("id") id: Int,
        @Part file: MultipartBody.Part
    ): Response<AttachmentDto>

    @POST("api/complaints/{id}/reply")
    suspend fun replyToInfoRequest(
        @Path("id") id: String,
        @Body request: ReplyRequest
    ): Response<ComplaintDetailDto>

    @POST("api/complaints/{id}/review")
    suspend fun submitReview(
        @Path("id") id: String,
        @Body request: ReviewRequest
    ): Response<ComplaintDetailDto>

    @POST("api/complaints/{id}/reopen")
    suspend fun reopenComplaint(
        @Path("id") id: String,
        @Body request: ReopenRequest
    ): Response<ComplaintDetailDto>

    // ── Notifications ───────────────────────────────────────────────────────

    @GET("api/notifications")
    suspend fun getNotifications(
        @Query("filter") filter: String = "all",
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): Response<PageResponse<NotificationDto>>

    @PATCH("api/notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: Int): Response<Map<String, String>>

    @PATCH("api/notifications/read-all")
    suspend fun markAllNotificationsRead(): Response<Map<String, Any>>

    // ── Devices (Push) ──────────────────────────────────────────────────────

    @POST("api/devices/register")
    suspend fun registerDevice(@Body request: DeviceRegisterRequest): Response<Map<String, String>>

    @DELETE("api/devices/{fcmToken}")
    suspend fun unregisterDevice(@Path("fcmToken") fcmToken: String): Response<Map<String, String>>
}
