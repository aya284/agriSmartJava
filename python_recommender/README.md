# AgriSmart Python Recommender

This folder contains a lightweight local recommender built with Python, pandas, scikit-learn, and MySQL.

## What it uses

The model can learn from these marketplace signals stored in MySQL:

- `wishlist` actions
- `purchase` actions
- product metadata such as `categorie`, `type`, `prix`, and `vendeur_id`

`cart` is currently session-based in the Java app, so it is not read from the database yet.

## Required CSV exports

### `products.csv`

Expected columns:

- `id`
- `nom`
- `description`
- `type`
- `prix`
- `categorie`
- `quantiteStock` or `quantite_stock`
- `image`
- `isPromotion`
- `promotionPrice` or `promotion_price`
- `locationAddress` or `location_address`
- `vendeurId` or `vendeur_id`
- `created_at`

### `interactions.csv`

Optional fallback mode. If you want to train without MySQL, use CSVs with these columns:

- `user_id`
- `product_id`
- `action` (`purchase`, `cart`, `wishlist`, `view`)
- `category`
- `type`
- `seller_id`
- `price`
- `timestamp` optional
- `price_band` optional

## Train the model

Install dependencies:

```bash
pip install -r requirements.txt
```

Train and save the model:

```bash
python train.py --products products.csv --interactions interactions.csv --output model.joblib
```

Or train directly from the MySQL database used by the Java app:

```bash
python train.py --output model.joblib --db-host localhost --db-port 3306 --db-name agrismart --db-user root --db-password ""
```

## Use it later

Load the saved model and ask for recommendations for a user id:

```python
from recommender import MarketplaceRecommender

model = MarketplaceRecommender.load("model.joblib")
recommendations = model.recommend(user_id=5, top_n=10)
print(recommendations[["id", "nom", "categorie", "final_score"]])
```

## Next integration step

Export the Java marketplace data into those CSV files, then plug the top results back into the JavaFX marketplace UI.
