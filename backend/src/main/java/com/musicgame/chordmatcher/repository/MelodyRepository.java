package com.musicgame.chordmatcher.repository;

import com.musicgame.chordmatcher.entity.Melody;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MelodyRepository extends JpaRepository<Melody, Long> {

    List<Melody> findByUserIdOrderByCreatedAtDesc(String userId);

    @Query("SELECT m FROM Melody m LEFT JOIN FETCH m.notes WHERE m.id = :id")
    Optional<Melody> findByIdWithNotes(Long id);

    @Query("SELECT m FROM Melody m LEFT JOIN FETCH m.chordMatch WHERE m.id = :id")
    Optional<Melody> findByIdWithChordMatch(Long id);

    List<Melody> findTop10ByOrderByCreatedAtDesc();
}
