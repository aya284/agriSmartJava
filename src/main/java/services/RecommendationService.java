package services;

import entities.Produit;
import utils.MyConnection;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Recommendation service that reads directly from the database to provide
 * personalized product recommendations based on user interactions
 * (wishlist items and purchase history).
 *
 * Mirrors the scoring logic from python_recommender/recommender.py.
 */
public class RecommendationService {

    private final Connection conn = MyConnection.getInstance().getConn();

    // Action weights matching Python recommender
    private static final double WEIGHT_PURCHASE = 5.0;
    private static final double WEIGHT_WISHLIST = 2.0;

    // Score blending weights
    private static final double W_SIMILARITY = 0.65;
    private static final double W_POPULARITY = 0.20;
    private static final double W_RECENCY = 0.15;

    /**
     * Returns a list of recommended products for the given user, ranked by score.
     * Reads live data from the database each time it is called.
     *
     * @param userId the current user's ID
     * @param topN   maximum number of recommendations to return
     * @return ordered list of recommended Produit objects (best first)
     */
    public List<Produit> recommend(int userId, int topN) throws SQLException {
        // 1. Load all non-banned products
        List<Produit> allProducts = loadProducts();
        if (allProducts.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. Load user interactions (wishlist + purchases)
        List<Interaction> interactions = loadUserInteractions(userId);

        // 3. Compute global popularity for each product
        Map<Integer, Double> popularityMap = computePopularity();

        // 4. If user has no interactions, return products sorted by popularity
        if (interactions.isEmpty()) {
            return allProducts.stream()
                    .sorted((a, b) -> {
                        double pa = popularityMap.getOrDefault(a.getId(), 0.0);
                        double pb = popularityMap.getOrDefault(b.getId(), 0.0);
                        int cmp = Double.compare(pb, pa);
                        if (cmp != 0) return cmp;
                        if (a.getCreatedAt() != null && b.getCreatedAt() != null) {
                            return b.getCreatedAt().compareTo(a.getCreatedAt());
                        }
                        return 0;
                    })
                    .limit(topN)
                    .collect(Collectors.toList());
        }

        // 5. Build user profile: weighted set of categories and types
        Map<String, Double> userProfile = buildUserProfile(interactions);

        // 6. Compute product-level scores
        // Normalize popularity to [0,1]
        double maxPop = popularityMap.values().stream().mapToDouble(v -> v).max().orElse(1.0);
        if (maxPop <= 0) maxPop = 1.0;

        // Set of product IDs the user already interacted with
        Set<Integer> interactedIds = interactions.stream()
                .map(i -> i.productId)
                .collect(Collectors.toSet());

        List<ScoredProduct> scored = new ArrayList<>();
        for (Produit p : allProducts) {
            double similarity = computeSimilarity(p, userProfile);
            double popularity = popularityMap.getOrDefault(p.getId(), 0.0) / maxPop;
            double recency = computeRecencyScore(p);
            double finalScore = W_SIMILARITY * similarity
                    + W_POPULARITY * popularity
                    + W_RECENCY * recency;

            // Small penalty if already interacted (user already knows about it)
            if (interactedIds.contains(p.getId())) {
                finalScore *= 0.5;
            }

            scored.add(new ScoredProduct(p, finalScore));
        }

        // 7. Sort descending by score, then by recency
        scored.sort((a, b) -> {
            int cmp = Double.compare(b.score, a.score);
            if (cmp != 0) return cmp;
            if (a.product.getCreatedAt() != null && b.product.getCreatedAt() != null) {
                return b.product.getCreatedAt().compareTo(a.product.getCreatedAt());
            }
            return 0;
        });

        return scored.stream()
                .limit(topN)
                .map(s -> s.product)
                .collect(Collectors.toList());
    }

    // ─── Private helpers ────────────────────────────────────────────────

    private List<Produit> loadProducts() throws SQLException {
        List<Produit> list = new ArrayList<>();
        String sql = "SELECT id, nom, description, type, prix, categorie, " +
                "quantite_stock, image, is_promotion, promotion_price, " +
                "location_address, vendeur_id, created_at " +
                "FROM produit WHERE banned = FALSE";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Produit p = new Produit();
                p.setId(rs.getInt("id"));
                p.setNom(rs.getString("nom"));
                p.setDescription(rs.getString("description"));
                p.setType(rs.getString("type"));
                p.setPrix(rs.getDouble("prix"));
                p.setCategorie(rs.getString("categorie"));
                p.setQuantiteStock(rs.getInt("quantite_stock"));
                p.setImage(rs.getString("image"));
                p.setPromotion(rs.getBoolean("is_promotion"));
                p.setPromotionPrice(rs.getDouble("promotion_price"));
                p.setLocationAddress(rs.getString("location_address"));
                p.setVendeurId(rs.getInt("vendeur_id"));
                Timestamp ca = rs.getTimestamp("created_at");
                if (ca != null) p.setCreatedAt(ca.toLocalDateTime());
                list.add(p);
            }
        }
        return list;
    }

    /**
     * Load user interactions from wishlist_item and commande/commande_item tables.
     * Mirrors the Python recommender's load_interactions_from_db().
     */
    private List<Interaction> loadUserInteractions(int userId) throws SQLException {
        List<Interaction> interactions = new ArrayList<>();

        // Wishlist interactions
        String wishlistSql =
                "SELECT w.produit_id, 'wishlist' AS action, p.categorie, p.type " +
                "FROM wishlist_item w " +
                "JOIN produit p ON p.id = w.produit_id " +
                "WHERE w.user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(wishlistSql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    interactions.add(new Interaction(
                            rs.getInt("produit_id"),
                            rs.getString("action"),
                            safe(rs.getString("categorie")),
                            safe(rs.getString("type"))
                    ));
                }
            }
        }

        // Purchase interactions
        String purchaseSql =
                "SELECT ci.produit_id, 'purchase' AS action, p.categorie, p.type " +
                "FROM commande c " +
                "JOIN commande_item ci ON ci.commande_id = c.id " +
                "JOIN produit p ON p.id = ci.produit_id " +
                "WHERE c.client_id = ? AND LOWER(c.statut) IN ('confirmee', 'livree')";
        try (PreparedStatement ps = conn.prepareStatement(purchaseSql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    interactions.add(new Interaction(
                            rs.getInt("produit_id"),
                            rs.getString("action"),
                            safe(rs.getString("categorie")),
                            safe(rs.getString("type"))
                    ));
                }
            }
        }

        return interactions;
    }

    /**
     * Compute global popularity for all products based on all users' interactions.
     */
    private Map<Integer, Double> computePopularity() throws SQLException {
        Map<Integer, Double> popularity = new HashMap<>();

        // Wishlist popularity
        String wishlistSql = "SELECT produit_id, COUNT(*) as cnt FROM wishlist_item GROUP BY produit_id";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(wishlistSql)) {
            while (rs.next()) {
                int pid = rs.getInt("produit_id");
                popularity.merge(pid, rs.getInt("cnt") * WEIGHT_WISHLIST, Double::sum);
            }
        }

        // Purchase popularity
        String purchaseSql =
                "SELECT ci.produit_id, COUNT(*) as cnt " +
                "FROM commande c " +
                "JOIN commande_item ci ON ci.commande_id = c.id " +
                "WHERE LOWER(c.statut) IN ('confirmee', 'livree') " +
                "GROUP BY ci.produit_id";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(purchaseSql)) {
            while (rs.next()) {
                int pid = rs.getInt("produit_id");
                popularity.merge(pid, rs.getInt("cnt") * WEIGHT_PURCHASE, Double::sum);
            }
        }

        return popularity;
    }

    /**
     * Build a weighted user profile from their interactions.
     * Keys are lowercase category and type strings; values are accumulated weights.
     */
    private Map<String, Double> buildUserProfile(List<Interaction> interactions) {
        Map<String, Double> profile = new HashMap<>();
        for (Interaction i : interactions) {
            double weight = "purchase".equals(i.action) ? WEIGHT_PURCHASE : WEIGHT_WISHLIST;
            if (!i.category.isEmpty()) {
                profile.merge(i.category.toLowerCase(), weight, Double::sum);
            }
            if (!i.type.isEmpty()) {
                profile.merge(i.type.toLowerCase(), weight, Double::sum);
            }
        }
        return profile;
    }

    /**
     * Compute similarity between a product and the user profile.
     * Simple overlap-based scoring: if product's category or type appears in the
     * user profile, add the profile weight to the score, then normalize.
     */
    private double computeSimilarity(Produit product, Map<String, Double> userProfile) {
        if (userProfile.isEmpty()) return 0.0;

        double score = 0.0;
        String cat = safe(product.getCategorie()).toLowerCase();
        String type = safe(product.getType()).toLowerCase();
        String desc = safe(product.getDescription()).toLowerCase();

        if (!cat.isEmpty() && userProfile.containsKey(cat)) {
            score += userProfile.get(cat);
        }
        if (!type.isEmpty() && userProfile.containsKey(type)) {
            score += userProfile.get(type);
        }

        // Bonus for description keywords that match profile keys
        for (Map.Entry<String, Double> entry : userProfile.entrySet()) {
            if (desc.contains(entry.getKey())) {
                score += entry.getValue() * 0.3;
            }
        }

        // Normalize: divide by sum of all profile weights so score is in [0, ~1]
        double totalProfileWeight = userProfile.values().stream().mapToDouble(v -> v).sum();
        return totalProfileWeight > 0 ? score / totalProfileWeight : 0.0;
    }

    /**
     * Score product recency: newer products get a higher recency score.
     * Products created within the last 7 days score 1.0, decaying linearly over 90 days.
     */
    private double computeRecencyScore(Produit product) {
        if (product.getCreatedAt() == null) return 0.0;
        long daysSinceCreation = java.time.temporal.ChronoUnit.DAYS.between(
                product.getCreatedAt().toLocalDate(),
                java.time.LocalDate.now()
        );
        if (daysSinceCreation <= 7) return 1.0;
        if (daysSinceCreation >= 90) return 0.0;
        return 1.0 - ((daysSinceCreation - 7.0) / 83.0);
    }

    /**
     * Find products similar to the given product based on category, type, and description.
     * Does NOT require user interaction data — purely content-based similarity.
     *
     * @param product the product to find similar items for
     * @param topN    maximum number of similar products to return
     * @return ordered list of similar products (most similar first), excluding the input product
     */
    public List<Produit> findSimilar(Produit product, int topN) throws SQLException {
        List<Produit> allProducts = loadProducts();
        if (allProducts.isEmpty() || product == null) {
            return Collections.emptyList();
        }

        String targetCat = safe(product.getCategorie()).toLowerCase();
        String targetType = safe(product.getType()).toLowerCase();
        String targetDesc = safe(product.getDescription()).toLowerCase();
        double targetPrice = product.isPromotion() ? product.getPromotionPrice() : product.getPrix();

        List<ScoredProduct> scored = new ArrayList<>();
        for (Produit p : allProducts) {
            if (p.getId() == product.getId()) continue; // skip itself

            double score = 0.0;
            String cat = safe(p.getCategorie()).toLowerCase();
            String type = safe(p.getType()).toLowerCase();
            String desc = safe(p.getDescription()).toLowerCase();

            // Category match is the strongest signal
            if (!targetCat.isEmpty() && targetCat.equals(cat)) {
                score += 3.0;
            }

            // Type match (vente/location)
            if (!targetType.isEmpty() && targetType.equals(type)) {
                score += 1.5;
            }

            // Description keyword overlap
            if (!targetDesc.isEmpty() && !desc.isEmpty()) {
                String[] targetWords = targetDesc.split("\\s+");
                for (String word : targetWords) {
                    if (word.length() > 3 && desc.contains(word)) {
                        score += 0.3;
                    }
                }
            }

            // Price proximity bonus (closer price = more similar)
            if (targetPrice > 0) {
                double pPrice = p.isPromotion() ? p.getPromotionPrice() : p.getPrix();
                double ratio = Math.min(targetPrice, pPrice) / Math.max(targetPrice, pPrice);
                score += ratio * 0.8;
            }

            // Recency bonus
            score += computeRecencyScore(p) * 0.3;

            if (score > 0) {
                scored.add(new ScoredProduct(p, score));
            }
        }

        scored.sort((a, b) -> {
            int cmp = Double.compare(b.score, a.score);
            if (cmp != 0) return cmp;
            if (a.product.getCreatedAt() != null && b.product.getCreatedAt() != null) {
                return b.product.getCreatedAt().compareTo(a.product.getCreatedAt());
            }
            return 0;
        });

        return scored.stream()
                .limit(topN)
                .map(s -> s.product)
                .collect(Collectors.toList());
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    // ─── Inner classes ──────────────────────────────────────────────────

    private static class Interaction {
        final int productId;
        final String action;
        final String category;
        final String type;

        Interaction(int productId, String action, String category, String type) {
            this.productId = productId;
            this.action = action == null ? "" : action.toLowerCase();
            this.category = category == null ? "" : category;
            this.type = type == null ? "" : type;
        }
    }

    private static class ScoredProduct {
        final Produit product;
        final double score;

        ScoredProduct(Produit product, double score) {
            this.product = product;
            this.score = score;
        }
    }
}
