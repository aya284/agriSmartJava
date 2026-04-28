from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Iterable, Optional

import joblib
import mysql.connector
import pandas as pd
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import linear_kernel
from sklearn.preprocessing import MinMaxScaler


ACTION_WEIGHTS = {
    "purchase": 5.0,
    "cart": 3.0,
    "wishlist": 2.0,
    "view": 1.0,
}

PURCHASE_STATUSES = {"confirmee", "livree"}


@dataclass
class RecommenderConfig:
    product_weight: float = 0.65
    popularity_weight: float = 0.20
    recency_weight: float = 0.15


class MarketplaceRecommender:
    def __init__(self, products: pd.DataFrame, interactions: pd.DataFrame, config: Optional[RecommenderConfig] = None):
        self.products = products.copy()
        self.interactions = interactions.copy()
        self.config = config or RecommenderConfig()

        self.products["text_blob"] = self.products.apply(self._build_product_blob, axis=1)
        self.vectorizer = TfidfVectorizer(stop_words="english", min_df=1)
        self.product_matrix = self.vectorizer.fit_transform(self.products["text_blob"].fillna(""))

        self.products["popularity_score"] = self._compute_product_popularity(self.interactions)
        self.products["popularity_score"] = MinMaxScaler().fit_transform(self.products[["popularity_score"]].fillna(0.0))

    @staticmethod
    def _safe_text(value: object) -> str:
        if value is None:
            return ""
        return str(value).strip().lower()

    @staticmethod
    def _safe_number(value: object) -> float:
        try:
            if value is None or value == "":
                return 0.0
            return float(value)
        except (TypeError, ValueError):
            return 0.0

    def _build_product_blob(self, row: pd.Series) -> str:
        parts = [
            self._safe_text(row.get("nom")),
            self._safe_text(row.get("description")),
            self._safe_text(row.get("type")),
            self._safe_text(row.get("categorie")),
            self._safe_text(row.get("location_address")),
        ]
        return " ".join(part for part in parts if part)

    def _compute_product_popularity(self, interactions: pd.DataFrame) -> pd.Series:
        if interactions.empty or "product_id" not in interactions.columns:
            return pd.Series([0.0] * len(self.products), index=self.products.index)

        weighted = interactions.copy()
        weighted["action"] = weighted["action"].fillna("").str.lower()
        weighted["weight"] = weighted["action"].map(ACTION_WEIGHTS).fillna(0.5)

        popularity = weighted.groupby("product_id")["weight"].sum()
        return self.products["id"].map(popularity).fillna(0.0)

    def _build_user_profile_blob(self, user_id: int) -> str:
        profile_rows = self.interactions[self.interactions["user_id"] == user_id]
        if profile_rows.empty:
            return ""

        categories = sorted({self._safe_text(value) for value in profile_rows.get("category", []) if self._safe_text(value)})
        types = sorted({self._safe_text(value) for value in profile_rows.get("type", []) if self._safe_text(value)})
        sellers = sorted({self._safe_text(value) for value in profile_rows.get("seller_id", []) if self._safe_text(value)})
        price_bands = sorted({self._safe_text(value) for value in profile_rows.get("price_band", []) if self._safe_text(value)})

        return " ".join(categories + types + sellers + price_bands)

    def recommend(self, user_id: int, top_n: int = 10, exclude_product_ids: Optional[Iterable[int]] = None) -> pd.DataFrame:
        exclude_product_ids = set(exclude_product_ids or [])

        if self.products.empty:
            return self.products.head(0)

        user_profile_blob = self._build_user_profile_blob(user_id)
        ranked = self.products.copy()

        if not user_profile_blob:
            ranked = ranked.sort_values(["popularity_score", "created_at"], ascending=[False, False])
            ranked = ranked[~ranked["id"].isin(exclude_product_ids)]
            return ranked.head(top_n).reset_index(drop=True)

        user_vector = self.vectorizer.transform([user_profile_blob])
        similarity = linear_kernel(user_vector, self.product_matrix).flatten()
        ranked["similarity_score"] = similarity
        ranked["final_score"] = (
            self.config.product_weight * ranked["similarity_score"]
            + self.config.popularity_weight * ranked["popularity_score"]
        )
        ranked = ranked[~ranked["id"].isin(exclude_product_ids)]
        ranked = ranked.sort_values(["final_score", "created_at"], ascending=[False, False])
        return ranked.head(top_n).reset_index(drop=True)

    @classmethod
    def load(cls, model_path: str | Path) -> "MarketplaceRecommender":
        return joblib.load(model_path)

    def save(self, model_path: str | Path) -> None:
        joblib.dump(self, model_path)


def load_products(products_csv: str | Path) -> pd.DataFrame:
    df = pd.read_csv(products_csv)
    rename_map = {
        "locationAddress": "location_address",
        "promotionPrice": "promotion_price",
        "quantiteStock": "quantite_stock",
        "vendeurId": "vendeur_id",
    }
    df = df.rename(columns=rename_map)

    expected_columns = [
        "id",
        "nom",
        "description",
        "type",
        "prix",
        "categorie",
        "quantite_stock",
        "image",
        "isPromotion",
        "promotion_price",
        "location_address",
        "vendeur_id",
        "created_at",
    ]
    for column in expected_columns:
        if column not in df.columns:
            df[column] = "" if column in {"nom", "description", "type", "categorie", "image", "location_address", "created_at"} else 0

    df["created_at"] = pd.to_datetime(df["created_at"], errors="coerce").fillna(pd.Timestamp.now())
    return df


def load_interactions(interactions_csv: str | Path) -> pd.DataFrame:
    interactions = pd.read_csv(interactions_csv)

    required_columns = ["user_id", "product_id", "action", "category", "type", "seller_id", "price"]
    for column in required_columns:
        if column not in interactions.columns:
            interactions[column] = ""

    if "timestamp" not in interactions.columns:
        interactions["timestamp"] = pd.Timestamp.now()

    if "price_band" not in interactions.columns:
        interactions["price_band"] = interactions["price"].apply(_infer_price_band)

    interactions["action"] = interactions["action"].fillna("").astype(str).str.lower()
    interactions["category"] = interactions["category"].fillna("").astype(str)
    interactions["type"] = interactions["type"].fillna("").astype(str)
    interactions["seller_id"] = interactions["seller_id"].fillna("").astype(str)
    interactions["price_band"] = interactions["price_band"].fillna("").astype(str)
    interactions["user_id"] = pd.to_numeric(interactions["user_id"], errors="coerce").fillna(0).astype(int)
    interactions["product_id"] = pd.to_numeric(interactions["product_id"], errors="coerce").fillna(0).astype(int)
    return interactions


def _infer_price_band(value: object) -> str:
    price = 0.0
    try:
        price = float(value)
    except (TypeError, ValueError):
        return "unknown"

    if price < 20:
        return "budget"
    if price < 80:
        return "mid"
    return "premium"


def build_model(products_csv: str | Path, interactions_csv: str | Path) -> MarketplaceRecommender:
    products = load_products(products_csv)
    interactions = load_interactions(interactions_csv)
    return MarketplaceRecommender(products=products, interactions=interactions)


def build_model_from_db(host: str, port: int, database: str, user: str, password: str) -> MarketplaceRecommender:
    connection = mysql.connector.connect(
        host=host,
        port=port,
        database=database,
        user=user,
        password=password,
    )
    try:
        products = load_products_from_db(connection)
        interactions = load_interactions_from_db(connection)
        return MarketplaceRecommender(products=products, interactions=interactions)
    finally:
        connection.close()


def load_products_from_db(connection) -> pd.DataFrame:
    query = """
        SELECT
            id,
            nom,
            description,
            type,
            prix,
            categorie,
            quantite_stock,
            image,
            is_promotion,
            promotion_price,
            location_address,
            vendeur_id,
            created_at
        FROM produit
        WHERE banned = FALSE
    """
    products = pd.read_sql(query, connection)
    products["created_at"] = pd.to_datetime(products["created_at"], errors="coerce").fillna(pd.Timestamp.now())
    return products


def load_interactions_from_db(connection) -> pd.DataFrame:
    wishlist_query = """
        SELECT
            w.user_id,
            w.produit_id AS product_id,
            'wishlist' AS action,
            p.categorie AS category,
            p.type AS type,
            p.vendeur_id AS seller_id,
            p.prix AS price,
            w.created_at AS timestamp
        FROM wishlist_item w
        JOIN produit p ON p.id = w.produit_id
    """

    purchase_query = """
        SELECT
            c.client_id AS user_id,
            ci.produit_id AS product_id,
            'purchase' AS action,
            p.categorie AS category,
            p.type AS type,
            p.vendeur_id AS seller_id,
            ci.prix_unitaire AS price,
            c.created_at AS timestamp
        FROM commande c
        JOIN commande_item ci ON ci.commande_id = c.id
        JOIN produit p ON p.id = ci.produit_id
        WHERE LOWER(c.statut) IN ('confirmee', 'livree')
    """

    wishlist = pd.read_sql(wishlist_query, connection)
    purchases = pd.read_sql(purchase_query, connection)

    interactions = pd.concat([wishlist, purchases], ignore_index=True)
    if interactions.empty:
        return pd.DataFrame(columns=["user_id", "product_id", "action", "category", "type", "seller_id", "price", "timestamp", "price_band"])

    interactions["timestamp"] = pd.to_datetime(interactions["timestamp"], errors="coerce").fillna(pd.Timestamp.now())
    interactions["price_band"] = interactions["price"].apply(_infer_price_band)
    interactions["action"] = interactions["action"].fillna("").astype(str).str.lower()
    interactions["category"] = interactions["category"].fillna("").astype(str)
    interactions["type"] = interactions["type"].fillna("").astype(str)
    interactions["seller_id"] = pd.to_numeric(interactions["seller_id"], errors="coerce").fillna(0).astype(int)
    interactions["user_id"] = pd.to_numeric(interactions["user_id"], errors="coerce").fillna(0).astype(int)
    interactions["product_id"] = pd.to_numeric(interactions["product_id"], errors="coerce").fillna(0).astype(int)
    return interactions
