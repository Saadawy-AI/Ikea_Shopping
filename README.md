# 🛒 IKEA Shopping — Admin Panel + Data & ML Extension

<p align="center">
  <img src="https://img.shields.io/badge/Java-Swing-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white"/>
  <img src="https://img.shields.io/badge/SQL_Server-CC2927?style=for-the-badge&logo=microsoftsqlserver&logoColor=white"/>
  <img src="https://img.shields.io/badge/Python-3.10+-3776AB?style=for-the-badge&logo=python&logoColor=white"/>
  <img src="https://img.shields.io/badge/XGBoost-AA4A44?style=for-the-badge&logo=python&logoColor=white"/>
  <img src="https://img.shields.io/badge/Streamlit-FF4B4B?style=for-the-badge&logo=streamlit&logoColor=white"/>
</p>

---

## 📌 Overview

A two-layer project: an operational **Java Swing admin panel** backed by SQL Server, extended with a **Data Engineering + Machine Learning** layer that turns the operational sales data into a forecasting dashboard.

**🚀 Live demo:** https://saadawy-ai-ikea-shopping-dashboardapp-kpdswe.streamlit.app/

---

## 🏗 Project Structure

```
ikea-shopping-data-ml-extension/
├── java-app/              # Original admin panel (Java Swing + SQL Server)
├── data_generation/       # Generates realistic historical sales data
├── etl/                   # ETL pipeline: SQL Server -> Data Warehouse (SQLite)
├── ml/                    # XGBoost training for sales forecasting
├── dashboard/             # Streamlit dashboard (actual sales + forecast)
├── Images/                # Screenshots used in this README
└── requirements.txt       # Python dependencies for the Data/ML layer
```

---

## 🗄 Database Design

The database (`Ikea_Shopping`) has 10 tables: Users, Products, Categories, Orders, Order_Items, Cart, Cart_Items, Addresses, Payments, and Reviews, all defined in `java-app/script.sql`.

**ER Diagram:**

![Database ER Diagram](Images/01-database-er-diagram.png)

**Schema script (SQL Server Management Studio):**

![Database creation script](Images/02-database-script.png)

---

## ⚙️ Technologies Used

| Category           | Tools                                      |
|--------------------|--------------------------------------------|
| Admin Panel        | Java Swing, MS SQL Server                  |
| Security           | SHA-256 password hashing                   |
| Data Generation    | Python, realistic time-series simulation   |
| ETL                | Python, SQLite (Star Schema)               |
| ML Model           | XGBoost, feature engineering               |
| Dashboard          | Streamlit                                  |

---

## 🖥 1) java-app/ — Admin Panel (OLTP)

A desktop CRUD application built with **Java Swing**, backed by **MS SQL Server**.

- Login system with **SHA-256** password hashing (`PasswordUtils.java`)
- Role-based access control (Admin / User)
- CRUD panels: Products, Orders, Users (`ProductsPanel.java`, `OrdersPanel.java`, `UsersPanel.java`)
- Dashboard panel showing live KPIs (`DashboardPanel.java`)

### 🚀 How to Run

### 1. Set up the database
```bash
# Run java-app/script.sql on your SQL Server instance to create the database and schema.
```

### 2. Configure the connection
Update the connection details in `DatabaseConnection.java` if needed (default uses Windows Integrated Security on a local server).

### 3. Launch the application
```bash
java -jar ikea_shopping.jar
```

Or run `run.bat`, or open the project in NetBeans and run it directly. The prebuilt jar is in `java-app/dist/` or `java-app/`.

### 🎬 Screenshots

**Login screen:**

![Java app login screen](Images/03-java-app-login.png)

**Dashboard (live KPIs):**

![Java app dashboard](Images/04-java-app-dashboard.png)

**Products CRUD:**

![Java app products CRUD](Images/05-java-app-products-crud.png)

**Orders CRUD:**

![Java app orders CRUD](Images/06-java-app-orders-crud.png)

---

## 🔄 2) Data & ML Extension (data_generation / etl / ml / dashboard)

This layer was added on top of the same SQL Server database, without modifying any Java code. It turns the operational data into an analytics-ready data product:

```
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
```

| Folder | File | Purpose |
|---|---|---|
| `data_generation/` | `generate_sales_data.py` | Generates 18 months of realistic historical sales data and inserts it into SQL Server |
| `etl/` | `etl_pipeline.py` | Extract from SQL Server → Transform into a Star Schema → Load into `warehouse.db` |
| `ml/` | `train_forecast_model.py` | Feature engineering + trains an XGBoost model to forecast daily sales per product |
| `dashboard/` | `app.py` | Streamlit dashboard showing actual sales + forecast |

---

### ❓ Why Synthetic Data?

The original data in the database is very limited (about 5 products, very few orders) and not enough to train a real time series model. The `generate_sales_data.py` script generates data with a realistic time pattern (monthly seasonality + weekly pattern + growth trend + random noise) using a fixed `seed` for reproducible results.

**Important note if asked in an interview:** the historical data is synthetic because the original demo data is too limited for training. The pipeline and schema themselves are designed to work on real data in exactly the same way, if it becomes available.

---

### 🚀 How to Run (in order)

All scripts connect to SQL Server using the same method as `DatabaseConnection.java` (Windows Integrated Security), so they run directly on your machine without any changes.

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

---

### 🧠 Technical Details

- **Star Schema**: `fact_sales` (sales aggregated daily per product) + `dim_product` + `dim_date`
- **Features used in the model**: day of week, month, weekend flag, lag-1, lag-7, 7-day rolling mean
- **Model evaluation**: time-based split — last 15% of the data is the test set, not a random split, since this is a time series problem
- **Metrics**: MAE and RMSE
- **Forecasting**: recursive forecasting for 14 days ahead, using predicted values as inputs for subsequent days

---

### 🔗 Live Demo

🔗 https://saadawy-ai-ikea-shopping-dashboardapp-kpdswe.streamlit.app/

The hosted version of the dashboard runs on a fixed data snapshot (`warehouse.db` + `forecast_model.joblib` included in this repo), since the SQL Server instance is local and not reachable from the internet.

### 🎬 Screenshots

**Main dashboard — actual sales + forecast:**

![Streamlit dashboard](Images/07-streamlit-dashboard.png)

**Product selector:**

![Streamlit product selector](Images/08-streamlit-product-selector.png)

**Top 10 products table:**

![Streamlit top products table](Images/09-streamlit-top-products.png)

---

## 🚀 Future Extensions

- Replace `warehouse.db` (SQLite) with PostgreSQL or Azure Synapse if the project grows
- Use Airflow to schedule the ETL periodically instead of running it manually
- Add anomaly detection on inventory movement (stock_qty) to catch unusual changes
- Add an "Open Analytics" button inside the Swing app itself (`ProcessBuilder`) to launch the dashboard

---

## 👤 Author

**Mohamed Saadawy**  
📎 [GitHub](https://github.com/Saadawy-AI) · [LinkedIn](https://linkedin.com/in/muhammad-saadawy) · [Portfolio](https://saadawy-ai.github.io/My-Portfolio/)

---

> *This project combines a full CRUD desktop application with a modern Data Engineering and ML pipeline, demonstrating end-to-end ownership from operational systems to analytics and forecasting.*
