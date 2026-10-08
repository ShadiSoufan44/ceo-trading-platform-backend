#!/usr/bin/env python3
"""
ETL Pipeline for Trading Analytics (Sprint 4 Deliverable)

Extracts, cleans, and transforms trading data from the analytics API.
Generates business insights and produces CSV reports.

Usage:
    python etl_pipeline.py --token <JWT_TOKEN> [--output-dir ./reports]

Requirements:
    pandas, requests, matplotlib, seaborn
"""

import argparse
import sys
import json
from datetime import datetime
from pathlib import Path

try:
    import pandas as pd
    import requests
    import matplotlib.pyplot as plt
    import seaborn as sns
except ImportError as e:
    print(f"Error: Missing required library: {e}")
    print("Install with: pip install pandas requests matplotlib seaborn")
    sys.exit(1)


class AnalyticsETL:
    """Extract, Transform, Load trading analytics data."""
    
    def __init__(self, api_base_url: str = "http://localhost:8081/api"):
        self.api_base_url = api_base_url
        self.volumes_data = None
        self.activity_data = None
        self.session = requests.Session()
    
    def set_auth_token(self, token: str):
        """Set JWT bearer token for API authentication."""
        self.session.headers.update({
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json"
        })
    
    def extract_volumes(self) -> bool:
        """Extract monthly trading volumes from API."""
        try:
            url = f"{self.api_base_url}/analytics/monthly-volumes"
            response = self.session.get(url, timeout=10)
            response.raise_for_status()
            
            data = response.json()
            self.volumes_data = pd.DataFrame(data)
            print(f"✓ Extracted {len(self.volumes_data)} volume records")
            return True
        except requests.RequestException as e:
            print(f"✗ Failed to extract volumes: {e}")
            return False
    
    def extract_activity(self) -> bool:
        """Extract monthly client activity from API."""
        try:
            url = f"{self.api_base_url}/analytics/client-activity"
            response = self.session.get(url, timeout=10)
            response.raise_for_status()
            
            data = response.json()
            self.activity_data = pd.DataFrame(data)
            print(f"✓ Extracted {len(self.activity_data)} activity records")
            return True
        except requests.RequestException as e:
            print(f"✗ Failed to extract activity: {e}")
            return False
    
    def transform_volumes(self) -> pd.DataFrame:
        """Clean and transform volume data."""
        if self.volumes_data is None or self.volumes_data.empty:
            print("⚠ No volume data to transform")
            return pd.DataFrame()
        
        df = self.volumes_data.copy()
        
        # Convert month_year to datetime
        df['month_year'] = pd.to_datetime(df['month_year'])
        
        # Ensure numeric columns
        df['total_orders'] = pd.to_numeric(df['total_orders'], errors='coerce')
        df['total_volume'] = pd.to_numeric(df['total_volume'], errors='coerce')
        
        # Remove rows with null values
        df = df.dropna()
        
        # Sort by month (desc) and symbol (asc)
        df = df.sort_values(['month_year', 'symbol'], ascending=[False, True])
        
        print(f"✓ Transformed {len(df)} volume records")
        return df
    
    def transform_activity(self) -> pd.DataFrame:
        """Clean and transform activity data."""
        if self.activity_data is None or self.activity_data.empty:
            print("⚠ No activity data to transform")
            return pd.DataFrame()
        
        df = self.activity_data.copy()
        
        # Convert month_year to datetime
        df['month_year'] = pd.to_datetime(df['month_year'])
        
        # Ensure numeric columns
        df['user_id'] = pd.to_numeric(df['user_id'], errors='coerce')
        df['total_trades'] = pd.to_numeric(df['total_trades'], errors='coerce')
        df['total_volume_value'] = pd.to_numeric(df['total_volume_value'], errors='coerce')
        
        # Remove rows with null values
        df = df.dropna()
        
        # Sort by month (desc) and volume (desc)
        df = df.sort_values(['month_year', 'total_volume_value'], ascending=[False, False])
        
        print(f"✓ Transformed {len(df)} activity records")
        return df
    
    def generate_insight_1_top_instruments(self, volumes_df: pd.DataFrame) -> dict:
        """Insight 1: Top 5 instruments by order count."""
        if volumes_df.empty:
            return {}
        
        top_instruments = volumes_df.groupby('symbol').agg({
            'total_orders': 'sum',
            'total_volume': 'sum'
        }).sort_values('total_orders', ascending=False).head(5)
        
        insight = {
            "title": "Top 5 Most Actively Traded Instruments",
            "metric": "Total Orders",
            "data": top_instruments.to_dict('index'),
            "generated_at": datetime.now().isoformat()
        }
        
        print(f"✓ Generated Insight 1: {len(insight['data'])} top instruments")
        return insight
    
    def generate_insight_2_volume_trends(self, volumes_df: pd.DataFrame) -> dict:
        """Insight 2: Monthly volume trends."""
        if volumes_df.empty:
            return {}
        
        monthly_totals = volumes_df.groupby('month_year').agg({
            'total_volume': 'sum',
            'total_orders': 'sum'
        }).sort_index(ascending=False)
        
        insight = {
            "title": "Monthly Trading Volume Trend",
            "metric": "Total Volume",
            "data": monthly_totals.to_dict('index'),
            "generated_at": datetime.now().isoformat()
        }
        
        print(f"✓ Generated Insight 2: {len(insight['data'])} months of trends")
        return insight
    
    def generate_insight_3_top_clients(self, activity_df: pd.DataFrame) -> dict:
        """Insight 3: Top 10 clients by trading value."""
        if activity_df.empty:
            return {}
        
        top_clients = activity_df.groupby(['user_id', 'full_name']).agg({
            'total_trades': 'sum',
            'total_volume_value': 'sum'
        }).sort_values('total_volume_value', ascending=False).head(10)
        
        insight = {
            "title": "Top 10 Clients by Trading Value",
            "metric": "Total Trading Value ($)",
            "data": top_clients.to_dict('index'),
            "generated_at": datetime.now().isoformat()
        }
        
        print(f"✓ Generated Insight 3: {len(insight['data'])} top clients")
        return insight
    
    def save_reports(self, output_dir: str, volumes_df: pd.DataFrame, activity_df: pd.DataFrame):
        """Save CSV reports."""
        Path(output_dir).mkdir(parents=True, exist_ok=True)
        
        volumes_file = f"{output_dir}/volumes_report.csv"
        activity_file = f"{output_dir}/activity_report.csv"
        
        if not volumes_df.empty:
            volumes_df.to_csv(volumes_file, index=False)
            print(f"✓ Saved volumes report: {volumes_file}")
        
        if not activity_df.empty:
            activity_df.to_csv(activity_file, index=False)
            print(f"✓ Saved activity report: {activity_file}")
    
    def save_insights(self, output_dir: str, insights: list):
        """Save insights as JSON."""
        Path(output_dir).mkdir(parents=True, exist_ok=True)
        
        insights_file = f"{output_dir}/business_insights.json"
        with open(insights_file, 'w') as f:
            json.dump(insights, f, indent=2, default=str)
        
        print(f"✓ Saved business insights: {insights_file}")


def main():
    parser = argparse.ArgumentParser(
        description="ETL Pipeline for Trading Analytics",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  python etl_pipeline.py --token eyJhbGc...
  python etl_pipeline.py --token $JWT_TOKEN --output-dir ./my_reports
  python etl_pipeline.py --token $JWT_TOKEN --api-url http://api.example.com:8081/api
        """
    )
    
    parser.add_argument("--token", required=True, help="JWT bearer token for API authentication")
    parser.add_argument("--output-dir", default="./analytics_reports", help="Output directory for reports (default: ./analytics_reports)")
    parser.add_argument("--api-url", default="http://localhost:8081/api", help="API base URL (default: http://localhost:8081/api)")
    
    args = parser.parse_args()
    
    print("=" * 60)
    print("Trading Analytics ETL Pipeline (Sprint 4)")
    print("=" * 60)
    print(f"Timestamp: {datetime.now().isoformat()}")
    print()
    
    # Initialize ETL
    etl = AnalyticsETL(api_base_url=args.api_url)
    etl.set_auth_token(args.token)
    
    # Extract
    print("[EXTRACT]")
    if not etl.extract_volumes() or not etl.extract_activity():
        print("\n✗ Extraction failed. Check API connection and token.")
        sys.exit(1)
    print()
    
    # Transform
    print("[TRANSFORM]")
    volumes_df = etl.transform_volumes()
    activity_df = etl.transform_activity()
    
    if volumes_df.empty and activity_df.empty:
        print("✗ No data to transform. Exiting.")
        sys.exit(1)
    print()
    
    # Generate Insights
    print("[INSIGHTS]")
    insights = [
        etl.generate_insight_1_top_instruments(volumes_df),
        etl.generate_insight_2_volume_trends(volumes_df),
        etl.generate_insight_3_top_clients(activity_df)
    ]
    print()
    
    # Load/Save
    print("[LOAD]")
    etl.save_reports(args.output_dir, volumes_df, activity_df)
    etl.save_insights(args.output_dir, insights)
    print()
    
    print("=" * 60)
    print("✓ ETL Pipeline completed successfully")
    print(f"Reports saved to: {args.output_dir}")
    print("=" * 60)


if __name__ == "__main__":
    main()
