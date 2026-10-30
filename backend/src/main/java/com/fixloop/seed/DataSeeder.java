package com.fixloop.seed;

import com.fixloop.domain.*;
import com.fixloop.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements ApplicationListener<ApplicationReadyEvent> {

    private final AppUserRepository userRepo;
    private final DepartmentRepository deptRepo;
    private final SkillRepository skillRepo;
    private final CategoryRepository categoryRepo;
    private final SlaRuleRepository slaRuleRepo;
    private final PriorityWeightConfigRepository pwConfigRepo;
    private final AdminProfileRepository adminRepo;
    private final StaffProfileRepository staffRepo;
    private final VendorRepository vendorRepo;
    private final LocationRepository locationRepo;
    private final IncidentRepository incidentRepo;
    private final ComplaintRepository complaintRepo;
    private final StatusHistoryRepository historyRepo;
    private final StudentProfileRepository studentRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (userRepo.count() > 0 || complaintRepo.count() > 0 || deptRepo.count() > 0) {
            log.info("Database already initialized/seeded. Skipping DataSeeder.");
            return;
        }

        log.info("Seeding FixLoop database with mock data...");

        // 1. Departments
        Department maint = deptRepo.save(Department.builder().name("Maintenance").code("MAINT").description("Handles hostel and building upkeep").headOfDept("Rajesh Kumar").build());
        Department food = deptRepo.save(Department.builder().name("Catering & Food").code("FOOD").description("Manages food quality and cafeteria operations").headOfDept("Meena Sundaram").build());
        Department it = deptRepo.save(Department.builder().name("IT & Systems").code("IT").description("Handles all technology and network issues").headOfDept("Arjun Nair").build());
        Department sa = deptRepo.save(Department.builder().name("Student Affairs").code("SA").description("Handles academic, health, and sensitive complaints").headOfDept("Dr. Priya Sharma").build());
        Department sec = deptRepo.save(Department.builder().name("Security & Lost Property").code("SEC").description("Handles campus security, lost items, and access control").headOfDept("Col. (Retd.) Suresh Nair").build());
        Department vigil = deptRepo.save(Department.builder().name("Vigilance & Compliance").code("VIGIL").description("Handles malpractice, integrity violations, and compliance").headOfDept("Dr. Anitha Balakrishnan").build());

        // 2. Skills
        String[] skillNames = {
            "plumbing", "electrical", "maintenance", "carpentry", "food-quality",
            "hygiene", "housekeeping", "sanitation", "IT", "networking",
            "hardware", "academic", "faculty", "health", "medical",
            "counseling", "authority", "anti-ragging-cell", "academic-integrity", "pest-control",
            "security", "lost-property", "vigilance", "welfare", "mental-health"
        };
        Map<String, Skill> skills = new HashMap<>();
        for (String s : skillNames) {
            skills.put(s, skillRepo.save(Skill.builder().name(s).build()));
        }

        // 3. Categories (with filterGroup for mobile filter pills)
        // filterGroup values: "FACILITIES" | "ACADEMIC_TECH" | "WELFARE_MISSING" | null (sensitive)
        String hostelSchema = "[{\"id\":\"hostelBlock\",\"label\":\"Hostel Block\",\"type\":\"select\",\"required\":true,\"options\":[\"Block A\",\"Block B\",\"Block C\",\"Block D\"]},{\"id\":\"issueType\",\"label\":\"Issue Type\",\"type\":\"select\",\"required\":true,\"options\":[\"Plumbing\",\"Electrical\",\"Furniture\",\"Pest Control\",\"Other\"]},{\"id\":\"roomNumber\",\"label\":\"Room Number\",\"type\":\"text\",\"required\":true},{\"id\":\"description\",\"label\":\"Describe the issue\",\"type\":\"textarea\",\"required\":false}]";
        String foodSchema = "[{\"id\":\"mealType\",\"label\":\"Meal Type\",\"type\":\"select\",\"required\":true,\"options\":[\"Breakfast\",\"Lunch\",\"Dinner\",\"Snacks\"]},{\"id\":\"cafeteria\",\"label\":\"Cafeteria\",\"type\":\"select\",\"required\":true,\"options\":[\"Main Cafeteria\",\"Block B Canteen\",\"Sports Canteen\"]},{\"id\":\"issueType\",\"label\":\"Issue Type\",\"type\":\"select\",\"required\":true,\"options\":[\"Quality\",\"Hygiene\",\"Quantity\",\"Pricing\"]},{\"id\":\"description\",\"label\":\"Describe the problem\",\"type\":\"textarea\",\"required\":false}]";
        String academicSchema = "[{\"id\":\"department\",\"label\":\"Department\",\"type\":\"text\",\"required\":true},{\"id\":\"subject\",\"label\":\"Subject / Course\",\"type\":\"text\",\"required\":true},{\"id\":\"issueType\",\"label\":\"Issue Type\",\"type\":\"select\",\"required\":true,\"options\":[\"Faculty Conduct\",\"Grading\",\"Syllabus\",\"Lab Equipment\",\"Attendance\"]},{\"id\":\"description\",\"label\":\"Additional Details\",\"type\":\"textarea\",\"required\":false}]";
        String healthSchema = "[{\"id\":\"symptoms\",\"label\":\"Symptoms Reported\",\"type\":\"textarea\",\"required\":true},{\"id\":\"studentsAffectedCount\",\"label\":\"Number Affected\",\"type\":\"number\",\"required\":true},{\"id\":\"urgent\",\"label\":\"Is this urgent?\",\"type\":\"boolean\",\"required\":true},{\"id\":\"description\",\"label\":\"Additional Details\",\"type\":\"textarea\",\"required\":false}]";
        String hygieneSchema = "[{\"id\":\"area\",\"label\":\"Area\",\"type\":\"select\",\"required\":true,\"options\":[\"Washroom\",\"Common Area\",\"Dining Hall\",\"Corridor\",\"Lab\",\"Library\"]},{\"id\":\"issueType\",\"label\":\"Issue Type\",\"type\":\"select\",\"required\":true,\"options\":[\"Cleanliness\",\"Waste Disposal\",\"Pest\",\"Drainage\"]},{\"id\":\"description\",\"label\":\"Describe the issue\",\"type\":\"textarea\",\"required\":false}]";
        String techSchema = "[{\"id\":\"deviceType\",\"label\":\"Device / System\",\"type\":\"select\",\"required\":true,\"options\":[\"PC/Laptop\",\"Projector\",\"Network/WiFi\",\"Printer\",\"Server\",\"Other\"]},{\"id\":\"errorDescription\",\"label\":\"Error / Issue Description\",\"type\":\"textarea\",\"required\":true},{\"id\":\"labRoom\",\"label\":\"Lab / Room\",\"type\":\"text\",\"required\":false},{\"id\":\"description\",\"label\":\"Additional context\",\"type\":\"textarea\",\"required\":false}]";
        String welfareSchema = "[{\"id\":\"welfareType\",\"label\":\"Welfare Concern\",\"type\":\"select\",\"required\":true,\"options\":[\"Mental Health\",\"Financial Hardship\",\"Disability Support\",\"Medical Leave\",\"Other\"]},{\"id\":\"urgency\",\"label\":\"Urgency\",\"type\":\"select\",\"required\":true,\"options\":[\"Immediate\",\"Within a week\",\"General inquiry\"]},{\"id\":\"description\",\"label\":\"Please describe your situation\",\"type\":\"textarea\",\"required\":true}]";
        String missingItemsSchema = "[{\"id\":\"itemType\",\"label\":\"Type of Item\",\"type\":\"select\",\"required\":true,\"options\":[\"Laptop / Electronics\",\"ID Card\",\"Clothing / Personal\",\"Bag / Books\",\"Bicycle\",\"Other\"]},{\"id\":\"lastSeenLocation\",\"label\":\"Last Seen Location\",\"type\":\"text\",\"required\":true},{\"id\":\"lastSeenDate\",\"label\":\"Date Last Seen\",\"type\":\"text\",\"required\":true},{\"id\":\"description\",\"label\":\"Item Description\",\"type\":\"textarea\",\"required\":true}]";
        String harassmentSchema = "[{\"id\":\"incidentDate\",\"label\":\"Date of Incident\",\"type\":\"text\",\"required\":true},{\"id\":\"incidentLocation\",\"label\":\"Location of Incident\",\"type\":\"text\",\"required\":true},{\"id\":\"relationshipToPerpetrator\",\"label\":\"Relation to Respondent\",\"type\":\"select\",\"required\":true,\"options\":[\"Faculty\",\"Staff\",\"Student\",\"Other\"]},{\"id\":\"description\",\"label\":\"Please describe the incident\",\"type\":\"textarea\",\"required\":true}]";
        String malpracticeSchema = "[{\"id\":\"examOrCourse\",\"label\":\"Exam / Course\",\"type\":\"text\",\"required\":true},{\"id\":\"malpracticeType\",\"label\":\"Type of Malpractice\",\"type\":\"select\",\"required\":true,\"options\":[\"Cheating\",\"Impersonation\",\"Plagiarism\",\"Bribery\",\"Other\"]},{\"id\":\"evidenceAvailable\",\"label\":\"Evidence Available?\",\"type\":\"boolean\",\"required\":false},{\"id\":\"description\",\"label\":\"Describe the incident\",\"type\":\"textarea\",\"required\":true}]";
        String raggingSchema = "[{\"id\":\"raggingType\",\"label\":\"Nature of Ragging\",\"type\":\"select\",\"required\":true,\"options\":[\"Physical Harassment\",\"Verbal Abuse / Intimidation\",\"Forced Acts / Tasks\",\"Extortion / Financial Demands\",\"Cyber / Online Ragging\"]},{\"id\":\"incidentLocation\",\"label\":\"Location / Hostel Block\",\"type\":\"text\",\"required\":true},{\"id\":\"perpetratorsKnown\",\"label\":\"Perpetrators Identified?\",\"type\":\"select\",\"required\":true,\"options\":[\"Yes - Senior batch known\",\"Partial - Room/Branch known\",\"Unknown / Group\"]},{\"id\":\"immediateAssistanceNeeded\",\"label\":\"Immediate Medical or Physical Assistance Required?\",\"type\":\"boolean\",\"required\":true},{\"id\":\"description\",\"label\":\"Describe the incident\",\"type\":\"textarea\",\"required\":true}]";

        Category catHostel = createCategory("Hostel", "Home", "#6366f1", false, "FACILITIES", new BigDecimal("3.00"), 24, maint, hostelSchema, skills.get("maintenance"), skills.get("plumbing"), skills.get("electrical"));
        Category catFood = createCategory("Food", "Utensils", "#f59e0b", false, "FACILITIES", new BigDecimal("2.00"), 12, food, foodSchema, skills.get("food-quality"), skills.get("hygiene"));
        Category catHygiene = createCategory("Hygiene", "Sparkles", "#22c55e", false, "FACILITIES", new BigDecimal("2.00"), 8, maint, hygieneSchema, skills.get("housekeeping"), skills.get("sanitation"));
        Category catAcademic = createCategory("Academic", "BookOpen", "#3b82f6", false, "ACADEMIC_TECH", new BigDecimal("2.00"), 48, sa, academicSchema, skills.get("academic"), skills.get("faculty"));
        Category catTech = createCategory("Tech Support", "Laptop", "#06b6d4", false, "ACADEMIC_TECH", new BigDecimal("2.00"), 12, it, techSchema, skills.get("IT"), skills.get("networking"), skills.get("hardware"));
        Category catHealth = createCategory("Health", "Activity", "#ef4444", false, "WELFARE_MISSING", new BigDecimal("4.00"), 6, sa, healthSchema, skills.get("health"), skills.get("medical"));
        Category catWelfare = createCategory("Welfare", "HeartHandshake", "#a855f7", false, "WELFARE_MISSING", new BigDecimal("3.00"), 48, sa, welfareSchema, skills.get("welfare"), skills.get("mental-health"), skills.get("counseling"));
        Category catMissing = createCategory("Missing Items", "PackageSearch", "#64748b", false, "WELFARE_MISSING", new BigDecimal("1.00"), 72, sec, missingItemsSchema, skills.get("security"), skills.get("lost-property"));
        Category catHarassment = createCategory("Harassment", "Lock", "#ec4899", true, null, new BigDecimal("5.00"), 24, sa, harassmentSchema, skills.get("counseling"), skills.get("authority"));
        Category catMalpractice = createCategory("Malpractice", "AlertTriangle", "#f97316", true, null, new BigDecimal("5.00"), 48, vigil, malpracticeSchema, skills.get("authority"), skills.get("academic-integrity"), skills.get("vigilance"));
        Category catRagging = createCategory("Ragging", "ShieldAlert", "#dc2626", true, null, new BigDecimal("5.00"), 12, sa, raggingSchema, skills.get("authority"), skills.get("anti-ragging-cell"), skills.get("counseling"));

        List<Category> allCats = Arrays.asList(catHostel, catFood, catHygiene, catAcademic, catTech, catHealth, catWelfare, catMissing, catHarassment, catMalpractice, catRagging);


        // 4. SLA Rules
        for (Category c : allCats) {
            slaRuleRepo.save(SlaRule.builder().category(c).severityTier("LOW").resolutionHours(c.getDefaultSlaHours() * 2).escalationHours(c.getDefaultSlaHours() * 3).build());
            slaRuleRepo.save(SlaRule.builder().category(c).severityTier("MEDIUM").resolutionHours(c.getDefaultSlaHours()).escalationHours((int) (c.getDefaultSlaHours() * 1.5)).build());
            slaRuleRepo.save(SlaRule.builder().category(c).severityTier("HIGH").resolutionHours(Math.max(1, c.getDefaultSlaHours() / 2)).escalationHours(c.getDefaultSlaHours()).build());
            slaRuleRepo.save(SlaRule.builder().category(c).severityTier("CRITICAL").resolutionHours(Math.max(1, c.getDefaultSlaHours() / 4)).escalationHours(Math.max(1, c.getDefaultSlaHours() / 2)).build());
        }

        // 5. Priority Weight Config
        pwConfigRepo.save(PriorityWeightConfig.builder()
                .w1Severity(new BigDecimal("0.480"))
                .w2Students(new BigDecimal("0.210"))
                .w3Recurrence(new BigDecimal("0.080"))
                .w4TimeDecay(new BigDecimal("0.230"))
                .effectiveFrom(LocalDateTime.now())
                .build());

        // 6. Admin User
        AppUser adminUser = userRepo.save(AppUser.builder()
                .email("admin@fixloop.edu")
                .passwordHash(passwordEncoder.encode("admin123"))
                .role("ADMIN")
                .name("Admin Krishnan")
                .avatarInitials("AK")
                .restrictedAccess(true)
                .build());

        AdminProfile adminProfile = AdminProfile.builder()
                .userId(adminUser.getId())
                .user(adminUser)
                .authorityLevel((short) 1)
                .build();
        adminRepo.save(adminProfile);

        // 7. Staff Users
        StaffProfile stf1 = createStaff("vikram@fixloop.edu", "Vikram Singh", maint, "Senior Technician", "ACTIVE", 42.5, skills.get("plumbing"), skills.get("electrical"), skills.get("maintenance"));
        StaffProfile stf2 = createStaff("anita@fixloop.edu", "Anita Rao", maint, "Junior Technician", "ACTIVE", 18.0, skills.get("maintenance"), skills.get("carpentry"));
        StaffProfile stf3 = createStaff("suresh@fixloop.edu", "Suresh Babu", food, "Food Inspector", "ACTIVE", 25.3, skills.get("food-quality"), skills.get("hygiene"), skills.get("sanitation"));
        StaffProfile stf4 = createStaff("kavitha@fixloop.edu", "Kavitha Menon", food, "Housekeeping Supervisor", "ACTIVE", 12.1, skills.get("housekeeping"), skills.get("sanitation"));
        StaffProfile stf5 = createStaff("staff@fixloop.edu", "Deepak Joshi", it, "IT Support Lead", "ACTIVE", 55.8, skills.get("IT"), skills.get("networking"), skills.get("hardware"));
        StaffProfile stf6 = createStaff("nisha@fixloop.edu", "Nisha Gupta", it, "Network Engineer", "ACTIVE", 30.2, skills.get("networking"), skills.get("IT"));
        StaffProfile stf7 = createStaff("ravi@fixloop.edu", "Ravi Chandran", sa, "Student Counselor", "ACTIVE", 20.0, skills.get("counseling"), skills.get("academic"), skills.get("authority"));
        StaffProfile stf8 = createStaff("lalitha@fixloop.edu", "Lalitha Devi", sa, "Medical Officer", "INACTIVE", 0.0, skills.get("health"), skills.get("medical"), skills.get("counseling"));

        // 8. Vendors
        createVendor("QuickFix Plumbers", "John Q", "9876543210", "quickfix@vendors.in", skills.get("plumbing"));
        createVendor("BrightSpark Electricals", "Mike B", "8765432109", "brightspark@vendors.in", skills.get("electrical"));
        createVendor("CleanZone Sanitation", "Sara C", "7654321098", "cleanzone@vendors.in", skills.get("sanitation"), skills.get("housekeeping"), skills.get("pest-control"));
        createVendor("CoolBreeze HVAC", "Tom C", "6543210987", "coolbreeze@vendors.in", skills.get("maintenance"), skills.get("electrical"));
        createVendor("TechServe Systems", "Tim T", "5432109876", "techserve@vendors.in", skills.get("hardware"), skills.get("IT"), skills.get("networking"));

        // 9. Locations
        Location locMedical = locationRepo.save(Location.builder().building("Medical Block").floor("Ground").room("Infirmary").campusZone("Health Center").build());
        Location locBlockA = locationRepo.save(Location.builder().building("Block A Hostel").floor("3rd Floor").room("A-312").campusZone("Hostel Zone").build());
        Location locCSBlock = locationRepo.save(Location.builder().building("CS Block").floor("2nd Floor").room("Lab CS-204").campusZone("Academic Zone").build());
        Location locCafeteria = locationRepo.save(Location.builder().building("Main Cafeteria").floor("Ground").room("Dining Hall").campusZone("Central Zone").build());
        Location locBlockB = locationRepo.save(Location.builder().building("Block B Hostel").floor("1st Floor").room("B-105").campusZone("Hostel Zone").build());

        // 9b. Student Test User for Mobile App
        AppUser studentUser = userRepo.save(AppUser.builder()
                .email("student@fixloop.edu")
                .passwordHash(passwordEncoder.encode("student123"))
                .role("STUDENT")
                .name("Alex Johnson")
                .avatarInitials("AJ")
                .restrictedAccess(false)
                .build());

        StudentProfile studentProfile = StudentProfile.builder()
                .userId(studentUser.getId())
                .user(studentUser)
                .rollNo("22CS084")
                .department(it)
                .defaultLocation(locBlockA)
                .build();
        studentProfile = studentRepo.save(studentProfile);

        // 10. Sample Complaints
        LocalDateTime now = LocalDateTime.now();

        // cmp-1
        Incident inc1 = incidentRepo.save(Incident.builder().category(catHealth).location(locMedical).clusterKey(catHealth.getId() + ":" + locMedical.getId() + ":" + now.toLocalDate()).status("OPEN").complaintCount(14).lastComplaintAt(now.minusHours(10)).build());
        Complaint c1 = complaintRepo.save(Complaint.builder()
                .token("FXL-2026-000182")
                .isAnonymous(false)
                .studentUser(studentProfile)
                .category(catHealth)
                .location(locMedical)
                .incident(inc1)
                .description("Multiple students from Block B reported severe stomach pain and vomiting after lunch. Currently 14 students are in the infirmary.")
                .severity((short) 4)
                .studentsAffected(14)
                .recurrenceCount90d(0)
                .status("IN_PROGRESS")
                .priorityScore(new BigDecimal("98.40"))
                .extraData("{\"symptoms\":\"Nausea, vomiting, abdominal cramps\",\"studentsAffectedCount\":14,\"urgent\":true}")
                .assignedToStaff(stf7)
                .routedDepartment(sa)
                .dispatchType("INTERNAL_STAFF")
                .escalationLevel((short) 1)
                .isAtRisk(true)
                .isBreached(false)
                .slaDeadline(now.plusHours(2))
                .build());
        addHistory(c1, "SUBMITTED", studentUser.getId(), "Alex Johnson", "STUDENT", null);
        addHistory(c1, "IN_PROGRESS", stf7.getUserId(), "Ravi Chandran", "STAFF", "Coordinating with infirmary staff. Food samples sent to lab.");

        // cmp-2
        Incident inc2 = incidentRepo.save(Incident.builder().category(catHostel).location(locBlockA).clusterKey(catHostel.getId() + ":" + locBlockA.getId() + ":" + now.minusDays(3).toLocalDate()).status("OPEN").complaintCount(3).lastComplaintAt(now.minusDays(3)).build());
        Complaint c2 = complaintRepo.save(Complaint.builder()
                .token("FXL-2026-000165")
                .isAnonymous(false)
                .studentUser(studentProfile)
                .category(catHostel)
                .location(locBlockA)
                .incident(inc2)
                .description("The bathroom ceiling has been leaking for 3 days. Water accumulation is causing floor slippage hazard.")
                .severity((short) 4)
                .studentsAffected(6)
                .recurrenceCount90d(2)
                .status("ASSIGNED")
                .priorityScore(new BigDecimal("87.10"))
                .extraData("{\"hostelBlock\":\"Block A\",\"issueType\":\"Plumbing\",\"roomNumber\":\"A-312\"}")
                .assignedToStaff(stf1)
                .routedDepartment(maint)
                .dispatchType("INTERNAL_STAFF")
                .escalationLevel((short) 2)
                .isAtRisk(false)
                .isBreached(true)
                .slaDeadline(now.minusHours(15))
                .build());
        addHistory(c2, "SUBMITTED", studentUser.getId(), "Alex Johnson", "STUDENT", null);
        addHistory(c2, "ASSIGNED", null, "System", "SYSTEM", "Auto-assigned to Vikram Singh");

        // cmp-3
        Incident inc3 = incidentRepo.save(Incident.builder().category(catTech).location(locCSBlock).clusterKey(catTech.getId() + ":" + locCSBlock.getId() + ":" + now.minusDays(1).toLocalDate()).status("OPEN").complaintCount(1).lastComplaintAt(now.minusDays(1)).build());
        Complaint c3 = complaintRepo.save(Complaint.builder()
                .token("FXL-2026-000201")
                .isAnonymous(false)
                .studentUser(studentProfile)
                .category(catTech)
                .location(locCSBlock)
                .description("All 30 PCs in Lab CS-204 are showing BSOD on startup since yesterday morning. Programming practicals are blocked.")
                .severity((short) 4)
                .studentsAffected(60)
                .recurrenceCount90d(0)
                .status("VENDOR_ASSIGNED")
                .priorityScore(new BigDecimal("91.70"))
                .extraData("{\"deviceType\":\"PC/Laptop\",\"errorDescription\":\"BSOD on startup\",\"labRoom\":\"CS-204\"}")
                .assignedToStaff(stf5)
                .routedDepartment(it)
                .dispatchType("EXTERNAL_VENDOR")
                .escalationLevel((short) 0)
                .isAtRisk(true)
                .isBreached(false)
                .slaDeadline(now.plusHours(4))
                .build());
        addHistory(c3, "SUBMITTED", studentUser.getId(), "Alex Johnson", "STUDENT", null);
        addHistory(c3, "VENDOR_ASSIGNED", stf5.getUserId(), "Deepak Joshi", "STAFF", "Escalated to TechServe Systems");

        // cmp-4
        Complaint c4 = complaintRepo.save(Complaint.builder()
                .token("FXL-2026-000198")
                .isAnonymous(false)
                .studentUser(studentProfile)
                .category(catFood)
                .location(locCafeteria)
                .description("The rice served at dinner consistently has small stones/foreign objects.")
                .severity((short) 3)
                .studentsAffected(200)
                .recurrenceCount90d(5)
                .status("AWAITING_STUDENT_INFO")
                .priorityScore(new BigDecimal("72.30"))
                .extraData("{\"mealType\":\"Dinner\",\"cafeteria\":\"Main Cafeteria\",\"issueType\":\"Quality\"}")
                .assignedToStaff(stf3)
                .routedDepartment(food)
                .dispatchType("INTERNAL_STAFF")
                .escalationLevel((short) 0)
                .isAtRisk(false)
                .isBreached(false)
                .slaDeadline(now.plusHours(8))
                .build());
        addHistory(c4, "SUBMITTED", studentUser.getId(), "Alex Johnson", "STUDENT", null);
        addHistory(c4, "AWAITING_STUDENT_INFO", stf3.getUserId(), "Suresh Babu", "STAFF", "Please provide photos of the food batch.");

        // cmp-5
        Complaint c5 = complaintRepo.save(Complaint.builder()
                .token("FXL-2026-000155")
                .isAnonymous(false)
                .studentUser(studentProfile)
                .category(catHygiene)
                .location(locBlockB)
                .description("Washroom on 1st floor of Block B has severe drainage blockage and odor.")
                .severity((short) 3)
                .studentsAffected(40)
                .recurrenceCount90d(1)
                .status("RESOLVED")
                .priorityScore(new BigDecimal("65.80"))
                .extraData("{\"area\":\"Washroom\",\"issueType\":\"Drainage\"}")
                .assignedToStaff(stf4)
                .routedDepartment(food)
                .dispatchType("INTERNAL_STAFF")
                .escalationLevel((short) 0)
                .isAtRisk(false)
                .isBreached(false)
                .slaDeadline(now.minusDays(4))
                .resolvedAt(now.minusDays(4))
                .build());
        addHistory(c5, "SUBMITTED", studentUser.getId(), "Alex Johnson", "STUDENT", null);
        addHistory(c5, "RESOLVED", stf4.getUserId(), "Kavitha Menon", "STAFF", "Issue resolved. Drainage cleared.");

        log.info("Database seeding completed successfully.");
    }

    private Category createCategory(String name, String icon, String color, boolean sensitive,
                                    String filterGroup, BigDecimal baseWeight, int defaultSla,
                                    Department dept, String schema, Skill... reqSkills) {
        Category c = Category.builder()
                .name(name)
                .icon(icon)
                .color(color)
                .sensitive(sensitive)
                .filterGroup(filterGroup)
                .baseSeverityWeight(baseWeight)
                .defaultSlaHours(defaultSla)
                .department(dept)
                .formSchema(schema)
                .requiredSkills(new HashSet<>(Arrays.asList(reqSkills)))
                .build();
        return categoryRepo.save(c);
    }

    private StaffProfile createStaff(String email, String name, Department dept, String title, String status, double load, Skill... skills) {
        String[] parts = name.trim().split("\\s+");
        String initials = parts.length >= 2 ? (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase() : parts[0].substring(0, 1).toUpperCase();

        AppUser user = userRepo.save(AppUser.builder()
                .email(email)
                .name(name)
                .passwordHash(passwordEncoder.encode("staff123"))
                .role("STAFF")
                .restrictedAccess(false)
                .avatarInitials(initials)
                .build());

        StaffProfile profile = StaffProfile.builder()
                .userId(user.getId())
                .user(user)
                .department(dept)
                .jobTitle(title)
                .status(status)
                .currentWeightedLoad(BigDecimal.valueOf(load))
                .skills(new HashSet<>(Arrays.asList(skills)))
                .build();

        return staffRepo.save(profile);
    }

    private void createVendor(String name, String contact, String phone, String email, Skill... skills) {
        Vendor v = Vendor.builder()
                .name(name)
                .contactPerson(contact)
                .phone(phone)
                .email(email)
                .skills(new HashSet<>(Arrays.asList(skills)))
                .build();
        vendorRepo.save(v);
    }

    private void addHistory(Complaint c, String status, Integer actorId, String actorName, String actorRole, String note) {
        historyRepo.save(StatusHistory.builder()
                .complaint(c)
                .status(status)
                .changedById(actorId)
                .changedByName(actorName)
                .changedByRole(actorRole)
                .note(note)
                .build());
    }
}
