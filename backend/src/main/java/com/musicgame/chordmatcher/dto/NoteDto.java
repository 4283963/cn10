package com.musicgame.chordmatcher.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public class NoteDto {

    @NotBlank(message = "Pitch cannot be blank")
    private String pitch;

    @Positive(message = "Duration must be positive")
    private double duration;

    private double startTime;

    public NoteDto() {}

    public NoteDto(String pitch, double duration) {
        this.pitch = pitch;
        this.duration = duration;
    }

    public String getPitch() {
        return pitch;
    }

    public void setPitch(String pitch) {
        this.pitch = pitch;
    }

    public double getDuration() {
        return duration;
    }

    public void setDuration(double duration) {
        this.duration = duration;
    }

    public double getStartTime() {
        return startTime;
    }

    public void setStartTime(double startTime) {
        this.startTime = startTime;
    }
}
