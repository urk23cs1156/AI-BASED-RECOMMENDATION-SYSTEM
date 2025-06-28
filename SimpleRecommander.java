import java.util.*;

public class SimpleRecommender {

    // User preferences: Map<UserID, Map<ItemID, Rating>>
    private static Map<Integer, Map<Integer, Double>> userRatings = new HashMap<>();

    public static void main(String[] args) {
        // Updated user ratings with better overlap
        userRatings.put(1, Map.of(101, 5.0, 102, 3.0, 104, 4.0));
        userRatings.put(2, Map.of(101, 4.0, 103, 4.0, 104, 5.0));
        userRatings.put(3, Map.of(102, 4.5, 103, 5.0, 104, 3.0));

        int targetUser = 1;
        List<Integer> recommendations = recommendItems(targetUser, 3);

        System.out.println("Recommendations for User " + targetUser + ": " + recommendations);
    }

    // Cosine similarity between two users
    private static double cosineSimilarity(Map<Integer, Double> ratings1, Map<Integer, Double> ratings2) {
        Set<Integer> commonItems = new HashSet<>(ratings1.keySet());
        commonItems.retainAll(ratings2.keySet());
        if (commonItems.isEmpty()) return 0;

        double dot = 0, norm1 = 0, norm2 = 0;
        for (int item : commonItems) {
            dot += ratings1.get(item) * ratings2.get(item);
        }
        for (double v : ratings1.values()) norm1 += v * v;
        for (double v : ratings2.values()) norm2 += v * v;

        if (norm1 == 0 || norm2 == 0) return 0;

        return dot / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    // Recommend items for target user based on similar users
    private static List<Integer> recommendItems(int userId, int maxRecommendations) {
        Map<Integer, Double> targetRatings = userRatings.get(userId);
        if (targetRatings == null) return Collections.emptyList();

        // Compute similarity scores with other users
        Map<Integer, Double> similarityScores = new HashMap<>();
        for (int otherUser : userRatings.keySet()) {
            if (otherUser != userId) {
                double sim = cosineSimilarity(targetRatings, userRatings.get(otherUser));
                similarityScores.put(otherUser, sim);
            }
        }

        // DEBUG: Print similarity scores
        System.out.println("Similarity Scores:");
        for (Map.Entry<Integer, Double> entry : similarityScores.entrySet()) {
            System.out.printf("User %d similarity: %.4f%n", entry.getKey(), entry.getValue());
        }

        // Weighted scores for items not rated by target user
        Map<Integer, Double> totals = new HashMap<>();
        Map<Integer, Double> simSums = new HashMap<>();

        for (Map.Entry<Integer, Double> entry : similarityScores.entrySet()) {
            int otherUser = entry.getKey();
            double sim = entry.getValue();

            if (sim <= 0) continue;  // exclude zero and negative similarities

            Map<Integer, Double> otherRatings = userRatings.get(otherUser);
            for (Map.Entry<Integer, Double> itemRating : otherRatings.entrySet()) {
                int item = itemRating.getKey();

                // Only consider items target user hasn't rated
                if (!targetRatings.containsKey(item)) {
                    totals.put(item, totals.getOrDefault(item, 0.0) + itemRating.getValue() * sim);
                    simSums.put(item, simSums.getOrDefault(item, 0.0) + sim);
                }
            }
        }

        // Calculate normalized scores
        Map<Integer, Double> rankings = new HashMap<>();
        for (int item : totals.keySet()) {
            rankings.put(item, totals.get(item) / simSums.get(item));
        }

        // Sort items by score descending
        List<Map.Entry<Integer, Double>> rankedItems = new ArrayList<>(rankings.entrySet());
        rankedItems.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        // Return top items
        List<Integer> recommendations = new ArrayList<>();
        for (int i = 0; i < Math.min(maxRecommendations, rankedItems.size()); i++) {
            recommendations.add(rankedItems.get(i).getKey());
        }
        return recommendations;
    }
}
