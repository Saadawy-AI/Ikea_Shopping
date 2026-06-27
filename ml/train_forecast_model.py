"""
train_forecast_model.py
------------------------------------------------------------
Trains an XGBoost model to forecast daily sales quantity per
product, using the Data Warehouse (warehouse.db) built by
etl_pipeline.py.

Steps:
  1. Load fact_sales + dim_product + dim_date from warehouse.db
  2. Feature Engineering:
       - time-based features (day of week, month, weekend flag)
       - lag features (sales from previous days for the same product)
       - rolling average (7-day moving average)
  3. Train/Test split done chronologically (not randomly, since
     this is a time series problem)
  4. Train XGBoost + evaluate with MAE and RMSE
  5. Save the model (forecast_model.joblib) to be used by the dashboard

Usage:
    pip install pandas scikit-learn xgboost joblib
    python train_forecast_model.py
"""

import sqlite3
import pandas as pd
import numpy as np
import joblib
from sklearn.metrics import mean_absolute_error, mean_squared_error
from xgboost import XGBRegressor

WAREHOUSE_PATH = "../etl/warehouse.db"
MODEL_OUTPUT_PATH = "forecast_model.joblib"

FEATURE_COLUMNS = [
    "product_id", "day_of_week", "month", "is_weekend",
    "lag_1", "lag_7", "rolling_mean_7",
]
TARGET_COLUMN = "quantity_sold"


def load_warehouse_data():
    print("[1/5] Loading data from warehouse.db ...")
    conn = sqlite3.connect(WAREHOUSE_PATH)
    fact_sales = pd.read_sql("SELECT * FROM fact_sales", conn)
    dim_date = pd.read_sql("SELECT * FROM dim_date", conn)
    conn.close()

    fact_sales["date"] = pd.to_datetime(fact_sales["date"])
    dim_date["date"] = pd.to_datetime(dim_date["date"])
    return fact_sales, dim_date


def build_full_grid(fact_sales):
    """
    On a day where a given product had no sales, we record a zero
    instead of leaving it missing. This is essential for a time
    series model - it needs to see the zeros too.
    """
    all_dates = pd.date_range(fact_sales["date"].min(), fact_sales["date"].max(), freq="D")
    all_products = fact_sales["product_id"].unique()

    full_grid = pd.MultiIndex.from_product(
        [all_dates, all_products], names=["date", "product_id"]
    ).to_frame(index=False)

    merged = full_grid.merge(fact_sales, on=["date", "product_id"], how="left")
    merged["quantity_sold"] = merged["quantity_sold"].fillna(0)
    merged["revenue"] = merged["revenue"].fillna(0)
    return merged


def engineer_features(df, dim_date):
    print("[2/5] Building features (lag + rolling average + time-based features) ...")
    df = df.merge(dim_date, on="date", how="left")
    df = df.sort_values(["product_id", "date"])

    df["lag_1"] = df.groupby("product_id")["quantity_sold"].shift(1)
    df["lag_7"] = df.groupby("product_id")["quantity_sold"].shift(7)
    df["rolling_mean_7"] = (
        df.groupby("product_id")["quantity_sold"]
        .shift(1)
        .rolling(window=7, min_periods=1)
        .mean()
        .reset_index(drop=True)
    )

    df["is_weekend"] = df["is_weekend"].astype(int)
    df = df.dropna(subset=["lag_1", "lag_7", "rolling_mean_7"])
    return df


def time_based_split(df, test_ratio=0.15):
    """Chronological split: most recent period is the test set, the rest is train.
    A random split is never appropriate for time series."""
    df = df.sort_values("date")
    split_date = df["date"].quantile(1 - test_ratio, interpolation="nearest")
    train = df[df["date"] < split_date]
    test = df[df["date"] >= split_date]
    return train, test


def train_model(train_df):
    print("[3/5] Training the XGBoost model ...")
    X_train = train_df[FEATURE_COLUMNS]
    y_train = train_df[TARGET_COLUMN]

    model = XGBRegressor(
        n_estimators=300,
        max_depth=5,
        learning_rate=0.05,
        subsample=0.8,
        colsample_bytree=0.8,
        random_state=42,
    )
    model.fit(X_train, y_train)
    return model


def evaluate_model(model, test_df):
    print("[4/5] Evaluating the model on the test set ...")
    X_test = test_df[FEATURE_COLUMNS]
    y_test = test_df[TARGET_COLUMN]

    predictions = model.predict(X_test)
    predictions = np.clip(predictions, 0, None)  # quantity cannot be negative

    mae = mean_absolute_error(y_test, predictions)
    rmse = np.sqrt(mean_squared_error(y_test, predictions))

    print(f"      MAE  (mean absolute error)      : {mae:.2f} units/day")
    print(f"      RMSE (root mean squared error)   : {rmse:.2f} units/day")
    return mae, rmse


def main():
    fact_sales, dim_date = load_warehouse_data()
    full_grid = build_full_grid(fact_sales)
    features_df = engineer_features(full_grid, dim_date)

    train_df, test_df = time_based_split(features_df)
    print(f"      Train: {len(train_df)} rows | Test: {len(test_df)} rows")

    model = train_model(train_df)
    evaluate_model(model, test_df)

    print(f"[5/5] Saving the model to {MODEL_OUTPUT_PATH} ...")
    joblib.dump({
        "model": model,
        "feature_columns": FEATURE_COLUMNS,
    }, MODEL_OUTPUT_PATH)

    print("\nTraining completed successfully. The model is ready for the dashboard.")


if __name__ == "__main__":
    main()
