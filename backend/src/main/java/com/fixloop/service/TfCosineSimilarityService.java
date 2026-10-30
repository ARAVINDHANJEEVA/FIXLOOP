package com.fixloop.service;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Plain TF (Term Frequency) cosine similarity — not TF-IDF.
 * IDF weighting is meaningless with 1–5 documents per incident corpus;
 * omitting it gives equivalent results with less code complexity.
 *
 * Algorithm:
 * 1. Tokenise each text: lowercase, strip non-alphanumeric, split on whitespace.
 * 2. Apply suffix-strip stemmer (removes -ing, -ed, -er, -s, -ly, -tion, -ness).
 * 3. Remove stopwords.
 * 4. Build TF vector for query and each corpus document.
 * 5. Return max cosine similarity over all corpus documents.
 */
@Service
@Primary
public class TfCosineSimilarityService implements SimilarityService {

    private static final Set<String> STOPWORDS = Set.of(
        "a", "an", "the", "in", "on", "at", "to", "for", "of", "and", "or", "but",
        "is", "are", "was", "were", "be", "been", "being", "have", "has", "had",
        "do", "does", "did", "will", "would", "could", "should", "may", "might",
        "i", "you", "we", "they", "he", "she", "it", "my", "our", "your", "their",
        "this", "that", "these", "those", "with", "from", "by", "as", "about",
        "not", "no", "so", "if", "then", "there", "here", "how", "when", "where",
        "very", "just", "also", "than", "more", "some", "any", "all"
    );

    @Override
    public double similarity(String query, List<String> corpus) {
        if (query == null || query.isBlank() || corpus == null || corpus.isEmpty()) return 0.0;

        Map<String, Double> queryVec = buildTfVector(query);
        if (queryVec.isEmpty()) return 0.0;

        double maxScore = 0.0;
        for (String doc : corpus) {
            if (doc == null || doc.isBlank()) continue;
            Map<String, Double> docVec = buildTfVector(doc);
            if (docVec.isEmpty()) continue;
            double score = cosine(queryVec, docVec);
            if (score > maxScore) maxScore = score;
        }
        return maxScore;
    }

    private Map<String, Double> buildTfVector(String text) {
        String[] tokens = text.toLowerCase().replaceAll("[^a-z0-9\\s]", " ").split("\\s+");
        Map<String, Integer> freq = new LinkedHashMap<>();
        for (String tok : tokens) {
            String stem = stem(tok);
            if (stem.length() >= 2 && !STOPWORDS.contains(stem)) {
                freq.merge(stem, 1, Integer::sum);
            }
        }
        if (freq.isEmpty()) return Collections.emptyMap();

        // Normalise by token count
        double total = freq.values().stream().mapToInt(Integer::intValue).sum();
        Map<String, Double> vec = new LinkedHashMap<>();
        freq.forEach((term, count) -> vec.put(term, count / total));
        return vec;
    }

    /**
     * Suffix-strip stemmer (lightweight Porter approximation).
     * Handles the most common English morphological variants found in campus complaint text.
     */
    private String stem(String word) {
        if (word.length() <= 4) return word;
        if (word.endsWith("tion")) return word.substring(0, word.length() - 3);
        if (word.endsWith("ness")) return word.substring(0, word.length() - 4);
        if (word.endsWith("ing") && word.length() > 6) return word.substring(0, word.length() - 3);
        if (word.endsWith("tion")) return word.substring(0, word.length() - 3);
        if (word.endsWith("ed") && word.length() > 5) return word.substring(0, word.length() - 2);
        if (word.endsWith("er") && word.length() > 5) return word.substring(0, word.length() - 2);
        if (word.endsWith("ly") && word.length() > 5) return word.substring(0, word.length() - 2);
        if (word.endsWith("s") && !word.endsWith("ss") && word.length() > 4) return word.substring(0, word.length() - 1);
        return word;
    }

    private double cosine(Map<String, Double> a, Map<String, Double> b) {
        double dot = 0.0;
        for (Map.Entry<String, Double> e : a.entrySet()) {
            Double bVal = b.get(e.getKey());
            if (bVal != null) dot += e.getValue() * bVal;
        }
        double normA = Math.sqrt(a.values().stream().mapToDouble(v -> v * v).sum());
        double normB = Math.sqrt(b.values().stream().mapToDouble(v -> v * v).sum());
        if (normA == 0 || normB == 0) return 0.0;
        return dot / (normA * normB);
    }
}
