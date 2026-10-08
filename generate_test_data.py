#!/usr/bin/env python3
"""
Generate Realistic Test Data for Analytics Testing (Sprint 4)

Creates sample trading data with realistic patterns:
- Multiple months of order history
- Diverse instruments (AAPL, GOOGL, MSFT, AMZN, TSLA)
- Multiple clients with varying trading patterns
- FULFILLED orders with realistic volumes and prices

This script generates JSON output that mimics the analytics API responses,
useful for testing without a live database.

Usage:
    python generate_test_data.py [--months 12] [--output ./test_data.json]
"""

import argparse
import json
from datetime import datetime, timedelta
from random import randint, choice, uniform
from pathlib import Path


def generate_instruments():
    """Generate sample instruments."""
    return {
        "AAPL": {"name": "Apple Inc.", "type": "EQUITY"},
        "GOOGL": {"name": "Alphabet Inc.", "type": "EQUITY"},
        "MSFT": {"name": "Microsoft Corporation", "type": "EQUITY"},
        "AMZN": {"name": "Amazon.com Inc.", "type": "EQUITY"},
        "TSLA": {"name": "Tesla Inc.", "type": "EQUITY"},
    }


def generate_clients():
    """Generate sample clients."""
    return [
        {"user_id": 1, "full_name": "Alice Johnson"},
        {"user_id": 2, "full_name": "Bob Smith"},
        {"user_id": 3, "full_name": "Carol Davis"},
        {"user_id": 4, "full_name": "David Wilson"},
        {"user_id": 5, "full_name": "Emma Martinez"},
        {"user_id": 6, "full_name": "Frank Thompson"},
        {"user_id": 7, "full_name": "Grace Lee"},
        {"user_id": 8, "full_name": "Henry Anderson"},
    ]


def generate_volume_data(months: int = 12) -> list:
    """
    Generate monthly volume report data.
    
    Mimics: /api/analytics/monthly-volumes response
    Returns list of dicts with: month_year, total_orders, total_volume, symbol
    """
    instruments = generate_instruments()
    data = []
    
    # Generate data for N months back
    for month_offset in range(months):
        # Calculate month_year
        month_date = datetime.now() - timedelta(days=30 * month_offset)
        month_year = month_date.strftime("%Y-%m-01")
        
        # Generate data for each instrument
        for symbol in instruments.keys():
            # Realistic trading patterns
            base_volume = randint(80, 300)
            volume_variance = uniform(0.8, 1.2)
            
            record = {
                "month_year": month_year,
                "symbol": symbol,
                "total_orders": int(base_volume * volume_variance),
                "total_volume": int(base_volume * 2.5 * volume_variance),
            }
            data.append(record)
    
    # Sort by month_year DESC, symbol ASC
    data.sort(key=lambda x: (x["month_year"], x["symbol"]), reverse=True)
    return data


def generate_activity_data(months: int = 12) -> list:
    """
    Generate monthly activity report data.
    
    Mimics: /api/analytics/client-activity response
    Returns list of dicts with: user_id, full_name, month_year, total_trades, total_volume_value
    """
    clients = generate_clients()
    data = []
    
    # Generate data for N months back
    for month_offset in range(months):
        # Calculate month_year
        month_date = datetime.now() - timedelta(days=30 * month_offset)
        month_year = month_date.strftime("%Y-%m-01")
        
        # Generate data for each client
        for client in clients:
            # Realistic trading patterns (some clients more active)
            activity_multiplier = uniform(0.5, 2.0)
            base_trades = randint(3, 15)
            trades = int(base_trades * activity_multiplier)
            
            # Average trade value $500 - $10,000
            avg_value_per_trade = uniform(500, 10000)
            total_value = round(trades * avg_value_per_trade, 2)
            
            record = {
                "user_id": client["user_id"],
                "full_name": client["full_name"],
                "month_year": month_year,
                "total_trades": trades,
                "total_volume_value": total_value,
            }
            data.append(record)
    
    # Sort by month_year DESC, total_volume_value DESC
    data.sort(key=lambda x: (x["month_year"], x["total_volume_value"]), reverse=True)
    return data


def generate_test_suite(months: int = 12) -> dict:
    """Generate complete test data suite."""
    return {
        "generated_at": datetime.now().isoformat(),
        "months": months,
        "instruments": generate_instruments(),
        "clients": generate_clients(),
        "volume_reports": generate_volume_data(months),
        "activity_reports": generate_activity_data(months),
    }


def save_test_data(data: dict, output_file: str):
    """Save test data to JSON file."""
    Path(output_file).parent.mkdir(parents=True, exist_ok=True)
    
    with open(output_file, 'w') as f:
        json.dump(data, f, indent=2, default=str)
    
    print(f"✓ Test data saved: {output_file}")
    print(f"  - Instruments: {len(data['instruments'])}")
    print(f"  - Clients: {len(data['clients'])}")
    print(f"  - Volume records: {len(data['volume_reports'])}")
    print(f"  - Activity records: {len(data['activity_reports'])}")


def print_sample_insights(data: dict):
    """Print sample insights from generated data."""
    print("\n" + "=" * 60)
    print("SAMPLE INSIGHTS FROM TEST DATA")
    print("=" * 60)
    
    # Insight 1: Top instruments
    volumes = data['volume_reports']
    instrument_totals = {}
    for record in volumes:
        symbol = record['symbol']
        if symbol not in instrument_totals:
            instrument_totals[symbol] = {"orders": 0, "volume": 0}
        instrument_totals[symbol]["orders"] += record["total_orders"]
        instrument_totals[symbol]["volume"] += record["total_volume"]
    
    top_5 = sorted(instrument_totals.items(), key=lambda x: x[1]["orders"], reverse=True)[:5]
    print("\nInsight 1: Top 5 Most Actively Traded Instruments")
    for rank, (symbol, metrics) in enumerate(top_5, 1):
        print(f"  {rank}. {symbol}: {metrics['orders']} orders, {metrics['volume']} volume")
    
    # Insight 2: Top clients
    activity = data['activity_reports']
    client_totals = {}
    for record in activity:
        key = f"{record['full_name']} (ID: {record['user_id']})"
        if key not in client_totals:
            client_totals[key] = {"trades": 0, "value": 0}
        client_totals[key]["trades"] += record["total_trades"]
        client_totals[key]["value"] += record["total_volume_value"]
    
    top_10 = sorted(client_totals.items(), key=lambda x: x[1]["value"], reverse=True)[:10]
    print("\nInsight 3: Top 10 Clients by Trading Value")
    for rank, (client, metrics) in enumerate(top_10, 1):
        print(f"  {rank}. {client}: {metrics['trades']} trades, ${metrics['value']:,.2f}")
    
    # Insight 2: Monthly trends
    month_totals = {}
    for record in volumes:
        month = record['month_year']
        if month not in month_totals:
            month_totals[month] = {"orders": 0, "volume": 0}
        month_totals[month]["orders"] += record["total_orders"]
        month_totals[month]["volume"] += record["total_volume"]
    
    print("\nInsight 2: Monthly Volume Trend")
    for month in sorted(month_totals.keys(), reverse=True)[:3]:
        metrics = month_totals[month]
        print(f"  {month}: {metrics['orders']} orders, {metrics['volume']} total volume")
    
    print("\n" + "=" * 60)


def main():
    parser = argparse.ArgumentParser(
        description="Generate realistic test data for analytics pipeline",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  python generate_test_data.py
  python generate_test_data.py --months 24
  python generate_test_data.py --output ./my_test_data.json
        """
    )
    
    parser.add_argument("--months", type=int, default=12, help="Number of months of test data (default: 12)")
    parser.add_argument("--output", default="test_data.json", help="Output file path (default: test_data.json)")
    
    args = parser.parse_args()
    
    print("=" * 60)
    print("Generating Realistic Test Data for Analytics (Sprint 4)")
    print("=" * 60)
    print(f"Months: {args.months}")
    print(f"Output: {args.output}")
    print()
    
    # Generate test data
    test_data = generate_test_suite(args.months)
    
    # Save to file
    save_test_data(test_data, args.output)
    
    # Print sample insights
    print_sample_insights(test_data)
    
    print(f"\n✓ Test data generation complete")
    print(f"Use with: etl_pipeline.py or mock_analytics_test.py")


if __name__ == "__main__":
    main()
