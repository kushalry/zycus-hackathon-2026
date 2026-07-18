package com.zycus.hackathon.service;

import com.zycus.hackathon.entity.ReassignmentSuggestion;
import com.zycus.hackathon.exception.NotFoundException;
import com.zycus.hackathon.repository.ReassignmentSuggestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SuggestionService {

    private final ReassignmentSuggestionRepository reassignmentSuggestionRepository;

    @Transactional(readOnly = true)
    public List<ReassignmentSuggestion> getSuggestions(ReassignmentSuggestion.Status status) {
        if (status == null) {
            return reassignmentSuggestionRepository.findAll();
        }
        return reassignmentSuggestionRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public ReassignmentSuggestion getSuggestion(Long id) {
        return reassignmentSuggestionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Suggestion not found: " + id));
    }

    @Transactional
    public ReassignmentSuggestion updateStatus(Long id, ReassignmentSuggestion.Status newStatus) {
        ReassignmentSuggestion suggestion = getSuggestion(id);
        ReassignmentSuggestion.Status currentStatus = suggestion.getStatus();

        boolean validTransition = currentStatus == ReassignmentSuggestion.Status.PENDING
                && (newStatus == ReassignmentSuggestion.Status.ACCEPTED
                        || newStatus == ReassignmentSuggestion.Status.REJECTED);

        if (!validTransition) {
            throw new IllegalArgumentException(
                    "Cannot transition from " + currentStatus + " to " + newStatus);
        }

        suggestion.setStatus(newStatus);
        ReassignmentSuggestion saved = reassignmentSuggestionRepository.save(suggestion);
        log.info("Suggestion {} transitioned from PENDING to {}", id, newStatus);
        return saved;
    }
}
