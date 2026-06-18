package com.musicgame.chordmatcher.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "notes")
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String pitch;

    @Column(nullable = false)
    private double duration;

    @Column(name = "start_time")
    private double startTime;

    @Column(name = "note_order")
    private int order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "melody_id", nullable = false)
    private Melody melody;

    public Note() {}

    public Note(String pitch, double duration, int order) {
        this.pitch = pitch;
        this.duration = duration;
        this.order = order;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public Melody getMelody() {
        return melody;
    }

    public void setMelody(Melody melody) {
        this.melody = melody;
    }
}
