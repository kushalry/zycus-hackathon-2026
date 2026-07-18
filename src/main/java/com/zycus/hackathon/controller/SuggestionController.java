package com.zycus.hackathon.controller;

import com.zycus.hackathon.dto.UpdateSuggestionStatusRequest;
import com.zycus.hackathon.entity.ReassignmentSuggestion;
import com.zycus.hackathon.service.SuggestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/suggestions")
@RequiredArgsConstructor
@Slf4j
public class SuggestionController {

    private final SuggestionService suggestionService;

    @GetMapping
    public ResponseEntity<List<ReassignmentSuggestion>> getSuggestions(
            @RequestParam(required = false) String status) {
        ReassignmentSuggestion.Status suggestionStatus =
                status != null ? ReassignmentSuggestion.Status.valueOf(status) : null;
        return ResponseEntity.ok(suggestionService.getSuggestions(suggestionStatus));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReassignmentSuggestion> getSuggestion(@PathVariable Long id) {
        return ResponseEntity.ok(suggestionService.getSuggestion(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ReassignmentSuggestion> updateSuggestionStatus(
            @PathVariable Long id,
            @RequestBody @Valid UpdateSuggestionStatusRequest request) {
        ReassignmentSuggestion saved = suggestionService.updateStatus(id, request.status());
        return ResponseEntity.ok(saved);
    }
}
