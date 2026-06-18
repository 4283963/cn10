-- Music Game Database Schema
-- Chord Matcher Backend

-- Create database (run this first if needed)
-- CREATE DATABASE music_game;

-- Connect to music_game database before running below

-- Melodies table
CREATE TABLE IF NOT EXISTS melodies (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL DEFAULT 'Untitled Melody',
    user_id VARCHAR(255),
    note_count INTEGER NOT NULL DEFAULT 0,
    total_duration DOUBLE PRECISION NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_melodies_user_id ON melodies(user_id);
CREATE INDEX IF NOT EXISTS idx_melodies_created_at ON melodies(created_at DESC);

-- Notes table
CREATE TABLE IF NOT EXISTS notes (
    id SERIAL PRIMARY KEY,
    melody_id INTEGER NOT NULL REFERENCES melodies(id) ON DELETE CASCADE,
    pitch VARCHAR(10) NOT NULL,
    duration DOUBLE PRECISION NOT NULL,
    start_time DOUBLE PRECISION DEFAULT 0,
    note_order INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_notes_melody_id ON notes(melody_id);
CREATE INDEX IF NOT EXISTS idx_notes_order ON notes(melody_id, note_order);

-- Chord matches table
CREATE TABLE IF NOT EXISTS chord_matches (
    id SERIAL PRIMARY KEY,
    melody_id INTEGER NOT NULL UNIQUE REFERENCES melodies(id) ON DELETE CASCADE,
    chords_json TEXT NOT NULL,
    musical_key VARCHAR(10),
    key_type VARCHAR(20),
    style VARCHAR(20),
    num_measures INTEGER DEFAULT 0,
    confidence DOUBLE PRECISION DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_chord_matches_melody_id ON chord_matches(melody_id);
CREATE INDEX IF NOT EXISTS idx_chord_matches_key ON chord_matches(musical_key);

-- Trigger for updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

DROP TRIGGER IF EXISTS update_melodies_updated_at ON melodies;
CREATE TRIGGER update_melodies_updated_at
    BEFORE UPDATE ON melodies
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Insert some sample data
INSERT INTO melodies (title, user_id, note_count, total_duration) VALUES
('Sample Melody 1', 'demo_user', 8, 8.0)
ON CONFLICT DO NOTHING;

WITH sample_melody AS (
    SELECT id FROM melodies WHERE title = 'Sample Melody 1' LIMIT 1
)
INSERT INTO notes (melody_id, pitch, duration, start_time, note_order)
SELECT 
    sm.id,
    unnest(ARRAY['C', 'D', 'E', 'F', 'E', 'D', 'C', 'G']),
    unnest(ARRAY[1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0]),
    unnest(ARRAY[0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0]),
    unnest(ARRAY[0, 1, 2, 3, 4, 5, 6, 7])
FROM sample_melody sm
WHERE NOT EXISTS (
    SELECT 1 FROM notes n WHERE n.melody_id = sm.id
);
