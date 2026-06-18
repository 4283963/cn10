package com.musicgame.chordmatcher.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.musicgame.chordmatcher.config.ChordMatcherConfig;
import com.musicgame.chordmatcher.dto.NoteDto;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Service
public class PythonChordMatcherService {

    private static final Logger logger = LoggerFactory.getLogger(PythonChordMatcherService.class);

    private static final long PROCESS_TIMEOUT_SECONDS = 15;
    private static final int MAX_CONCURRENT_PROCESSES = 4;

    private final ChordMatcherConfig config;
    private final ObjectMapper objectMapper;
    private final Semaphore processSemaphore;
    private final ExecutorService streamExecutor;
    private final Path scriptAbsolutePath;

    public PythonChordMatcherService(ChordMatcherConfig config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.processSemaphore = new Semaphore(MAX_CONCURRENT_PROCESSES, true);
        this.streamExecutor = Executors.newFixedThreadPool(MAX_CONCURRENT_PROCESSES * 2, r -> {
            Thread t = new Thread(r, "chord-matcher-stream");
            t.setDaemon(true);
            return t;
        });
        this.scriptAbsolutePath = Paths.get(config.getScriptPath()).toAbsolutePath().normalize();
        logger.info("Chord matcher initialized. Script: {}, Max concurrent: {}, Timeout: {}s",
                scriptAbsolutePath, MAX_CONCURRENT_PROCESSES, PROCESS_TIMEOUT_SECONDS);
    }

    @PreDestroy
    public void shutdown() {
        streamExecutor.shutdownNow();
    }

    public ChordMatchResult matchChords(List<NoteDto> notes) throws IOException, InterruptedException, TimeoutException {
        return matchChords(notes, "folk");
    }

    public ChordMatchResult matchChords(List<NoteDto> notes, String style) throws IOException, InterruptedException, TimeoutException {
        String inputJson = buildInputJson(notes, style);

        if (!processSemaphore.tryAcquire(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            throw new TimeoutException("Too many chord matching requests. Please try again later.");
        }

        Process process = null;
        try {
            process = startProcess(inputJson);
            return collectResult(process);
        } finally {
            cleanupProcess(process);
            processSemaphore.release();
        }
    }

    private String buildInputJson(List<NoteDto> notes, String style) throws JsonProcessingException {
        Map<String, Object> input = new java.util.HashMap<>();
        input.put("notes", notes);
        input.put("style", style != null ? style : "folk");
        return objectMapper.writeValueAsString(input);
    }

    private Process startProcess(String inputJson) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(
                config.getPythonPath(),
                scriptAbsolutePath.toString()
        );
        processBuilder.redirectErrorStream(false);

        Process process = processBuilder.start();

        try (OutputStream stdin = process.getOutputStream()) {
            stdin.write(inputJson.getBytes(StandardCharsets.UTF_8));
            stdin.flush();
        }

        return process;
    }

    private ChordMatchResult collectResult(Process process) throws IOException, InterruptedException, TimeoutException {
        Future<String> stdoutFuture = streamExecutor.submit(() -> readStream(process.getInputStream()));
        Future<String> stderrFuture = streamExecutor.submit(() -> readStream(process.getErrorStream()));

        boolean finished = process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        if (!finished) {
            stderrFuture.cancel(true);
            String partialStdout = safeGetFuture(stdoutFuture, 1, TimeUnit.SECONDS);
            String partialStderr = safeGetFuture(stderrFuture, 1, TimeUnit.SECONDS);
            logger.error("Python process timed out after {}s. stdout={}, stderr={}",
                    PROCESS_TIMEOUT_SECONDS, partialStdout, partialStderr);
            throw new TimeoutException("Chord matching timed out after " + PROCESS_TIMEOUT_SECONDS + " seconds");
        }

        int exitCode = process.exitValue();
        String stdout = safeGetFuture(stdoutFuture, 2, TimeUnit.SECONDS);
        String stderr = safeGetFuture(stderrFuture, 2, TimeUnit.SECONDS);

        if (exitCode != 0) {
            logger.error("Python script exited with code {}. stdout={}, stderr={}", exitCode, stdout, stderr);
            throw new RuntimeException("Chord matching failed (exit code " + exitCode + "): " + (stderr.isEmpty() ? stdout : stderr));
        }

        if (stderr != null && !stderr.isEmpty()) {
            logger.warn("Python stderr: {}", stderr);
        }

        String resultJson = stdout.trim();
        if (resultJson.isEmpty()) {
            throw new RuntimeException("Chord matcher returned empty output");
        }

        return parseResult(resultJson);
    }

    private String readStream(InputStream inputStream) {
        StringBuilder sb = new StringBuilder();
        char[] buffer = new char[4096];
        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            int read;
            while ((read = reader.read(buffer)) != -1) {
                sb.append(buffer, 0, read);
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
            }
        } catch (IOException e) {
            logger.debug("Stream read interrupted: {}", e.getMessage());
        }
        return sb.toString();
    }

    private String safeGetFuture(Future<String> future, long timeout, TimeUnit unit) {
        try {
            return future.get(timeout, unit);
        } catch (Exception e) {
            future.cancel(true);
            return "";
        }
    }

    private void cleanupProcess(Process process) {
        if (process == null) {
            return;
        }
        try {
            if (process.isAlive()) {
                process.destroy();
                boolean terminated = process.waitFor(2, TimeUnit.SECONDS);
                if (!terminated) {
                    process.destroyForcibly();
                    process.waitFor(1, TimeUnit.SECONDS);
                    logger.warn("Process had to be forcibly destroyed");
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        } catch (Exception e) {
            logger.warn("Error cleaning up process: {}", e.getMessage());
        }
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
        if (root.has("style")) {
            result.setStyle(root.get("style").asText());
        }
        if (root.has("style_info")) {
            result.setStyleInfo(objectMapper.convertValue(root.get("style_info"),
                    new TypeReference<Map<String, Object>>() {}));
        }
        if (root.has("rhythm_pattern")) {
            result.setRhythmPattern(objectMapper.convertValue(root.get("rhythm_pattern"),
                    new TypeReference<Map<String, Object>>() {}));
        }
        if (root.has("accompaniment")) {
            result.setAccompaniment(objectMapper.convertValue(root.get("accompaniment"),
                    new TypeReference<List<Map<String, Object>>>() {}));
        }

        return result;
    }

    public static class ChordMatchResult {
        private List<String> chords;
        private String key;
        private String keyType;
        private int numMeasures;
        private double confidence;
        private String style;
        private Map<String, Object> styleInfo;
        private Map<String, Object> rhythmPattern;
        private List<Map<String, Object>> accompaniment;

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
}
