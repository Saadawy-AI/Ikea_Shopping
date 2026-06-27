"""
app.py
------------------------------------------------------------
Streamlit Dashboard for the IKEA Shopping Analytics project.

Shows:
  - Actual historical sales per product (from warehouse.db)
  - Sales forecast for upcoming days (using forecast_model.joblib)
  - A product selector

Usage:
    pip install streamlit pandas joblib plotly
    streamlit run app.py

Run in this order the first time:
    1. python ../data_generation/generate_sales_data.py
    2. python ../etl/etl_pipeline.py
    3. python ../ml/train_forecast_model.py
    4. streamlit run app.py
"""

import os
import sqlite3
from datetime import timedelta

import joblib
import numpy as np
import pandas as pd
import plotly.graph_objects as go
import streamlit as st

# Build absolute paths based on this file's location, so the app works
# regardless of the working directory it's launched from (local machine,
# Streamlit Cloud, etc.)
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
WAREHOUSE_PATH = os.path.join(BASE_DIR, "..", "etl", "warehouse.db")
MODEL_PATH = os.path.join(BASE_DIR, "..", "ml", "forecast_model.joblib")
FORECAST_DAYS = 14


@st.cache_data
def load_data():
    conn = sqlite3.connect(WAREHOUSE_PATH)
    fact_sales = pd.read_sql("SELECT * FROM fact_sales", conn)
    dim_product = pd.read_sql("SELECT * FROM dim_product", conn)
    dim_date = pd.read_sql("SELECT * FROM dim_date", conn)
    conn.close()

    fact_sales["date"] = pd.to_datetime(fact_sales["date"])
    dim_date["date"] = pd.to_datetime(dim_date["date"])
    return fact_sales, dim_product, dim_date


@st.cache_resource
def load_model():
    bundle = joblib.load(MODEL_PATH)
    return bundle["model"], bundle["feature_columns"]


def build_forecast(model, feature_columns, product_id, history, dim_date_template):
    """Generates a recursive forecast for FORECAST_DAYS days ahead, using the
    last known actual values as the starting point."""
    history = history.sort_values("date").copy()
    last_known = history["quantity_sold"].tolist()

    last_date = history["date"].max()
    forecast_rows = []

    for step in range(1, FORECAST_DAYS + 1):
        forecast_date = last_date + timedelta(days=step)

        lag_1 = last_known[-1] if len(last_known) >= 1 else 0
        lag_7 = last_known[-7] if len(last_known) >= 7 else np.mean(last_known) if last_known else 0
        rolling_mean_7 = np.mean(last_known[-7:]) if last_known else 0

        day_of_week = forecast_date.dayofweek
        month = forecast_date.month
        is_weekend = int(day_of_week in (3, 4, 5))

        row = pd.DataFrame([{
            "product_id": product_id,
            "day_of_week": day_of_week,
            "month": month,
            "is_weekend": is_weekend,
            "lag_1": lag_1,
            "lag_7": lag_7,
            "rolling_mean_7": rolling_mean_7,
        }])[feature_columns]

        predicted = max(0.0, float(model.predict(row)[0]))
        forecast_rows.append({"date": forecast_date, "quantity_sold": predicted})
        last_known.append(predicted)

    return pd.DataFrame(forecast_rows)


def main():
    st.set_page_config(page_title="IKEA Sales Analytics", layout="wide")
    st.title("📊 IKEA Shopping — Sales Analytics & Forecasting")
    st.caption("Analytics dashboard: actual sales + ML forecast (XGBoost) — historical data is synthetic, generated for demo purposes")

    fact_sales, dim_product, dim_date = load_data()
    model, feature_columns = load_model()

    # ─── Product filter ──────────────────────────────────
    product_options = dim_product.set_index("product_id")["name"].to_dict()
    selected_product_id = st.selectbox(
        "Select a product",
        options=list(product_options.keys()),
        format_func=lambda pid: product_options.get(pid, str(pid)),
    )

    product_history = fact_sales[fact_sales["product_id"] == selected_product_id]

    if product_history.empty:
        st.warning("No sales data available for this product.")
        return

    forecast_df = build_forecast(model, feature_columns, selected_product_id, product_history, dim_date)

    # ─── KPIs ────────────────────────────────────────────
    col1, col2, col3 = st.columns(3)
    col1.metric("Total quantity sold (historical)", int(product_history["quantity_sold"].sum()))
    col2.metric("Total revenue (historical)", f"{product_history['revenue'].sum():,.0f} EGP")
    col3.metric(f"Forecasted quantity (next {FORECAST_DAYS} days)", f"{forecast_df['quantity_sold'].sum():.0f}")

    # ─── Chart ───────────────────────────────────────────
    fig = go.Figure()
    fig.add_trace(go.Scatter(
        x=product_history["date"], y=product_history["quantity_sold"],
        mode="lines", name="Actual sales", line=dict(color="#0058A3"),
    ))
    fig.add_trace(go.Scatter(
        x=forecast_df["date"], y=forecast_df["quantity_sold"],
        mode="lines+markers", name="Forecast", line=dict(color="#FFBD00", dash="dash"),
    ))
    fig.update_layout(
        title=f"Daily sales — {product_options.get(selected_product_id)}",
        xaxis_title="Date", yaxis_title="Quantity sold",
        legend=dict(orientation="h", yanchor="bottom", y=1.02),
        height=450,
    )
    st.plotly_chart(fig, use_container_width=True)

    # ─── Top products table ──────────────────────────────
    st.subheader("🏆 Top 10 products (total quantity sold)")
    top_products = (
        fact_sales.groupby("product_id")["quantity_sold"].sum()
        .sort_values(ascending=False).head(10).reset_index()
    )
    top_products["name"] = top_products["product_id"].map(product_options)
    st.dataframe(top_products[["name", "quantity_sold"]], use_container_width=True)


if __name__ == "__main__":
    main()
