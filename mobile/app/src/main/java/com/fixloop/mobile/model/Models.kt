package com.fixloop.mobile.model

import androidx.compose.ui.graphics.Color
import com.fixloop.mobile.ui.theme.*

enum class CategoryId(
    val idString: String,
    val displayName: String,
    val color: Color,
    val isSensitive: Boolean
) {
    ACADEMIC("academic", "Academic", CatAcademic, false),
    HOSTEL("hostel", "Hostel", CatHostel, false),
    FOOD("food", "Food", CatFood, false),
    HEALTH("health", "Health", CatHealth, false),
    HYGIENE("hygiene", "Hygiene", CatHygiene, false),
    TECH("tech", "Tech Support", CatTech, false),
    WELFARE("welfare", "Student Welfare", CatWelfare, false),
    MISSING_ITEMS("missing_items", "Missing Items", CatMissing, false),
    RAGGING("ragging", "Anti-Ragging", CatRagging, true),
    HARASSMENT("harassment", "Harassment", CatHarassment, true),
    MALPRACTICE("malpractice", "Malpractice", CatMalpractice, true);

    companion object {
        fun fromId(id: String): CategoryId =
            values().find { it.idString.equals(id, ignoreCase = true) } ?: HOSTEL
    }
}

enum class ComplaintStatus(
    val label: String,
    val color: Color,
    val textColor: Color = Ink
) {
    SUBMITTED("Submitted", StatusSubmitted, Color.White),
    UNDER_ADMIN_REVIEW("Under review", StatusReview, Color.White),
    ASSIGNED("Assigned", StatusAssigned, Color.White),
    IN_PROGRESS("In progress", StatusProgress, Ink),
    AWAITING_STUDENT_INFO("Needs you", StatusAwaiting, Color.White),
    VENDOR_ASSIGNED("Vendor assigned", StatusVendor, Color.White),
    RESOLVED("Resolved", StatusResolved, Color.White),
    REOPENED("Reopened", StatusReopened, Color.White),
    CLOSED("Closed", StatusClosed, Color.White);

    companion object {
        fun fromString(str: String): ComplaintStatus =
            values().find { it.name.equals(str, ignoreCase = true) } ?: SUBMITTED
    }
}

data class TimelineNode(
    val status: ComplaintStatus,
    val time: String,
    val note: String? = null,
    val staffNote: String? = null,
    val isCurrent: Boolean = false,
    val isFuture: Boolean = false
)

data class DispatchInfo(
    val type: String, // "internal" or "vendor"
    val text: String,
    val subText: String
)

data class Review(
    val stars: Int,
    val comment: String
)

data class Complaint(
    val id: String,
    val categoryId: CategoryId,
    val title: String,
    val description: String,
    val severity: String, // "Low", "Medium", "High", "Critical"
    val location: String,
    val status: ComplaintStatus,
    val createdAt: String,
    val timestamp: String,
    val anonymous: Boolean = false,
    val photos: List<String> = emptyList(),
    val dispatch: DispatchInfo? = null,
    val timeline: List<TimelineNode> = emptyList(),
    val review: Review? = null
)

data class Draft(
    val id: String,
    val categoryId: CategoryId,
    val title: String,
    val description: String,
    val severity: String,
    val location: String,
    val anonymous: Boolean,
    val updatedAt: String
)

data class NotificationItem(
    val id: String,
    val complaintId: String,
    val dateGroup: String, // "Today", "Earlier"
    val text: String,
    val time: String,
    val status: ComplaintStatus,
    val categoryId: CategoryId,
    val isUnread: Boolean
)

data class UserProfile(
    val name: String = "",
    val rollNo: String = "",
    val email: String = "",
    val department: String = "",
    val hostelBlock: String = "",
    val roomNo: String = "",
    val isLoggedIn: Boolean = false
)

data class FormFieldSchema(
    val key: String,
    val label: String,
    val type: FieldType,
    val options: List<String> = emptyList(),
    val placeholder: String = ""
)

enum class FieldType {
    TEXT,
    TEXTAREA,
    SELECT
}

object FormSchemas {
    fun getSchema(category: CategoryId): List<FormFieldSchema> = when (category) {
        CategoryId.HOSTEL -> listOf(
            FormFieldSchema("issue_type", "Issue type", FieldType.SELECT, listOf("Plumbing / Tap leak", "Electrical (Fan/Switch/Light)", "Broken Furniture (Bed/Desk)", "Pest control / Termites", "Door lock / Window latch", "Other (Specify below)")),
            FormFieldSchema("title", "Short summary", FieldType.TEXT, placeholder = "e.g. Bathroom tap leaking continuously"),
            FormFieldSchema("description", "Detailed description", FieldType.TEXTAREA, placeholder = "Explain what happened, since when, and exact room spot...")
        )
        CategoryId.ACADEMIC -> listOf(
            FormFieldSchema("course_code", "Course code / Subject", FieldType.TEXT, placeholder = "e.g. CS302 - Operating Systems"),
            FormFieldSchema("issue_type", "Issue type", FieldType.SELECT, listOf("Lab equipment not functional", "Timetable slot overlap", "Classroom projector / AC failure", "Attendance portal mismatch", "Course material missing", "Other (Specify below)")),
            FormFieldSchema("title", "Summary", FieldType.TEXT, placeholder = "e.g. Lab 3 projector dead during practical"),
            FormFieldSchema("description", "Detailed description", FieldType.TEXTAREA, placeholder = "Provide details about the session, lab number, and faculty in charge...")
        )
        CategoryId.FOOD -> listOf(
            FormFieldSchema("mess_hall", "Mess / Dining hall", FieldType.SELECT, listOf("Central Dining Hall (Ground Floor)", "Annapurna Mess (Hostel C)", "North Campus Food Court", "Night Canteen Block B", "Other (Specify below)")),
            FormFieldSchema("meal_time", "Meal session", FieldType.SELECT, listOf("Breakfast", "Lunch", "Evening Snacks", "Dinner")),
            FormFieldSchema("issue_type", "Issue category", FieldType.SELECT, listOf("Foreign object in food", "Hygiene / Unclean utensils", "Food shortage / Quality spoiled", "Overcharging / Billing issue", "Other (Specify below)")),
            FormFieldSchema("title", "Summary", FieldType.TEXT, placeholder = "e.g. Undercooked rice during lunch service"),
            FormFieldSchema("description", "Detailed description", FieldType.TEXTAREA, placeholder = "Detail the food counter, time of dining, and batch...")
        )
        CategoryId.HEALTH -> listOf(
            FormFieldSchema("incident_type", "Medical request type", FieldType.SELECT, listOf("First aid & dressing", "High fever / Infection cluster", "Allergic reaction / Food issue", "Emergency ambulance / Transport", "Other (Specify below)")),
            FormFieldSchema("patient_status", "Is the student able to walk?", FieldType.SELECT, listOf("Yes - Can visit health center", "No - Bedridden / Needs room visit", "Urgent emergency")),
            FormFieldSchema("title", "Condition summary", FieldType.TEXT, placeholder = "e.g. Severe sprain during sports match"),
            FormFieldSchema("description", "Symptoms & situation", FieldType.TEXTAREA, placeholder = "List symptoms, allergies if known, and immediate assistance needed...")
        )
        CategoryId.HYGIENE -> listOf(
            FormFieldSchema("area_type", "Location type", FieldType.SELECT, listOf("Washroom & Toilets", "Drinking Water Cooler", "Corridor Waste Bins", "Drainage & Sewage overflow", "Common Room / Lounge", "Other (Specify below)")),
            FormFieldSchema("issue_type", "Hygiene issue", FieldType.SELECT, listOf("Water supply stopped", "Dustbin overflowing with waste", "Unsanitary washroom condition", "Filter replacement overdue", "Stagnant water pest risk", "Other (Specify below)")),
            FormFieldSchema("title", "Summary", FieldType.TEXT, placeholder = "e.g. 2nd floor water purifier filter red indicator"),
            FormFieldSchema("description", "Description", FieldType.TEXTAREA, placeholder = "Specify wing, floor, and when cleaning was last witnessed...")
        )
        CategoryId.TECH -> listOf(
            FormFieldSchema("service_type", "Tech service", FieldType.SELECT, listOf("Hostel Wi-Fi access point", "Campus ERP / Student portal bug", "Library computer workstation", "Classroom smart board audio/video", "Other (Specify below)")),
            FormFieldSchema("device_model", "Device / IP details (optional)", FieldType.TEXT, placeholder = "e.g. Laptop MAC address or PC #14"),
            FormFieldSchema("title", "Summary", FieldType.TEXT, placeholder = "e.g. Wi-Fi AP-C2-04 frequent disconnection"),
            FormFieldSchema("description", "Detailed symptoms", FieldType.TEXTAREA, placeholder = "Describe exact error messages, speed test results, or reboot attempts...")
        )
        CategoryId.RAGGING -> listOf(
            FormFieldSchema("incident_nature", "Nature of ragging / bullying", FieldType.SELECT, listOf("Physical intimidation / Coercion", "Forced acts / Humiliation", "Verbal abuse / Psychological pressure", "Hostel confinement / Late night summoning", "Academic extortion / Note writing", "Other (Specify below)")),
            FormFieldSchema("perpetrators_info", "Senior / Batch info (optional)", FieldType.TEXT, placeholder = "e.g. 3rd year hostel seniors, Wing B, names if known"),
            FormFieldSchema("title", "Incident summary", FieldType.TEXT, placeholder = "e.g. Ragging incident in Hostel C corridor"),
            FormFieldSchema("description", "Confidential description", FieldType.TEXTAREA, placeholder = "Provide dates, times, and observable facts. Routed immediately with top priority to Anti-Ragging Committee...")
        )
        CategoryId.HARASSMENT -> listOf(
            FormFieldSchema("incident_nature", "Incident nature", FieldType.SELECT, listOf("Verbal abuse / Taunting", "Online harassment / Cyber threats", "Physical intimidation / Stalking", "Hostel bullying", "Discrimination", "Other (Specify below)")),
            FormFieldSchema("title", "Incident summary", FieldType.TEXT, placeholder = "e.g. Incident near hostel corridor"),
            FormFieldSchema("description", "Confidential description", FieldType.TEXTAREA, placeholder = "Describe what happened in your own words. If you chose Anonymous, staff cannot see your name or roll number...")
        )
        CategoryId.MALPRACTICE -> listOf(
            FormFieldSchema("malpractice_category", "Malpractice category", FieldType.SELECT, listOf("Exam misconduct / Leakage attempt", "Grading tampering / Favoritism", "Extortion / Unauthorized fees", "Campus asset theft or misuse", "Other (Specify below)")),
            FormFieldSchema("title", "Summary", FieldType.TEXT, placeholder = "e.g. Irregularity observed in exam hall"),
            FormFieldSchema("description", "Detailed report", FieldType.TEXTAREA, placeholder = "State dates, courses, and observable facts. Your report will route strictly to the Chief Vigilance Officer...")
        )
        CategoryId.WELFARE -> listOf(
            FormFieldSchema("welfare_type", "Welfare assistance type", FieldType.SELECT, listOf("Mental health & counseling support", "Financial aid / Scholarship assistance", "Differently-abled campus accessibility", "Emergency leave / Family distress", "Hostel accommodation welfare", "Other (Specify below)")),
            FormFieldSchema("urgency_level", "Assistance urgency", FieldType.SELECT, listOf("Immediate / Today", "Within 48 hours", "Routine consultation")),
            FormFieldSchema("title", "Short summary", FieldType.TEXT, placeholder = "e.g. Request for confidential student counseling session"),
            FormFieldSchema("description", "Situation details", FieldType.TEXTAREA, placeholder = "Share your concerns in confidence. Handled with utmost discretion by the Student Welfare Deanery...")
        )
        CategoryId.MISSING_ITEMS -> listOf(
            FormFieldSchema("report_type", "Report type", FieldType.SELECT, listOf("Lost an item (I misplaced something)", "Found an item (Handing over found property)", "Theft suspected from room/campus", "Other (Specify below)")),
            FormFieldSchema("item_category", "Item type", FieldType.SELECT, listOf("ID Card / Wallet / Keys", "Laptop / Phone / Charger / Earbuds", "Bag / Backpack / Documents", "Watch / Glasses / Personal items", "Bicycle / Vehicle helmet", "Other (Specify below)")),
            FormFieldSchema("title", "Item description & Brand", FieldType.TEXT, placeholder = "e.g. Black Dell laptop charger left in Lib Lab 2"),
            FormFieldSchema("description", "Identifying marks & last seen spot", FieldType.TEXTAREA, placeholder = "Describe serial number, color, stickers, exact time and bench/desk where last seen...")
        )
    }
}
