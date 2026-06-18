package com.musicgame.chordmatcher.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.musicgame.chordmatcher.config.ChordMatcherConfig;
import com.musicgame.chordmatcher.dto.NoteDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class PythonChordMatcherService {

    private static final Logger logger = LoggerFactory.getLogger(PythonChordMatcherService.class);

    private final ChordMatcherConfig config;
    private final ObjectMapper objectMapper;

    public PythonChordMatcherService(ChordMatcherConfig config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
    }

    public ChordMatchResult matchChords(List<NoteDto> notes) throws IOException, InterruptedException {
        String notesJson = objectMapper.writeValueAsString(notes);

        Path scriptPath = Paths.get(config.getScriptPath()).toAbsolutePath().normalize();

        ProcessBuilder processBuilder = new ProcessBuilder(
                config.getPythonPath(),
                scriptPath.toString(),
                notesJson
        );

        processBuilder.redirectErrorStream(true);

        logger.info("Executing chord matching script: {} {}", config.getPythonPath(), scriptPath);

        Process process = processBuilder.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            logger.error("Python script exited with code {}: {}", exitCode, output);
            throw new RuntimeException("Chord matching failed with exit code: " + exitCode);
        }

        String resultJson = output.toString().trim();
        logger.debug("Chord matching result: {}", resultJson);

        return parseResult(resultJson);
    }

    private ChordMatchResult parseResult(String json) throws IOException {
        JsonNode root = objectMapper.readTree(json);

        if (root.has("error")) {
            throw new RuntimeException("Chord matcher error: " + root.get("error").asText());
        }

        ChordMatchResult result = new ChordMatchResult();

        List<String> chords = new ArrayList<>();
        JsonNode chordsNode = root.get("chords");
        if (chordsNode != null && chordsNode.isArray()) {
            for (JsonNode chord : chordsNode) {
                chords.add(chord.asText());
            }
        }
        result.setChords(chords);

        if (root.has("key")) {
            result.setKey(root.get("key").asText());
        }
        if (root.has("key_type")) {
            result.setKeyType(root.get("key_type").asText());
        }
        if (root.has("num_measures")) {
            result.setNumMeasures(root.get("num_measures").asInt());
        }
        if (root.has("confidence")) {
            result.setConfidence(root.get("confidence").asDouble());
        }

        return result;
    }

    public static class ChordMatchResult {
        private List<String> chords;
        private String key;
        private String keyType;
        private int numMeasures;
        private double confidence;

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
    }
}
