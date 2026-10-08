#!/usr/bin/env python3
"""
Analytics Report Generator for Priya (Commercial Analyst)

Usage:
    python analytics_report.py --token <JWT_TOKEN> [--output report.png]

This script calls the AnalystController REST API endpoints to fetch:
1. Monthly trading volumes by instrument
2. Client activity summary (trade counts and volumes)

Then generates visualization charts for executive reporting.

Requirements:
    pip install requests pandas matplotlib seaborn
"""

import requests
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
import argparse
import sys
from typing import List, Dict, Any

# Configure plotting style
sns.set_theme(style="whitegrid")
plt.rcParams['figure.figsize'] = (16, 12)

class AnalyticsReportGenerator:
    def __init__(self, api_base_url: str, jwt_token: str):
        self.api_base_url = api_base_url.rstrip('/')
        self.headers = {
            "Authorization": f"Bearer {jwt_token}",
            "Content-Type": "application/json"
        }
    
    def fetch_monthly_volumes(self) -> List[Dict[str, Any]]:
        """Fetch monthly trading volumes by instrument from API."""
        url = f"{self.api_base_url}/api/analytics/monthly-volumes"
        try:
            response = requests.get(url, headers=self.headers)
            response.raise_for_status()
            return response.json()
        except requests.exceptions.RequestException as e:
            print(f"❌ Error fetching monthly volumes: {e}", file=sys.stderr)
            return []
    
    def fetch_client_activity(self) -> List[Dict[str, Any]]:
        """Fetch client activity report from API."""
        url = f"{self.api_base_url}/api/analytics/client-activity"
        try:
            response = requests.get(url, headers=self.headers)
            response.raise_for_status()
            return response.json()
        except requests.exceptions.RequestException as e:
            print(f"❌ Error fetching client activity: {e}", file=sys.stderr)
            return []
    
    def generate_report(self, output_file: str = "trading_analytics_report.png"):
        """Generate multi-panel visualization report."""
        print("📊 Fetching analytics data from API...")
        
        volumes_data = self.fetch_monthly_volumes()
        activity_data = self.fetch_client_activity()
        
        if not volumes_data or not activity_data:
            print("❌ No data retrieved from API. Check authentication and API connectivity.", file=sys.stderr)
            return False
        
        volumes_df = pd.DataFrame(volumes_data)
        activity_df = pd.DataFrame(activity_data)
        
        print("📈 Generating visualization panels...")
        fig, axes = plt.subplots(2, 2, figsize=(16, 12))
        fig.suptitle("Trading Platform Analytics Report - Executive Dashboard", 
                     fontsize=16, fontweight='bold', y=0.995)
        
        # Panel 1: Monthly Volume Trend
        print("  → Monthly volume trend...")
        if 'month_year' in volumes_df.columns and 'total_volume' in volumes_df.columns:
            monthly_volume = volumes_df.groupby('month_year')['total_volume'].sum().sort_index()
            axes[0, 0].plot(monthly_volume.index, monthly_volume.values, 
                           marker='o', linewidth=2, markersize=6, color='#1f77b4')
            axes[0, 0].set_title("Monthly Trading Volume Trend", fontweight='bold')
            axes[0, 0].set_xlabel("Month")
            axes[0, 0].set_ylabel("Total Volume (Units)")
            axes[0, 0].tick_params(axis='x', rotation=45)
            axes[0, 0].grid(True, alpha=0.3)
        
        # Panel 2: Top Instruments
        print("  → Top instruments by volume...")
        if 'symbol' in volumes_df.columns and 'total_volume' in volumes_df.columns:
            top_instruments = volumes_df.groupby('symbol')['total_volume'].sum().sort_values(ascending=True).tail(10)
            axes[0, 1].barh(top_instruments.index, top_instruments.values, color='#ff7f0e')
            axes[0, 1].set_title("Top 10 Instruments by Volume", fontweight='bold')
            axes[0, 1].set_xlabel("Total Volume")
            axes[0, 1].grid(True, alpha=0.3, axis='x')
        
        # Panel 3: Top Clients by Trade Count
        print("  → Top clients by activity...")
        if 'full_name' in activity_df.columns and 'total_trades' in activity_df.columns:
            top_traders = activity_df.groupby('full_name')['total_trades'].sum().sort_values(ascending=True).tail(10)
            axes[1, 0].barh(top_traders.index, top_traders.values, color='#2ca02c')
            axes[1, 0].set_title("Top 10 Clients by Trade Count", fontweight='bold')
            axes[1, 0].set_xlabel("Number of Trades")
            axes[1, 0].grid(True, alpha=0.3, axis='x')
        
        # Panel 4: Top Clients by Volume Value
        print("  → Top clients by volume value...")
        if 'full_name' in activity_df.columns and 'total_volume_value' in activity_df.columns:
            top_revenue = activity_df.groupby('full_name')['total_volume_value'].sum().sort_values(ascending=True).tail(10)
            axes[1, 1].barh(top_revenue.index, top_revenue.values, color='#d62728')
            axes[1, 1].set_title("Top 10 Clients by Volume Value ($)", fontweight='bold')
            axes[1, 1].set_xlabel("Total Amount ($)")
            axes[1, 1].grid(True, alpha=0.3, axis='x')
        
        plt.tight_layout()
        plt.savefig(output_file, dpi=300, bbox_inches='tight')
        print(f"✅ Report saved: {output_file}")
        return True

def main():
    parser = argparse.ArgumentParser(description="Generate trading analytics report for Priya")
    parser.add_argument("--api-url", default="http://localhost:8080", 
                       help="API base URL (default: http://localhost:8080)")
    parser.add_argument("--token", required=True, help="JWT authentication token")
    parser.add_argument("--output", default="trading_analytics_report.png", 
                       help="Output file path (default: trading_analytics_report.png)")
    
    args = parser.parse_args()
    
    generator = AnalyticsReportGenerator(args.api_url, args.token)
    success = generator.generate_report(args.output)
    
    return 0 if success else 1

if __name__ == "__main__":
    sys.exit(main())
