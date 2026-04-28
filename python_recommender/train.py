from __future__ import annotations

import argparse
import os
from pathlib import Path

from recommender import build_model_from_db, build_model


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Train the AgriSmart marketplace recommender")
    parser.add_argument("--products", help="Path to products CSV export")
    parser.add_argument("--interactions", help="Path to interactions CSV export")
    parser.add_argument("--db-host", default=os.getenv("AGRISMART_DB_HOST", "localhost"), help="MySQL host")
    parser.add_argument("--db-port", type=int, default=int(os.getenv("AGRISMART_DB_PORT", "3306")), help="MySQL port")
    parser.add_argument("--db-name", default=os.getenv("AGRISMART_DB_NAME", "agrismart"), help="MySQL database name")
    parser.add_argument("--db-user", default=os.getenv("AGRISMART_DB_USER", "root"), help="MySQL user")
    parser.add_argument("--db-password", default=os.getenv("AGRISMART_DB_PASSWORD", ""), help="MySQL password")
    parser.add_argument("--output", default="model.joblib", help="Path to save the trained model")
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    if args.products and args.interactions:
        model = build_model(args.products, args.interactions)
    else:
        model = build_model_from_db(
            host=args.db_host,
            port=args.db_port,
            database=args.db_name,
            user=args.db_user,
            password=args.db_password,
        )
    output_path = Path(args.output)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    model.save(output_path)
    print(f"Saved recommender model to {output_path}")


if __name__ == "__main__":
    main()
