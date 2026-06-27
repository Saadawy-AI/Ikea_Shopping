# IKEA Shopping — Admin Panel + Data & ML Extension

A two-layer project: an operational **Java Swing admin panel** backed by
SQL Server, extended with a **Data Engineering + Machine Learning** layer
that turns the operational sales data into a forecasting dashboard.

**🚀 Live demo:** https://saadawy-ai-ikea-shopping-dashboardapp-kpdswe.streamlit.app/

## Project structure
ikea-shopping-data-ml-extension/

├── java-app/              # Original admin panel (Java Swing + SQL Server)

├── data_generation/       # Generates realistic historical sales data

├── etl/                   # ETL pipeline: SQL Server -> Data Warehouse (SQLite)

├── ml/                    # XGBoost training for sales forecasting

├── dashboard/             # Streamlit dashboard (actual sales + forecast)

└── requirements.txt       # Python dependencies for the Data/ML layer

## 1) java-app/ — Admin Panel (OLTP)

A desktop CRUD application built with **Java Swing**, backed by **MS SQL
Server**.

- Login system with **SHA-256** password hashing (`PasswordUtils.java`)
- Role-based access control (Admin / User)
- CRUD panels: Products, Orders, Users (`ProductsPanel.java`,
  `OrdersPanel.java`, `UsersPanel.java`)
- Dashboard panel showing live KPIs (`DashboardPanel.java`)
- Database schema (10 tables: Users, Products, Categories, Orders,
  Order_Items, Cart, Cart_Items, Addresses, Payments, Reviews) defined in
  `script.sql`

### How to run

1. Run `script.sql` on your SQL Server instance to create the database
   and schema.
2. Update the connection details in `DatabaseConnection.java` if needed
   (default uses Windows Integrated Security on a local server).
3. Run `run.bat`, or open the project in NetBeans and run it directly, or
   run the prebuilt `ikea_shopping.jar` (in `java-app/dist/` or
   `java-app/`) with:
```bash
   java -jar ikea_shopping.jar
```

## 2) Data & ML Extension (data_generation / etl / ml / dashboard)

This layer was added on top of the same SQL Server database, without
modifying any Java code. It turns the operational data into an
analytics-ready data product:
┌──────────────────┐     ┌──────────────┐     ┌──────────────────┐

│  IKEA Swing App   │ --> │  SQL Server   │ --> │  ETL Pipeline     │

│  (CRUD + Orders)  │     │  (OLTP)       │     │  Extract/Transform│

└──────────────────┘     └──────────────┘     └────────┬──────────┘

▼

┌──────────────────┐

│  warehouse.db     │

│  (SQLite, Star    │

│  Schema)          │

└────────┬──────────┘

▼

┌───────────────────────┴────────────────┐

▼                                        ▼

┌──────────────────┐                   ┌──────────────────┐

│  ML Model          │                   │  Streamlit         │

│  (XGBoost Forecast)│ ───────────────> │  Dashboard          │

└──────────────────┘                   └──────────────────┘

| Folder | File | Purpose |
|---|---|---|
| `data_generation/` | `generate_sales_data.py` | Generates 18 months of realistic historical sales data and inserts it into SQL Server |
| `etl/` | `etl_pipeline.py` | Extract from SQL Server → Transform into a Star Schema → Load into `warehouse.db` |
| `ml/` | `train_forecast_model.py` | Feature engineering + trains an XGBoost model to forecast daily sales per product |
| `dashboard/` | `app.py` | Streamlit dashboard showing actual sales + forecast |

### Why synthetic data?

The original data in the database is very limited (about 5 products, very
few orders) and not enough to train a real time series model. The
`generate_sales_data.py` script generates data with a realistic time
pattern (monthly seasonality + weekly pattern + growth trend + random
noise) using a fixed `seed` for reproducible results.

**Important note if asked in an interview:** the historical data is
synthetic because the original demo data is too limited for training. The
pipeline and schema themselves are designed to work on real data in
exactly the same way, if it becomes available.

### How to run (in order)

All scripts connect to SQL Server using the same method as
`DatabaseConnection.java` (Windows Integrated Security), so they run
directly on your machine without any changes.

```bash
pip install -r requirements.txt

# 1) Generate realistic historical data into SQL Server
cd data_generation
python generate_sales_data.py

# 2) ETL: from SQL Server -> local Data Warehouse (SQLite)
cd ../etl
python etl_pipeline.py

# 3) Train the forecasting model
cd ../ml
python train_forecast_model.py

# 4) Run the dashboard
cd ../dashboard
streamlit run app.py
```

### Technical details

- **Star Schema**: `fact_sales` (sales aggregated daily per product) + `dim_product` + `dim_date`
- **Features used in the model**: day of week, month, weekend flag, lag-1, lag-7, 7-day rolling mean
- **Model evaluation**: time-based split — last 15% of the data is the test set, not a random split, since this is a time series problem
- **Metrics**: MAE and RMSE
- **Forecasting**: recursive forecasting for 14 days ahead, using predicted values as inputs for subsequent days

### Live demo

🔗 https://saadawy-ai-ikea-shopping-dashboardapp-kpdswe.streamlit.app/

The hosted version of the dashboard runs on a fixed data snapshot
(`warehouse.db` + `forecast_model.joblib` included in this repo), since
the SQL Server instance is local and not reachable from the internet.

## Suggested future extensions

- Replace `warehouse.db` (SQLite) with PostgreSQL or Azure Synapse if the project grows
- Use Airflow to schedule the ETL periodically instead of running it manually
- Add anomaly detection on inventory movement (stock_qty) to catch unusual changes
- Add an "Open Analytics" button inside the Swing app itself (`ProcessBuilder`) to launch the dashboard
