package controllers.marketplace;

import entities.MarketplaceConversation;

import java.util.HashMap;
import java.util.Map;

public class MarketplaceMessagingState {
    public MarketplaceConversation selectedConversation;
    public int pendingProductId = -1;
    public int pendingSellerId = -1;
    public final Map<Integer, String> productNameById = new HashMap<>();
    public final Map<Integer, String> userNameById = new HashMap<>();

    public boolean hasPendingMessageContext() {
        return pendingProductId > 0 && pendingSellerId > 0;
    }

    public void clearPendingMessageContext() {
        pendingProductId = -1;
        pendingSellerId = -1;
    }
}
