-- WARNING: This schema is for context only and is not meant to be run.
-- Table order and constraints may not be valid for execution.

CREATE TABLE public.user_account (
  full_name text,
  email text,
  password_hash text,
  join_date timestamp with time zone,
  role text,
  user_id uuid NOT NULL DEFAULT gen_random_uuid(),
  CONSTRAINT user_account_pkey PRIMARY KEY (user_id)
);

CREATE TABLE public.instrument (
  symbol text,
  full_name text,
  instrument_type text,
  instrument_id uuid NOT NULL DEFAULT gen_random_uuid(),
  CONSTRAINT instrument_pkey PRIMARY KEY (instrument_id)
);

CREATE TABLE public.portfolio (
  type text,
  portfolio_id uuid NOT NULL DEFAULT gen_random_uuid(),
  client_id uuid DEFAULT gen_random_uuid(),
  CONSTRAINT portfolio_pkey PRIMARY KEY (portfolio_id),
  CONSTRAINT portfolio_client_id_fkey FOREIGN KEY (client_id) REFERENCES public.user_account(user_id)
);

CREATE TABLE public."order" (
  side text,
  quote numeric,
  increase_threshold numeric,
  quantity numeric,
  portfolio_id uuid DEFAULT gen_random_uuid(),
  user_id uuid DEFAULT gen_random_uuid(),
  instrument_id uuid DEFAULT gen_random_uuid(),
  order_id uuid NOT NULL DEFAULT gen_random_uuid(),
  CONSTRAINT order_pkey PRIMARY KEY (order_id),
  CONSTRAINT order_portfolio_id_fkey FOREIGN KEY (portfolio_id) REFERENCES public.portfolio(portfolio_id),
  CONSTRAINT order_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.user_account(user_id),
  CONSTRAINT order_instrument_id_fkey FOREIGN KEY (instrument_id) REFERENCES public.instrument(instrument_id)
);

CREATE TABLE public.holding (
  order_date timestamp with time zone,
  purchased_price numeric,
  quantity numeric,
  holding_id uuid NOT NULL DEFAULT gen_random_uuid(),
  order_id uuid DEFAULT gen_random_uuid(),
  portfolio_id uuid DEFAULT gen_random_uuid(),
  CONSTRAINT holding_pkey PRIMARY KEY (holding_id),
  CONSTRAINT holding_order_id_fkey FOREIGN KEY (order_id) REFERENCES public."order"(order_id),
  CONSTRAINT holding_portfolio_id_fkey FOREIGN KEY (portfolio_id) REFERENCES public.portfolio(portfolio_id)
);

CREATE TABLE public.order_status_change (
  status text,
  message text,
  change_date timestamp with time zone,
  order_status_change_id uuid NOT NULL DEFAULT gen_random_uuid(),
  order_id uuid DEFAULT gen_random_uuid(),
  CONSTRAINT order_status_change_pkey PRIMARY KEY (order_status_change_id),
  CONSTRAINT order_status_change_order_id_fkey FOREIGN KEY (order_id) REFERENCES public."order"(order_id)
);