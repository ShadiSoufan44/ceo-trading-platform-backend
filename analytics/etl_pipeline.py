from __future__ import annotations

import argparse
import os
import random
from dataclasses import dataclass
from datetime import UTC, datetime, timedelta
from pathlib import Path
from typing import Any

import matplotlib.pyplot as plt
import pandas as pd
from sqlalchemy import URL, create_engine, text


@dataclass
class InsightBundle:
    volume_by_day: pd.DataFrame
    top_instruments: pd.DataFrame
    client_activity_trends: pd.DataFrame


def extract_fulfilled_trades_from_db(db_url: str | URL) -> pd.DataFrame:
    query = text(
        '''
        SELECT
            o.order_id,
            fulfilled.change_date AS executed_at,
            o.client_id,
            ua.client_segment,
            o.add_instrument_id AS instrument_id,
            add_instrument.symbol,
            o.subtract_instrument_id AS subtract_instrument_id,
            o.side,
            o.quantity,
            COALESCE(o.quote, 0) AS price
        FROM public."order" o
        INNER JOIN (
            SELECT DISTINCT ON (order_id)
                order_id,
                change_date
            FROM public.order_status_change
            WHERE status = 'FULFILLED'
            ORDER BY order_id, change_date DESC
        ) fulfilled ON fulfilled.order_id = o.order_id
        INNER JOIN public.instrument add_instrument
            ON add_instrument.instrument_id = o.add_instrument_id
        LEFT JOIN public.instrument subtract_instrument
            ON subtract_instrument.instrument_id = o.subtract_instrument_id
        INNER JOIN public.user_account ua ON ua.user_id = o.client_id
        WHERE o.side <> 'DEPOSIT'
        '''
    )

    engine = create_engine(
        db_url,
        pool_size=1,
        max_overflow=0,
        pool_pre_ping=True,
        connect_args={
            "options": "-c statement_timeout=30000 -c default_transaction_read_only=on"
        },
    )
    try:
        with engine.connect() as connection:
            return pd.read_sql_query(query, connection)
    finally:
        engine.dispose()


def generate_realistic_test_data(rows: int = 5000) -> pd.DataFrame:
    if rows <= 0:
        raise ValueError("rows must be greater than zero")

    symbols = ["AAPL", "MSFT", "NVDA", "TSLA", "AMD", "GOOG"]
    segments = ["RETAIL", "HIGH_NET_WORTH", "INSTITUTIONAL"]
    sides = ["BUY", "SELL"]
    base_dt = datetime.now(tz=UTC) - timedelta(days=180)

    records: list[dict[str, Any]] = []
    for idx in range(rows):
        executed_at = base_dt + timedelta(minutes=random.randint(0, 180 * 24 * 60))
        quantity = round(random.uniform(1, 1500), 4)
        price = round(random.uniform(10, 500), 4)
        records.append(
            {
                "order_id": f"SIM-{idx:06d}",
                "executed_at": executed_at,
                "client_id": f"CLIENT-{random.randint(1, 400):04d}",
                "client_segment": random.choice(segments),
                "instrument_id": f"INS-{random.randint(1, 120):04d}",
                "symbol": random.choice(symbols),
                "side": random.choice(sides),
                "quantity": quantity,
                "price": price,
            }
        )

    return pd.DataFrame.from_records(records)


def clean_trade_data(df: pd.DataFrame) -> pd.DataFrame:
    required_columns = {
        "order_id",
        "executed_at",
        "client_id",
        "client_segment",
        "instrument_id",
        "symbol",
        "side",
        "quantity",
        "price",
    }
    missing = required_columns.difference(df.columns)
    if missing:
        raise ValueError(f"Missing required columns: {sorted(missing)}")

    cleaned = df.copy()
    cleaned = cleaned.dropna(subset=["order_id", "executed_at", "client_segment", "symbol", "quantity", "price"])

    cleaned["executed_at"] = pd.to_datetime(cleaned["executed_at"], utc=True, errors="coerce")
    cleaned["quantity"] = pd.to_numeric(cleaned["quantity"], errors="coerce")
    cleaned["price"] = pd.to_numeric(cleaned["price"], errors="coerce")

    cleaned = cleaned.dropna(subset=["executed_at", "quantity", "price"])
    cleaned = cleaned[(cleaned["quantity"] > 0) & (cleaned["price"] > 0)]
    cleaned = cleaned.sort_values("executed_at").drop_duplicates("order_id", keep="last")

    cleaned["symbol"] = cleaned["symbol"].astype(str).str.strip().str.upper()
    cleaned["client_segment"] = cleaned["client_segment"].astype(str).str.strip().str.upper()
    cleaned["side"] = cleaned["side"].astype(str).str.strip().str.upper()
    cleaned = cleaned[cleaned["side"] != "DEPOSIT"]

    cleaned["notional"] = cleaned["quantity"] * cleaned["price"]
    cleaned["executed_day"] = cleaned["executed_at"].dt.floor("D")

    return cleaned.reset_index(drop=True)


def compute_insights(df: pd.DataFrame) -> InsightBundle:
    executed_day_naive_utc = df["executed_day"].dt.tz_convert("UTC").dt.tz_localize(None)

    volume_by_day = (
        df.groupby("executed_day", as_index=False)
        .agg(
            total_notional=("notional", "sum"),
            total_quantity=("quantity", "sum"),
            executed_trades=("order_id", "count"),
        )
        .sort_values("executed_day")
    )

    top_instruments = (
        df.groupby("symbol", as_index=False)
        .agg(
            total_notional=("notional", "sum"),
            total_quantity=("quantity", "sum"),
            executed_trades=("order_id", "count"),
        )
        .sort_values("total_notional", ascending=False)
    )

    client_activity_trends = (
        df.assign(executed_week=executed_day_naive_utc.dt.to_period("W").dt.start_time)
        .groupby(["executed_week", "client_segment"], as_index=False)
        .agg(
            total_notional=("notional", "sum"),
            total_quantity=("quantity", "sum"),
            executed_trades=("order_id", "count"),
        )
        .sort_values(["executed_week", "client_segment"])
    )

    return InsightBundle(
        volume_by_day=volume_by_day,
        top_instruments=top_instruments,
        client_activity_trends=client_activity_trends,
    )


def save_charts(insights: InsightBundle, output_dir: Path) -> None:
    output_dir.mkdir(parents=True, exist_ok=True)

    plt.figure(figsize=(10, 5))
    plt.plot(insights.volume_by_day["executed_day"], insights.volume_by_day["total_notional"])
    plt.title("Trading Volume Over Time")
    plt.xlabel("Day")
    plt.ylabel("Total Notional")
    plt.tight_layout()
    plt.savefig(output_dir / "trading_volume_over_time.png")
    plt.close()

    top_n = insights.top_instruments.head(10)
    plt.figure(figsize=(10, 5))
    plt.bar(top_n["symbol"], top_n["total_notional"])
    plt.title("Top Active Instruments")
    plt.xlabel("Symbol")
    plt.ylabel("Total Notional")
    plt.tight_layout()
    plt.savefig(output_dir / "top_active_instruments.png")
    plt.close()

    pivot = insights.client_activity_trends.pivot_table(
        index="executed_week",
        columns="client_segment",
        values="executed_trades",
        aggfunc="sum",
        fill_value=0,
    )
    plt.figure(figsize=(10, 5))
    for segment in pivot.columns:
        plt.plot(pivot.index, pivot[segment], label=segment)
    plt.title("Client Activity Trends")
    plt.xlabel("Week")
    plt.ylabel("Executed Trades")
    plt.legend()
    plt.tight_layout()
    plt.savefig(output_dir / "client_activity_trends.png")
    plt.close()


def write_cli_report(insights: InsightBundle, output_dir: Path) -> Path:
    output_dir.mkdir(parents=True, exist_ok=True)

    total_notional = insights.volume_by_day["total_notional"].sum()
    total_trades = int(insights.volume_by_day["executed_trades"].sum())
    top_symbol = "N/A"
    if not insights.top_instruments.empty:
        top_symbol = str(insights.top_instruments.iloc[0]["symbol"])

    report_lines = [
        "ANALYST INSIGHT REPORT",
        f"Generated at UTC: {datetime.now(tz=UTC).isoformat()}",
        "",
        "Insight 1: Trading Volume",
        f"- Total notional traded: {total_notional:,.2f}",
        f"- Total executed trades: {total_trades}",
        "",
        "Insight 2: Most Active Instruments",
        f"- Top instrument by notional: {top_symbol}",
        "",
        "Insight 3: Client Activity Trends",
        f"- Segments covered: {', '.join(sorted(insights.client_activity_trends['client_segment'].unique().tolist()))}",
    ]

    report_path = output_dir / "analyst_cli_report.txt"
    report_path.write_text("\n".join(report_lines), encoding="utf-8")
    return report_path


def load_reporting_trade_fact(df: pd.DataFrame, db_url: str | URL) -> None:
    if df.empty:
        print("No fulfilled trades to load into reporting_trade_fact")
        return

    engine = create_engine(
        db_url,
        pool_size=1,
        max_overflow=0,
        pool_pre_ping=True,
        connect_args={"options": "-c statement_timeout=30000"},
    )
    upsert = text(
        '''
        INSERT INTO public.reporting_trade_fact (
            order_id,
            executed_at,
            client_id,
            client_segment,
            instrument_id,
            symbol,
            side,
            quantity,
            price,
            notional
        ) VALUES (
            :order_id,
            :executed_at,
            :client_id,
            :client_segment,
            :instrument_id,
            :symbol,
            :side,
            :quantity,
            :price,
            :notional
        )
        ON CONFLICT (order_id) DO UPDATE SET
            executed_at = EXCLUDED.executed_at,
            client_id = EXCLUDED.client_id,
            client_segment = EXCLUDED.client_segment,
            instrument_id = EXCLUDED.instrument_id,
            symbol = EXCLUDED.symbol,
            side = EXCLUDED.side,
            quantity = EXCLUDED.quantity,
            price = EXCLUDED.price,
            notional = EXCLUDED.notional
        '''
    )

    records = df[
        [
            "order_id",
            "executed_at",
            "client_id",
            "client_segment",
            "instrument_id",
            "symbol",
            "side",
            "quantity",
            "price",
            "notional",
        ]
    ].to_dict(orient="records")

    try:
        with engine.begin() as connection:
            connection.execute(upsert, records)
    finally:
        engine.dispose()


def run_pipeline(
    output_dir: Path,
    db_url: str | URL | None = None,
    input_csv: Path | None = None,
    generated_rows: int | None = None,
    load_reporting: bool = False,
    save_clean_csv: bool = False,
) -> tuple[InsightBundle, Path]:
    if db_url:
        raw_df = extract_fulfilled_trades_from_db(db_url)
    elif input_csv is not None:
        raw_df = pd.read_csv(input_csv)
    elif generated_rows is not None:
        raw_df = generate_realistic_test_data(generated_rows)
    else:
        raise ValueError("One of db_url, input_csv, or generated_rows must be provided")

    cleaned_df = clean_trade_data(raw_df)
    insights = compute_insights(cleaned_df)

    chart_dir = output_dir / "charts"
    save_charts(insights, chart_dir)
    report_path = write_cli_report(insights, output_dir)

    if save_clean_csv:
        cleaned_df.to_csv(output_dir / "cleaned_fulfilled_trades.csv", index=False)

    if load_reporting:
        if not db_url:
            raise ValueError("db_url is required when load_reporting is enabled")
        load_reporting_trade_fact(cleaned_df, db_url)

    return insights, report_path


def _parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Analyst ETL pipeline for BR-16 and BR-17")
    parser.add_argument("--db-url", dest="db_url", help="Database URL for PostgreSQL source data")
    parser.add_argument("--input-csv", type=Path, help="Optional CSV input file")
    parser.add_argument("--generate-rows", type=int, help="Generate realistic sample data instead of DB extract")
    parser.add_argument("--output-dir", type=Path, default=Path("analytics/output"), help="Output directory")
    parser.add_argument("--load-reporting", action="store_true", help="Load cleaned data into reporting_trade_fact")
    parser.add_argument("--save-clean-csv", action="store_true", help="Persist cleaned rows for inspection")
    return parser.parse_args()


def _database_url_from_environment() -> URL | None:
    host = os.getenv("ANALYST_ETL_DB_HOST")
    name = os.getenv("ANALYST_ETL_DB_NAME")
    username = os.getenv("ANALYST_ETL_DB_USERNAME")
    password = os.getenv("ANALYST_ETL_DB_PASSWORD")
    if not all((host, name, username, password)):
        return None

    port = int(os.getenv("ANALYST_ETL_DB_PORT", "5432"))
    return URL.create(
        "postgresql+psycopg",
        username=username,
        password=password,
        host=host,
        port=port,
        database=name,
    )


def main() -> None:
    args = _parse_args()
    db_url = args.db_url or _database_url_from_environment()
    _, report_path = run_pipeline(
        output_dir=args.output_dir,
        db_url=db_url,
        input_csv=args.input_csv,
        generated_rows=args.generate_rows,
        load_reporting=args.load_reporting,
        save_clean_csv=args.save_clean_csv,
    )
    print(f"ETL pipeline completed. Report written to: {report_path}")


if __name__ == "__main__":
    main()
