package com.musicgame.chordmatcher.dto;

import java.util.List;
import java.util.Map;

public class ChordMatchResponse {

    private Long melodyId;
    private List<String> chords;
    private String key;
    private String keyType;
    private int numMeasures;
    private double confidence;
    private List<NoteDto> originalNotes;

    private String style;
    private Map<String, Object> styleInfo;
    private Map<String, Object> rhythmPattern;
    private List<Map<String, Object>> accompaniment;

    public Long getMelodyId() {
        return melodyId;
    }

    public void setMelodyId(Long melodyId) {
        this.melodyId = melodyId;
    }

    public List<String> getChords() {
        return chords;
    }

    public void setChords(List<String> chords) {
        this.chords = chords;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getKeyType() {
        return keyType;
    }

    public void setKeyType(String keyType) {
        this.keyType = keyType;
    }

    public int getNumMeasures() {
        return numMeasures;
    }

    public void setNumMeasures(int numMeasures) {
        this.numMeasures = numMeasures;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public List<NoteDto> getOriginalNotes() {
        return originalNotes;
    }

    public void setOriginalNotes(List<NoteDto> originalNotes) {
        this.originalNotes = originalNotes;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public Map<String, Object> getStyleInfo() {
        return styleInfo;
    }

    public void setStyleInfo(Map<String, Object> styleInfo) {
        this.styleInfo = styleInfo;
    }

    public Map<String, Object> getRhythmPattern() {
        return rhythmPattern;
    }

    public void setRhythmPattern(Map<String, Object> rhythmPattern) {
        this.rhythmPattern = rhythmPattern;
    }

    public List<Map<String, Object>> getAccompaniment() {
        return accompaniment;
    }

    public void setAccompaniment(List<Map<String, Object>> accompaniment) {
        this.accompaniment = accompaniment;
    }
}
