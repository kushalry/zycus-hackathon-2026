package com.zycus.hackathon.repository;

import com.zycus.hackathon.entity.ReassignmentSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReassignmentSuggestionRepository extends JpaRepository<ReassignmentSuggestion, Long> {

    List<ReassignmentSuggestion> findByStatus(ReassignmentSuggestion.Status status);

    boolean existsByOrderIdAndTriggerReasonAndStatus(
            Long orderId,
            ReassignmentSuggestion.TriggerReason triggerReason,
            ReassignmentSuggestion.Status status);

    List<ReassignmentSuggestion> findByOrderIdAndStatus(Long orderId, ReassignmentSuggestion.Status status);
}
