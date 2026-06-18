package com.musicgame.chordmatcher.controller;

import com.musicgame.chordmatcher.dto.ChordMatchResponse;
import com.musicgame.chordmatcher.dto.MelodyRequest;
import com.musicgame.chordmatcher.entity.Melody;
import com.musicgame.chordmatcher.service.MelodyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/melodies")
@CrossOrigin(origins = "*")
public class MelodyController {

    private final MelodyService melodyService;

    public MelodyController(MelodyService melodyService) {
        this.melodyService = melodyService;
    }

    @PostMapping("/match")
    public ResponseEntity<ChordMatchResponse> matchChords(@Valid @RequestBody MelodyRequest request) {
        ChordMatchResponse response = melodyService.createMelodyAndMatchChords(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChordMatchResponse> getMelody(@PathVariable Long id) {
        Optional<ChordMatchResponse> response = melodyService.getMelodyWithChords(id);
        return response.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<Melody>> listMelodies(
            @RequestParam(required = false) String userId) {
        List<Melody> melodies;
        if (userId != null && !userId.isEmpty()) {
            melodies = melodyService.getUserMelodies(userId);
        } else {
            melodies = melodyService.getRecentMelodies();
        }
        return ResponseEntity.ok(melodies);
    }

    @PostMapping
    public ResponseEntity<Map<String, Long>> createMelody(@Valid @RequestBody MelodyRequest request) {
        Melody melody = melodyService.saveMelody(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("id", melody.getId()));
    }
}
