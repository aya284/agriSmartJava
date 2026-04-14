package services;

import entities.CartItem;
import entities.Produit;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CartSessionService {

    private static final CartSessionService INSTANCE = new CartSessionService();

    private final Map<Integer, Map<Integer, CartItem>> userCarts = new ConcurrentHashMap<>();

    private CartSessionService() {
    }

    public static CartSessionService getInstance() {
        return INSTANCE;
    }

    public synchronized void addProduit(int userId, Produit produit, int quantite) {
        if (produit == null || produit.getId() <= 0 || quantite <= 0) {
            return;
        }

        Map<Integer, CartItem> cart = userCarts.computeIfAbsent(userId, key -> new LinkedHashMap<>());
        CartItem existing = cart.get(produit.getId());
        if (existing == null) {
            cart.put(produit.getId(), new CartItem(produit, quantite));
        } else {
            existing.increment(quantite);
        }
    }

    public synchronized void removeProduit(int userId, int produitId) {
        Map<Integer, CartItem> cart = userCarts.get(userId);
        if (cart == null) {
            return;
        }
        cart.remove(produitId);
        if (cart.isEmpty()) {
            userCarts.remove(userId);
        }
    }

    public synchronized void clear(int userId) {
        userCarts.remove(userId);
    }

    public synchronized List<CartItem> getItems(int userId) {
        Map<Integer, CartItem> cart = userCarts.get(userId);
        if (cart == null || cart.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(cart.values());
    }

    public synchronized int countItems(int userId) {
        return getItems(userId).stream().mapToInt(CartItem::getQuantite).sum();
    }

    public synchronized double getTotal(int userId) {
        return getItems(userId).stream().mapToDouble(CartItem::getLineTotal).sum();
    }
}
