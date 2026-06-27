"""
etl_pipeline.py
------------------------------------------------------------
ETL Pipeline for the IKEA Shopping project.

Stages:
  1. Extract  : read operational (OLTP) data from SQL Server
                (Orders, Order_Items, Products, Categories tables)
  2. Transform: aggregate sales daily per product + build
                dimension tables (Dim_Product, Dim_Date) and a
                fact table (Fact_Sales)
  3. Load     : save the result into a local Data Warehouse
                (SQLite), easy to use afterwards with the ML
                model and the dashboard, without needing a
                permanent connection to SQL Server.

Usage:
    pip install pyodbc pandas sqlalchemy
    python etl_pipeline.py

Output: a warehouse.db (SQLite) file containing:
    - dim_product
    - dim_date
    - fact_sales   (sales aggregated daily per product)
"""

import pyodbc
import pandas as pd
import sqlite3
from datetime import datetime

# ============================================================
# Connection settings - same as DatabaseConnection.java
# ============================================================
SERVER = "MOHAMOSTAFA"
DATABASE = "Ikea_Shopping"

SOURCE_CONN_STR = (
    f"DRIVER={{ODBC Driver 17 for SQL Server}};"
    f"SERVER={SERVER};"
    f"DATABASE={DATABASE};"
    f"Trusted_Connection=yes;"
    f"TrustServerCertificate=yes;"
)

WAREHOUSE_PATH = "warehouse.db"


# ============================================================
# 1) EXTRACT
# ============================================================
def extract():
    print("[1/3] Extract: reading data from SQL Server ...")
    conn = pyodbc.connect(SOURCE_CONN_STR)

    orders = pd.read_sql(
        "SELECT order_id, status, created_at FROM Orders", conn
    )
    order_items = pd.read_sql(
        "SELECT order_item_id, order_id, product_id, quantity, unit_price FROM Order_Items",
        conn,
    )
    products = pd.read_sql(
        "SELECT product_id, category_id, name, price FROM Products", conn
    )
    categories = pd.read_sql(
        "SELECT category_id, name AS category_name FROM Categories", conn
    )

    conn.close()
    print(f"      Orders: {len(orders)} | Order_Items: {len(order_items)} | Products: {len(products)}")
    return orders, order_items, products, categories


# ============================================================
# 2) TRANSFORM
# ============================================================
def transform(orders, order_items, products, categories):
    print("[2/3] Transform: building the Star Schema (Fact_Sales + Dim_Product + Dim_Date) ...")

    # exclude cancelled orders from the actual sales data
    valid_orders = orders[orders["status"] != "cancelled"].copy()
    valid_orders["created_at"] = pd.to_datetime(valid_orders["created_at"])
    valid_orders["order_date"] = valid_orders["created_at"].dt.date

    # join Order_Items with the order date
    sales = order_items.merge(
        valid_orders[["order_id", "order_date"]], on="order_id", how="inner"
    )

    # aggregate sales daily per product
    fact_sales = (
        sales.groupby(["order_date", "product_id"])
        .agg(
            quantity_sold=("quantity", "sum"),
            revenue=("unit_price", lambda x: (x * sales.loc[x.index, "quantity"]).sum()),
            num_orders=("order_id", "nunique"),
        )
        .reset_index()
        .rename(columns={"order_date": "date"})
    )

    # Dim_Product: join products with their categories
    dim_product = products.merge(categories, on="category_id", how="left")
    dim_product = dim_product[["product_id", "name", "category_name", "price"]]

    # Dim_Date: every date in the full time range (from first to last data point)
    # important: we generate every consecutive day, not just days that had sales,
    # because some days may have zero sales for any product, and they need to
    # stay present in Dim_Date so the ML model sees a real zero, not a missing value.
    full_date_range = pd.date_range(fact_sales["date"].min(), fact_sales["date"].max(), freq="D")
    dim_date = pd.DataFrame({"date": full_date_range})
    dim_date["day_of_week"] = dim_date["date"].dt.dayofweek
    dim_date["day_name"] = dim_date["date"].dt.day_name()
    dim_date["month"] = dim_date["date"].dt.month
    dim_date["year"] = dim_date["date"].dt.year
    dim_date["is_weekend"] = dim_date["day_of_week"].isin([3, 4, 5])  # Thu/Fri/Sat

    print(f"      Fact_Sales rows: {len(fact_sales)} | Dim_Product: {len(dim_product)} | Dim_Date: {len(dim_date)}")
    return fact_sales, dim_product, dim_date


# ============================================================
# 3) LOAD
# ============================================================
def load(fact_sales, dim_product, dim_date):
    print(f"[3/3] Load: saving to {WAREHOUSE_PATH} ...")
    conn = sqlite3.connect(WAREHOUSE_PATH)

    fact_sales["date"] = fact_sales["date"].astype(str)
    dim_date["date"] = dim_date["date"].astype(str)

    fact_sales.to_sql("fact_sales", conn, if_exists="replace", index=False)
    dim_product.to_sql("dim_product", conn, if_exists="replace", index=False)
    dim_date.to_sql("dim_date", conn, if_exists="replace", index=False)

    conn.close()
    print("      Saved successfully.")


def main():
    started_at = datetime.now()
    orders, order_items, products, categories = extract()
    fact_sales, dim_product, dim_date = transform(orders, order_items, products, categories)
    load(fact_sales, dim_product, dim_date)
    print(f"\nETL finished in {(datetime.now() - started_at).total_seconds():.1f} seconds.")
    print(f"Data Warehouse ready at: {WAREHOUSE_PATH}")


if __name__ == "__main__":
    main()
