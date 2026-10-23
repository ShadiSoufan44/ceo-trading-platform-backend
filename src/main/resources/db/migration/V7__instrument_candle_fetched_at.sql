-- When the candle was last fetched from the market API. A candle is only final if it was
-- fetched after its day ended; rows from before this column existed (NULL) are refetched once.
ALTER TABLE public.instrument_candle
  ADD COLUMN IF NOT EXISTS fetched_at timestamp with time zone;
