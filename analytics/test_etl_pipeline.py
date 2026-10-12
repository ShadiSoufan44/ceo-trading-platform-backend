import tempfile
import unittest
from pathlib import Path

import pandas as pd

from etl_pipeline import clean_trade_data, compute_insights, run_pipeline


class EtlPipelineTests(unittest.TestCase):

    def _sample_df(self) -> pd.DataFrame:
        return pd.DataFrame(
            [
                {
                    "order_id": "A-1",
                    "executed_at": "2026-01-02T10:00:00Z",
                    "client_id": "C1",
                    "client_segment": "retail",
                    "instrument_id": "I1",
                    "symbol": "aapl",
                    "side": "BUY",
                    "quantity": 10,
                    "price": 100,
                },
                {
                    "order_id": "A-2",
                    "executed_at": "2026-01-02T11:00:00Z",
                    "client_id": "C2",
                    "client_segment": "institutional",
                    "instrument_id": "I2",
                    "symbol": "msft",
                    "side": "SELL",
                    "quantity": 5,
                    "price": 200,
                },
                {
                    "order_id": "BAD",
                    "executed_at": "2026-01-03T11:00:00Z",
                    "client_id": "C3",
                    "client_segment": "retail",
                    "instrument_id": "I3",
                    "symbol": "tsla",
                    "side": "BUY",
                    "quantity": -1,
                    "price": 10,
                },
            ]
        )

    def test_clean_trade_data_filters_invalid_rows_and_computes_notional(self) -> None:
        cleaned = clean_trade_data(self._sample_df())
        self.assertEqual(len(cleaned), 2)
        self.assertIn("notional", cleaned.columns)
        self.assertAlmostEqual(float(cleaned["notional"].sum()), 2000.0, places=4)
        self.assertEqual(cleaned.iloc[0]["symbol"], "AAPL")

    def test_compute_insights_returns_three_business_views(self) -> None:
        cleaned = clean_trade_data(self._sample_df())
        insights = compute_insights(cleaned)
        self.assertFalse(insights.volume_by_day.empty)
        self.assertFalse(insights.top_instruments.empty)
        self.assertFalse(insights.client_activity_trends.empty)
        self.assertEqual(str(insights.top_instruments.iloc[0]["symbol"]), "AAPL")

    def test_run_pipeline_writes_report_and_charts(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            output_dir = Path(tmp_dir)
            csv_path = output_dir / "input.csv"
            self._sample_df().to_csv(csv_path, index=False)

            _, report_path = run_pipeline(
                output_dir=output_dir,
                input_csv=csv_path,
                save_clean_csv=True,
            )

            self.assertTrue(report_path.exists())
            self.assertTrue((output_dir / "charts" / "trading_volume_over_time.png").exists())
            self.assertTrue((output_dir / "charts" / "top_active_instruments.png").exists())
            self.assertTrue((output_dir / "charts" / "client_activity_trends.png").exists())
            self.assertTrue((output_dir / "cleaned_fulfilled_trades.csv").exists())


if __name__ == "__main__":
    unittest.main()
