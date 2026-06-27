"""
generate_sales_data.py
------------------------------------------------------------
Generates realistic-looking historical sales data (synthetic)
for the IKEA Shopping project, and inserts it directly into
the real SQL Server database.

Why do we need this script?
The current data in the database (demo data) is very limited
(about 5 products, very few orders) and is not enough to train
a real forecasting model. This script generates ~18 months of
sales data with a realistic time pattern (seasonality + growth
trend + random noise) for every product that already exists in
the Products table.

Usage:
    pip install pyodbc numpy
    python generate_sales_data.py

Note: this script connects to SQL Server using the same
connection method used in DatabaseConnection.java (Windows
Integrated Security), so it runs directly on your machine
without any changes.
"""

import random
import math
from datetime import datetime, timedelta

import pyodbc
import numpy as np

# ============================================================
# 1) Connection settings - must match DatabaseConnection.java
# ============================================================
SERVER = "MOHAMOSTAFA"          # same server name as in the original code
DATABASE = "Ikea_Shopping"

CONN_STR = (
    f"DRIVER={{ODBC Driver 17 for SQL Server}};"
    f"SERVER={SERVER};"
    f"DATABASE={DATABASE};"
    f"Trusted_Connection=yes;"
    f"TrustServerCertificate=yes;"
)

# ============================================================
# 2) Data generation settings
# ============================================================
MONTHS_OF_HISTORY = 18          # number of months of data to generate
START_DATE = datetime.now() - timedelta(days=30 * MONTHS_OF_HISTORY)
END_DATE = datetime.now() - timedelta(days=1)

ORDER_STATUSES = ["delivered", "delivered", "delivered", "shipped",
                   "processing", "cancelled"]   # realistic distribution (mostly delivered)
PAYMENT_METHODS = ["credit_card", "cash_on_delivery", "debit_card", "paypal"]
PAYMENT_STATUSES_MAP = {
    "delivered": "completed",
    "shipped": "completed",
    "processing": "pending",
    "cancelled": "failed",
}


def seasonal_multiplier(day: datetime) -> float:
    """
    Simulates sales fluctuation across the year (seasonality) + a weekly pattern.
    - Higher sales in November/December (Black Friday + holidays)
    - Higher sales on Thursday/Friday/Saturday (weekend in Egypt)
    """
    month_factor = 1.0 + 0.5 * math.sin((day.month - 3) / 12 * 2 * math.pi)
    if day.month in (11, 12):
        month_factor *= 1.6  # holiday/sales season

    weekday = day.weekday()  # 0=Monday ... 6=Sunday
    weekend_factor = 1.4 if weekday in (3, 4, 5) else 1.0  # Thu/Fri/Sat

    return month_factor * weekend_factor


def growth_multiplier(day: datetime) -> float:
    """Simple gradual growth in sales over time (slight upward trend)."""
    progress = (day - START_DATE).days / max((END_DATE - START_DATE).days, 1)
    return 1.0 + 0.3 * progress


def fetch_users_and_addresses(cursor):
    cursor.execute("SELECT user_id FROM Users")
    user_ids = [row[0] for row in cursor.fetchall()]

    cursor.execute("SELECT address_id, user_id FROM Addresses")
    address_by_user = {}
    for address_id, user_id in cursor.fetchall():
        address_by_user.setdefault(user_id, []).append(address_id)

    return user_ids, address_by_user


def fetch_products(cursor):
    cursor.execute("SELECT product_id, price FROM Products WHERE is_active = 1")
    return cursor.fetchall()  # [(product_id, price), ...]


def generate_orders_for_day(day, user_ids, address_by_user, products, rng):
    """Generates a random number of orders for this day based on seasonality."""
    base_orders_per_day = 3
    multiplier = seasonal_multiplier(day) * growth_multiplier(day)
    num_orders = max(0, int(rng.poisson(base_orders_per_day * multiplier)))

    orders = []
    for _ in range(num_orders):
        user_id = rng.choice(user_ids)
        addresses = address_by_user.get(user_id)
        if not addresses:
            continue
        address_id = rng.choice(addresses)

        status = rng.choice(ORDER_STATUSES)

        # each order contains 1-4 different products
        num_items = rng.integers(1, 5)
        chosen_products = rng.choice(
            len(products), size=min(num_items, len(products)), replace=False
        )

        items = []
        total_price = 0.0
        for idx in chosen_products:
            product_id, price = products[idx]
            quantity = int(rng.integers(1, 4))
            unit_price = float(price)
            items.append((product_id, quantity, unit_price))
            total_price += quantity * unit_price

        created_at = day + timedelta(
            hours=int(rng.integers(8, 23)), minutes=int(rng.integers(0, 60))
        )

        orders.append({
            "user_id": int(user_id),
            "address_id": int(address_id),
            "status": status,
            "total_price": round(total_price, 2),
            "created_at": created_at,
            "items": items,
        })

    return orders


def insert_orders(cursor, orders):
    inserted = 0
    for order in orders:
        cursor.execute(
            """
            INSERT INTO Orders (user_id, address_id, status, total_price, created_at)
            OUTPUT INSERTED.order_id
            VALUES (?, ?, ?, ?, ?)
            """,
            order["user_id"], order["address_id"], order["status"],
            order["total_price"], order["created_at"],
        )
        order_id = cursor.fetchone()[0]

        for product_id, quantity, unit_price in order["items"]:
            cursor.execute(
                """
                INSERT INTO Order_Items (order_id, product_id, quantity, unit_price)
                VALUES (?, ?, ?, ?)
                """,
                order_id, product_id, quantity, unit_price,
            )

        payment_status = PAYMENT_STATUSES_MAP.get(order["status"], "pending")
        method = random.choice(PAYMENT_METHODS)
        paid_at = order["created_at"] if payment_status == "completed" else None
        cursor.execute(
            """
            INSERT INTO Payments (order_id, method, status, amount, paid_at)
            VALUES (?, ?, ?, ?, ?)
            """,
            order_id, method, payment_status, order["total_price"], paid_at,
        )

        inserted += 1
    return inserted


def main():
    print(f"Connecting to SQL Server: {SERVER}/{DATABASE} ...")
    conn = pyodbc.connect(CONN_STR)
    cursor = conn.cursor()

    user_ids, address_by_user = fetch_users_and_addresses(cursor)
    products = fetch_products(cursor)

    if not user_ids or not products:
        print("Not enough Users or Products found in the database. Make sure script.sql has been run first.")
        return

    print(f"Number of users: {len(user_ids)} | Number of products: {len(products)}")
    print(f"Date range: {START_DATE.date()} -> {END_DATE.date()} ({MONTHS_OF_HISTORY} months)")

    rng = np.random.default_rng(seed=42)  # fixed seed for reproducible data

    total_inserted = 0
    current_day = START_DATE
    batch_count = 0

    while current_day <= END_DATE:
        daily_orders = generate_orders_for_day(
            current_day, user_ids, address_by_user, products, rng
        )
        if daily_orders:
            total_inserted += insert_orders(cursor, daily_orders)
            batch_count += 1

        if batch_count % 30 == 0:
            conn.commit()
            print(f"  ... saved data up to {current_day.date()} | total orders so far: {total_inserted}")

        current_day += timedelta(days=1)

    conn.commit()
    cursor.close()
    conn.close()

    print(f"\nDone! Total orders generated: {total_inserted}")
    print("Data is now ready for the ETL and training stages.")


if __name__ == "__main__":
    main()
