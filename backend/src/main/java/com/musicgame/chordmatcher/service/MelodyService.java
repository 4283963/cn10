package com.musicgame.chordmatcher.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.musicgame.chordmatcher.dto.ChordMatchResponse;
import com.musicgame.chordmatcher.dto.MelodyRequest;
import com.musicgame.chordmatcher.dto.NoteDto;
import com.musicgame.chordmatcher.entity.ChordMatch;
import com.musicgame.chordmatcher.entity.Melody;
import com.musicgame.chordmatcher.entity.Note;
import com.musicgame.chordmatcher.repository.ChordMatchRepository;
import com.musicgame.chordmatcher.repository.MelodyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MelodyService {

    private static final Logger logger = LoggerFactory.getLogger(MelodyService.class);

    private final MelodyRepository melodyRepository;
    private final ChordMatchRepository chordMatchRepository;
    private final PythonChordMatcherService chordMatcherService;
    private final ObjectMapper objectMapper;

    public MelodyService(MelodyRepository melodyRepository,
                         ChordMatchRepository chordMatchRepository,
                         PythonChordMatcherService chordMatcherService,
                         ObjectMapper objectMapper) {
        this.melodyRepository = melodyRepository;
        this.chordMatchRepository = chordMatchRepository;
        this.chordMatcherService = chordMatcherService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ChordMatchResponse createMelodyAndMatchChords(MelodyRequest request) {
        Melody melody = saveMelody(request);
        ChordMatch chordMatch = matchAndSaveChords(melody, request.getNotes());
        return buildResponse(melody, chordMatch, request.getNotes());
    }

    @Transactional
    public Melody saveMelody(MelodyRequest request) {
        Melody melody = new Melody();
        melody.setTitle(request.getTitle() != null ? request.getTitle() : "Untitled Melody");
        melody.setUserId(request.getUserId());
        melody.setNoteCount(request.getNotes().size());

        double totalDuration = request.getNotes().stream()
                .mapToDouble(NoteDto::getDuration)
                .sum();
        melody.setTotalDuration(totalDuration);

        double currentTime = 0.0;
        for (int i = 0; i < request.getNotes().size(); i++) {
            NoteDto noteDto = request.getNotes().get(i);
            Note note = new Note(noteDto.getPitch(), noteDto.getDuration(), i);
            note.setStartTime(currentTime);
            melody.addNote(note);
            currentTime += noteDto.getDuration();
        }

        Melody saved = melodyRepository.save(melody);
        logger.info("Saved melody with id: {}, notes: {}", saved.getId(), saved.getNoteCount());
        return saved;
    }

    @Transactional
    public ChordMatch matchAndSaveChords(Melody melody, List<NoteDto> notes) {
        try {
            PythonChordMatcherService.ChordMatchResult matchResult =
                    chordMatcherService.matchChords(notes);

            ChordMatch chordMatch = new ChordMatch();
            chordMatch.setChordsJson(objectMapper.writeValueAsString(matchResult.getChords()));
            chordMatch.setKey(matchResult.getKey());
            chordMatch.setKeyType(matchResult.getKeyType());
            chordMatch.setNumMeasures(matchResult.getNumMeasures());
            chordMatch.setConfidence(matchResult.getConfidence());
            chordMatch.setMelody(melody);

            ChordMatch saved = chordMatchRepository.save(chordMatch);
            logger.info("Saved chord match for melody {}: {} chords, key: {} {}",
                    melody.getId(), matchResult.getChords().size(),
                    matchResult.getKey(), matchResult.getKeyType());
            return saved;

        } catch (Exception e) {
            logger.error("Failed to match chords for melody {}: {}", melody.getId(), e.getMessage(), e);
            throw new RuntimeException("Chord matching failed: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public Optional<ChordMatchResponse> getMelodyWithChords(Long id) {
        Optional<Melody> melodyOpt = melodyRepository.findByIdWithNotes(id);
        if (melodyOpt.isEmpty()) {
            return Optional.empty();
        }

        Melody melody = melodyOpt.get();
        Optional<ChordMatch> chordMatchOpt = chordMatchRepository.findByMelodyId(id);

        List<NoteDto> notes = melody.getNotes().stream()
                .map(this::toNoteDto)
                .collect(Collectors.toList());

        ChordMatchResponse response = new ChordMatchResponse();
        response.setMelodyId(melody.getId());
        response.setOriginalNotes(notes);

        if (chordMatchOpt.isPresent()) {
            ChordMatch chordMatch = chordMatchOpt.get();
            response.setChords(parseChordsJson(chordMatch.getChordsJson()));
            response.setKey(chordMatch.getKey());
            response.setKeyType(chordMatch.getKeyType());
            response.setNumMeasures(chordMatch.getNumMeasures());
            response.setConfidence(chordMatch.getConfidence());
        }

        return Optional.of(response);
    }

    @Transactional(readOnly = true)
    public List<Melody> getUserMelodies(String userId) {
        return melodyRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<Melody> getRecentMelodies() {
        return melodyRepository.findTop10ByOrderByCreatedAtDesc();
    }

    private ChordMatchResponse buildResponse(Melody melody, ChordMatch chordMatch, List<NoteDto> originalNotes) {
        ChordMatchResponse response = new ChordMatchResponse();
        response.setMelodyId(melody.getId());
        response.setOriginalNotes(originalNotes);
        response.setChords(parseChordsJson(chordMatch.getChordsJson()));
        response.setKey(chordMatch.getKey());
        response.setKeyType(chordMatch.getKeyType());
        response.setNumMeasures(chordMatch.getNumMeasures());
        response.setConfidence(chordMatch.getConfidence());
        return response;
    }

    private NoteDto toNoteDto(Note note) {
        NoteDto dto = new NoteDto();
        dto.setPitch(note.getPitch());
        dto.setDuration(note.getDuration());
        dto.setStartTime(note.getStartTime());
        return dto;
    }

    @SuppressWarnings("unchecked")
    private List<String> parseChordsJson(String chordsJson) {
        try {
            return objectMapper.readValue(chordsJson, List.class);
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse chords JSON: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}
