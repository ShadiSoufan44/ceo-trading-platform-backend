import unittest

import pandas as pd

from analytics.etl_pipeline import clean_trade_data, compute_insights


class AnalystEtlPipelineTest(unittest.TestCase):
    def test_excludes_deposits_but_keeps_equity_and_forex_trades(self) -> None:
        trades = pd.DataFrame(
            [
                {
                    "order_id": "deposit-usd",
                    "executed_at": "2026-10-01T12:00:00Z",
                    "client_id": "client-1",
                    "client_segment": "RETAIL",
                    "instrument_id": "usd",
                    "symbol": "USD",
                    "side": "DEPOSIT",
                    "quantity": 100000,
                    "price": 1,
                },
                {
                    "order_id": "buy-aapl",
                    "executed_at": "2026-10-02T12:00:00Z",
                    "client_id": "client-1",
                    "client_segment": "RETAIL",
                    "instrument_id": "aapl",
                    "symbol": "AAPL",
                    "side": "BUY",
                    "quantity": 5,
                    "price": 200,
                },
                {
                    "order_id": "exchange-usd-inr",
                    "executed_at": "2026-10-03T12:00:00Z",
                    "client_id": "client-1",
                    "client_segment": "RETAIL",
                    "instrument_id": "inr",
                    "symbol": "INR",
                    "side": "BUY",
                    "quantity": 50000,
                    "price": 0.012,
                },
            ]
        )

        cleaned = clean_trade_data(trades)
        insights = compute_insights(cleaned)

        self.assertEqual(set(cleaned["order_id"]), {"buy-aapl", "exchange-usd-inr"})
        self.assertEqual(insights.volume_by_day["total_quantity"].sum(), 50005)
        self.assertEqual(insights.volume_by_day["executed_trades"].sum(), 2)


if __name__ == "__main__":
    unittest.main()