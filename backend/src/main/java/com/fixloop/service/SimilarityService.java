package com.fixloop.service;

import java.util.List;

/**
 * Strategy interface for computing textual similarity.
 * Default implementation: TF cosine similarity (TfCosineSimilarityService).
 * Swap point for future embedding model (e.g. ONNX MiniLM) — same interface, different @Primary impl.
 */
public interface SimilarityService {
    /**
     * Returns 0.0–1.0 similarity between the query string and the best-matching
     * document in the corpus. Returns 0.0 if query or corpus is null/empty.
     *
     * @param query  the new complaint description
     * @param corpus descriptions of all existing complaints in a candidate incident
     */
    double similarity(String query, List<String> corpus);
}
