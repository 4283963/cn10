package com.musicgame.chordmatcher.dto;

import java.util.List;

public class ChordMatchResponse {

    private Long melodyId;
    private List<String> chords;
    private String key;
    private String keyType;
    private int numMeasures;
    private double confidence;
    private List<NoteDto> originalNotes;

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
}
