package com.musicgame.chordmatcher.repository;

import com.musicgame.chordmatcher.entity.ChordMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChordMatchRepository extends JpaRepository<ChordMatch, Long> {

    Optional<ChordMatch> findByMelodyId(Long melodyId);
}
