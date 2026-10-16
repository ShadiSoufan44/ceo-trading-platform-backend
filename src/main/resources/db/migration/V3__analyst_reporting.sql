ALTER TABLE public.user_account
ADD COLUMN IF NOT EXISTS client_segment text NOT NULL DEFAULT 'RETAIL';

ALTER TABLE public.user_account
DROP CONSTRAINT IF EXISTS user_account_client_segment_check;

ALTER TABLE public.user_account
ADD CONSTRAINT user_account_client_segment_check
CHECK (client_segment IN ('RETAIL', 'HIGH_NET_WORTH', 'INSTITUTIONAL'));

CREATE TABLE IF NOT EXISTS public.reporting_trade_fact (
  trade_fact_id uuid NOT NULL DEFAULT gen_random_uuid(),
  order_id uuid NOT NULL,
  executed_at timestamp with time zone NOT NULL,
  client_id uuid NOT NULL,
  client_segment text NOT NULL,
  instrument_id uuid NOT NULL,
  symbol text NOT NULL,
  side text NOT NULL,
  quantity numeric NOT NULL,
  price numeric NOT NULL,
  notional numeric NOT NULL,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT reporting_trade_fact_pkey PRIMARY KEY (trade_fact_id),
  CONSTRAINT reporting_trade_fact_order_id_key UNIQUE (order_id),
  CONSTRAINT reporting_trade_fact_client_id_fkey FOREIGN KEY (client_id) REFERENCES public.user_account(user_id),
  CONSTRAINT reporting_trade_fact_instrument_id_fkey FOREIGN KEY (instrument_id) REFERENCES public.instrument(instrument_id)
);

CREATE INDEX IF NOT EXISTS idx_reporting_trade_fact_executed_at
  ON public.reporting_trade_fact(executed_at);

CREATE INDEX IF NOT EXISTS idx_reporting_trade_fact_symbol_executed_at
  ON public.reporting_trade_fact(symbol, executed_at);

CREATE INDEX IF NOT EXISTS idx_reporting_trade_fact_segment_executed_at
  ON public.reporting_trade_fact(client_segment, executed_at);
