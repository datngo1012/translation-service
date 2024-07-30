package com.datngo.translation.migration;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TranslationGenerator {

    private static final int BATCH_SIZE = 1000;
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/translation";
    private static final String USER =  "postgres";
    private static final String PASS =  "postgres";

    private static final String ENG_LAG = "eng";
    private static final String VEI_LAG = "vie";

    public static void main(String[] args) throws IOException, SQLException {
        // Read and write translation data to a CSV file
        processTranslationFiles(
        );

        // Import CSV data to PostgreSQL
        importCsvToPostgres();
    }

    private static void processTranslationFiles() throws IOException {
        try (BufferedReader sentenceReader = new BufferedReader(new FileReader("./static/sentences.csv"));
             BufferedReader audioReader = new BufferedReader(new FileReader("./static/sentences_with_audio.csv"));
             BufferedReader linksReader = new BufferedReader(new FileReader("./static/links.csv"));
             FileWriter writer = new FileWriter("./static/english_vietnamese_translation.csv")) {

            writer.write("ID,Text,Audio URL,Translate ID,Translate Text\n");

            // Read sentences
            Map<String, Sentence> sentencesList = new HashMap<>();
            String line;
            while ((line = sentenceReader.readLine()) != null) {
                String[] parts = line.split("\t");
                if (parts.length == 3) {
                    sentencesList.put(parts[0], new Sentence(parts[1], parts[2]));
                }
            }

            // Read audio URLs
            Map<String, String> audioUrls = new HashMap<>();
            while ((line = audioReader.readLine()) != null) {
                String[] parts = line.split("\t");
                if (parts.length == 4) {
                    audioUrls.put(parts[0], parts[3]);
                }
            }

            // Read links and write to CSV in batches
            List<String[]> batch = new ArrayList<>();
            while ((line = linksReader.readLine()) != null) {
                String[] parts = line.split("\t");
                if (parts.length == 2) {
                    String sentenceId = parts[0];
                    String translateId = parts[1];
                    Sentence englishSentence = sentencesList.get(sentenceId);
                    Sentence vietnameseSentence = sentencesList.get(translateId);  // Assuming same file for simplicity
                    if (englishSentence != null && ENG_LAG.equals(englishSentence.lang)  && vietnameseSentence != null
                            && VEI_LAG.equals(vietnameseSentence.lang)) {
                        batch.add(new String[]{
                                sentenceId,
                                escapeForCSV(englishSentence.text),
                                audioUrls.get(sentenceId) != null && !audioUrls.get(sentenceId).equals("\\N") ?
                                        getTatoebaLinkAudio(sentenceId) : null,
                                translateId,
                                escapeForCSV(vietnameseSentence.text)
                        });

                        if (batch.size() >= BATCH_SIZE) {
                            writeBatch(writer, batch);
                            batch.clear();
                        }
                    }
                }
            }
            // Write any remaining records
            if (!batch.isEmpty()) {
                writeBatch(writer, batch);
            }
        }
    }

    private static void writeBatch(FileWriter writer, List<String[]> batch) throws IOException {
        for (String[] record : batch) {
            writer.write(String.join(",", record));
            writer.write("\n");
        }
    }

    private static void importCsvToPostgres()
            throws IOException, SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             BufferedReader reader = new BufferedReader(
                     new FileReader("./static/english_vietnamese_translation.csv"))
        ) {

            createTable(conn);

            String line;
            reader.readLine();  // Skip header
            String sql = "INSERT INTO translation (english_id, english_text, english_audio_url, vietnamese_id, " +
                    "vietnamese_text) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                List<String[]> batch = new ArrayList<>();
                while ((line = reader.readLine()) != null) {
                    String[] columns = line.split(",");
                    if (columns.length == 5) {
                        batch.add(columns);

                        if (batch.size() >= BATCH_SIZE) {
                            executeBatch(pstmt, batch);
                            batch.clear();
                        }
                    }
                }
                if (!batch.isEmpty()) {
                    executeBatch(pstmt, batch);
                }
            }
        }
    }

    private static void executeBatch(PreparedStatement pstmt, List<String[]> batch) throws SQLException {
        for (String[] columns : batch) {
            pstmt.setString(1, columns[0]);
            pstmt.setString(2, columns[1]);
            pstmt.setString(3, columns[2]);
            pstmt.setString(4, columns[3]);
            pstmt.setString(5, columns[4]);
            pstmt.addBatch();
        }
        pstmt.executeBatch();
    }

    private static void createTable(Connection conn) throws SQLException {
        try (var stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS translation (
                    id SERIAL PRIMARY KEY,
                    english_id VARCHAR(255),
                    english_text TEXT,
                    english_audio_url TEXT,
                    vietnamese_id VARCHAR(255),
                    vietnamese_text TEXT
                )
            """);
        }
    }

    private static String getTatoebaLinkAudio(String sentenceId) {
        return "https://audio.tatoeba.org/sentences/eng/" + sentenceId + ".mp3";
    }

    private static String escapeForCSV(String value) {
        if (value.contains("\"")) {
            value = value.replace("\"", "\"\"");
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            value = "\"" + value + "\"";
        }
        return value;
    }

    static class Sentence {
        String lang;
        String text;

        Sentence(String lang, String text) {
            this.lang = lang;
            this.text = text;
        }
    }
}
