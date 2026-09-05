import { Complaint, PriorityWeights } from '../types';

export const DEFAULT_PRIORITY_WEIGHTS: PriorityWeights = {
  w1: 0.48, // severity_norm
  w2: 0.21, // scale_norm
  w3: 0.08, // recurrence_norm
  w4: 0.23, // time_urgency_norm
};

export interface PriorityFactors {
  severity: number;
  studentsAffected: number;
  recurrenceCountLast90Days: number;
  hoursSinceCreated: number;
  slaHours: number;
}

export interface PriorityCalculationResult {
  severityNorm: number;
  scaleNorm: number;
  recurrenceNorm: number;
  timeUrgencyNorm: number;
  severityComponent: number;
  scaleComponent: number;
  recurrenceComponent: number;
  timeUrgencyComponent: number;
  rawScore: number; // 0.0 - 1.0 (exact formula result)
  scaledScore: number; // 0.0 - 100.0 (for UI gauges, indicators, and filters)
  weights: PriorityWeights;
}

/**
 * Calculates priority score based on the user's formula:
 * 
 * severity_norm     = severity / 5
 * scale_norm        = ln(1 + students_affected) / ln(1 + 500)   // cap at 500 students
 * recurrence_norm   = min(recurrence_count_last_90_days, 10) / 10
 * time_urgency_norm = min(hours_since_created / sla_hours, 1.0)
 * 
 * priority_score = (0.48 * severity_norm)
 *                + (0.21 * scale_norm)
 *                + (0.08 * recurrence_norm)
 *                + (0.23 * time_urgency_norm)
 */
export function calculatePriorityScore(
  factors: PriorityFactors,
  customWeights?: Partial<PriorityWeights>
): PriorityCalculationResult {
  const w1 = customWeights?.w1 ?? DEFAULT_PRIORITY_WEIGHTS.w1;
  const w2 = customWeights?.w2 ?? DEFAULT_PRIORITY_WEIGHTS.w2;
  const w3 = customWeights?.w3 ?? DEFAULT_PRIORITY_WEIGHTS.w3;
  const w4 = customWeights?.w4 ?? DEFAULT_PRIORITY_WEIGHTS.w4;

  // 1. severity_norm = severity / 5
  const clampedSeverity = Math.max(1, Math.min(factors.severity || 1, 5));
  const severityNorm = clampedSeverity / 5;

  // 2. scale_norm = ln(1 + students_affected) / ln(1 + 500) // cap at 500 students
  const clampedStudents = Math.max(0, Math.min(factors.studentsAffected || 1, 500));
  const scaleNorm = Math.log(1 + clampedStudents) / Math.log(1 + 500);

  // 3. recurrence_norm = min(recurrence_count_last_90_days, 10) / 10
  const clampedRecurrence = Math.max(0, factors.recurrenceCountLast90Days || 0);
  const recurrenceNorm = Math.min(clampedRecurrence, 10) / 10;

  // 4. time_urgency_norm = min(hours_since_created / sla_hours, 1.0)
  const safeSla = Math.max(0.5, factors.slaHours || 24);
  const safeHoursElapsed = Math.max(0, factors.hoursSinceCreated || 0);
  const timeUrgencyNorm = Math.min(safeHoursElapsed / safeSla, 1.0);

  // Components: weight * norm
  const severityComponent = w1 * severityNorm;
  const scaleComponent = w2 * scaleNorm;
  const recurrenceComponent = w3 * recurrenceNorm;
  const timeUrgencyComponent = w4 * timeUrgencyNorm;

  // Exact raw formula score [0.0 - 1.0]
  const rawScore = severityComponent + scaleComponent + recurrenceComponent + timeUrgencyComponent;

  // Scaled 0 - 100 for UI meters and badges
  const scaledScore = Math.min(100, Math.max(0, rawScore * 100));

  return {
    severityNorm: Number(severityNorm.toFixed(4)),
    scaleNorm: Number(scaleNorm.toFixed(4)),
    recurrenceNorm: Number(recurrenceNorm.toFixed(4)),
    timeUrgencyNorm: Number(timeUrgencyNorm.toFixed(4)),
    severityComponent: Number(severityComponent.toFixed(4)),
    scaleComponent: Number(scaleComponent.toFixed(4)),
    recurrenceComponent: Number(recurrenceComponent.toFixed(4)),
    timeUrgencyComponent: Number(timeUrgencyComponent.toFixed(4)),
    rawScore: Number(rawScore.toFixed(4)),
    scaledScore: Number(scaledScore.toFixed(1)),
    weights: { w1, w2, w3, w4 },
  };
}

/**
 * Extracts factors from a Complaint object and calculates the priority breakdown.
 */
export function calculateComplaintPriority(
  complaint: Complaint,
  defaultSlaHours?: number,
  customWeights?: Partial<PriorityWeights>
): PriorityCalculationResult {
  // Determine SLA hours from deadline or default
  let slaHours = defaultSlaHours;
  if (!slaHours && complaint.slaDeadline && complaint.createdAt) {
    const diffMs = new Date(complaint.slaDeadline).getTime() - new Date(complaint.createdAt).getTime();
    if (diffMs > 0) {
      slaHours = diffMs / (1000 * 60 * 60);
    }
  }
  if (!slaHours) {
    slaHours = 24;
  }

  // Hours elapsed since created
  const hoursSinceCreated = complaint.createdAt
    ? Math.max(0, (Date.now() - new Date(complaint.createdAt).getTime()) / (1000 * 60 * 60))
    : 0;

  // Recurrence count (prioritizing recurrenceCountLast90Days, then incidentCount, then sibling count)
  const recurrenceCountLast90Days =
    complaint.recurrenceCountLast90Days ??
    complaint.incidentCount ??
    (complaint.siblingTokens ? complaint.siblingTokens.length : 0);

  return calculatePriorityScore(
    {
      severity: complaint.severity,
      studentsAffected: complaint.studentsAffected,
      recurrenceCountLast90Days,
      hoursSinceCreated,
      slaHours,
    },
    customWeights
  );
}
