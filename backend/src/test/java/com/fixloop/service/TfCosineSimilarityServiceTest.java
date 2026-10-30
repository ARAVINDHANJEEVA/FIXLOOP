package com.fixloop.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for TfCosineSimilarityService.
 *
 * Acceptance criteria (from implementation plan Phase 5):
 *  - Identical text       → cosine ≥ 0.99
 *  - Completely unrelated → cosine < 0.25
 *  - Paraphrased campus complaint text → cosine ≥ 0.35 (above semantic floor)
 *  - Empty / null inputs  → 0.0 (no exception)
 */
class TfCosineSimilarityServiceTest {

    private TfCosineSimilarityService service;

    @BeforeEach
    void setUp() {
        service = new TfCosineSimilarityService();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Edge cases
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Null query returns 0.0")
    void nullQuery() {
        assertThat(service.similarity(null, List.of("some text"))).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Empty corpus returns 0.0")
    void emptyCorpus() {
        assertThat(service.similarity("water leaking", List.of())).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Blank query returns 0.0")
    void blankQuery() {
        assertThat(service.similarity("   ", List.of("water leakage pipe"))).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Null corpus returns 0.0")
    void nullCorpus() {
        assertThat(service.similarity("water leaking", null)).isEqualTo(0.0);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Identical text
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Identical text produces cosine ≥ 0.99")
    void identicalText() {
        String text = "Water leaking from pipe near washroom on second floor";
        double score = service.similarity(text, List.of(text));
        assertThat(score).isGreaterThanOrEqualTo(0.99);
    }

    @Test
    @DisplayName("Case-insensitive identical text produces cosine ≥ 0.99")
    void caseInsensitiveIdentical() {
        String query = "WATER LEAKING FROM PIPE NEAR WASHROOM";
        String corpus = "water leaking from pipe near washroom";
        double score = service.similarity(query, List.of(corpus));
        assertThat(score).isGreaterThanOrEqualTo(0.99);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Completely unrelated text
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Completely unrelated complaints score < 0.25")
    void unrelatedText() {
        String query  = "Water leaking from broken pipe near washroom";
        String corpus = "WiFi connection not working in library, no internet access";
        double score = service.similarity(query, List.of(corpus));
        assertThat(score).isLessThan(0.25);
    }

    @Test
    @DisplayName("Food complaint vs maintenance complaint scores < 0.25")
    void differentCategories() {
        String query  = "Mess food quality very poor, rice undercooked";
        String corpus = "Electrical short circuit in room 305, sparks visible";
        double score = service.similarity(query, List.of(corpus));
        assertThat(score).isLessThan(0.25);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Paraphrased campus complaint text (same issue, different wording)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Paraphrased water leakage complaints score ≥ 0.35 (above semantic floor)")
    void paraphrasedWaterLeakage() {
        String query  = "There is water dripping from the ceiling pipe near the washroom";
        String corpus = "Pipe burst on second floor corridor, water leaking onto floor";
        double score = service.similarity(query, List.of(corpus));
        assertThat(score).isGreaterThanOrEqualTo(0.35);
    }

    @Test
    @DisplayName("Same WiFi issue described differently scores ≥ 0.35")
    void paraphrasedWifiIssue() {
        String query  = "Internet not working in lab, WiFi not connecting";
        String corpus = "No WiFi signal in computer lab, unable to access network";
        double score = service.similarity(query, List.of(corpus));
        assertThat(score).isGreaterThanOrEqualTo(0.35);
    }

    @Test
    @DisplayName("Power outage described differently scores ≥ 0.35")
    void paraphrasedPowerOutage() {
        String query  = "Electricity gone in Block B, no power in room";
        String corpus = "Power cut in hostel Block B, lights and fans not working";
        double score = service.similarity(query, List.of(corpus));
        assertThat(score).isGreaterThanOrEqualTo(0.35);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Multi-document corpus (MAX similarity)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Returns MAX score when one of multiple corpus docs matches well")
    void maxScoreFromCorpus() {
        String query = "Water dripping from ceiling pipe near washroom corridor";
        List<String> corpus = List.of(
            "WiFi not working in lab",                                // unrelated
            "Mess food tasteless and cold today",                     // unrelated
            "Pipe burst near washroom, water leaking continuously"    // related
        );
        double score = service.similarity(query, corpus);
        // Should match the related document
        assertThat(score).isGreaterThanOrEqualTo(0.35);
    }

    @Test
    @DisplayName("Corpus with only unrelated docs scores < 0.25")
    void unrelatedCorpusAllDocs() {
        String query = "Water leaking from broken pipe in hostel bathroom";
        List<String> corpus = List.of(
            "WiFi no signal in library second floor",
            "Examination hall chairs broken and damaged",
            "AC not working in seminar hall room 401"
        );
        double score = service.similarity(query, corpus);
        assertThat(score).isLessThan(0.25);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Semantic floor validation (critical for clustering accuracy)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Boundary: water+WiFi should score below semantic floor (0.35)")
    void waterAndWifiBelowFloor() {
        String query  = "Water leaking from pipe, floor wet";
        String corpus = "WiFi slow and disconnecting frequently";
        double score = service.similarity(query, List.of(corpus));
        // These are different issues — score must stay below semantic floor
        assertThat(score).isLessThan(0.35);
    }
}
