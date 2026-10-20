CREATE INDEX IF NOT EXISTS idx_order_status_change_fulfilled_lookup
  ON public.order_status_change(status, order_id, change_date DESC);

CREATE INDEX IF NOT EXISTS idx_order_add_instrument_id
  ON public."order"(add_instrument_id);

CREATE INDEX IF NOT EXISTS idx_order_client_id
  ON public."order"(client_id);