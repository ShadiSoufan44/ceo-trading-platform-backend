CREATE INDEX IF NOT EXISTS idx_order_subtract_instrument_id
  ON public."order"(subtract_instrument_id);