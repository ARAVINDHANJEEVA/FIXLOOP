package com.fixloop.service;

import com.fixloop.domain.Complaint;
import com.fixloop.domain.Skill;
import com.fixloop.domain.StaffProfile;
import com.fixloop.repository.StaffProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class AutoAssignService {
    private final StaffProfileRepository staffRepo;
    private final NotificationService notificationService;
    
    /**
     * Three-tier fallback assignment:
     * 1. Skill match in dept, least loaded
     * 2. Any ACTIVE in dept, least loaded
     * 3. No match -> return Optional.empty() (caller flags for manual)
     */
    public Optional<StaffProfile> findBestStaff(Integer deptId, Set<Skill> requiredSkills) {
        Pageable first = PageRequest.of(0, 1);
        
        // Try each required skill
        for (Skill skill : requiredSkills) {
            List<StaffProfile> candidates = staffRepo.findActiveByDeptAndSkill(deptId, skill.getId(), first);
            if (!candidates.isEmpty()) return Optional.of(candidates.get(0));
        }
        
        // Fallback: any active in dept
        List<StaffProfile> anyInDept = staffRepo.findActiveByDept(deptId, first);
        return anyInDept.isEmpty() ? Optional.empty() : Optional.of(anyInDept.get(0));
    }
    
    /**
     * Atomically assign and update load.
     * Must be called inside a transaction (caller's or this method's).
     */
    public void assignComplaint(Complaint complaint, StaffProfile staff) {
        complaint.setAssignedToStaff(staff);
        complaint.setStatus("ASSIGNED");
        // Increment staff load by complaint's priority score
        if (complaint.getPriorityScore() != null) {
            staffRepo.adjustLoad(staff.getUserId(), complaint.getPriorityScore());
        }
    }
    
    /**
     * Decrement load when complaint resolved/closed.
     */
    public void releaseLoad(Complaint complaint) {
        if (complaint.getAssignedToStaff() != null && complaint.getPriorityScore() != null) {
            staffRepo.adjustLoad(complaint.getAssignedToStaff().getUserId(),
                complaint.getPriorityScore().negate());
        }
    }
}
