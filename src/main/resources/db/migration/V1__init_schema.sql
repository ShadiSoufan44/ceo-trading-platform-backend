-- WARNING: This schema is for context only and is not meant to be run.
-- Table order and constraints may not be valid for execution.

CREATE TABLE public.user_account (
  user_id integer GENERATED ALWAYS AS IDENTITY NOT NULL,
  full_name text,
  email text,
  password_hash text,
  join_date timestamp with time zone,
  role text,
  CONSTRAINT user_account_pkey PRIMARY KEY (user_id)
);
CREATE TABLE public.portfolio (
  portfolio_id integer GENERATED ALWAYS AS IDENTITY NOT NULL,
  user_id integer,
  type text,
  CONSTRAINT portfolio_pkey PRIMARY KEY (portfolio_id),
  CONSTRAINT portfolio_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.user_account(user_id)
);
CREATE TABLE public.order (
  order_id integer GENERATED ALWAYS AS IDENTITY NOT NULL,
  instr_id integer,
  side text,
  portfolio_id integer,
  user_id integer,
  quote numeric,
  increase_threshold numeric,
  CONSTRAINT order_pkey PRIMARY KEY (order_id),
  CONSTRAINT order_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.user_account(user_id),
  CONSTRAINT order_instr_id_fkey FOREIGN KEY (instr_id) REFERENCES public.instrument(instrument_id),
  CONSTRAINT order_portfolio_id_fkey FOREIGN KEY (portfolio_id) REFERENCES public.portfolio(portfolio_id)
);
CREATE TABLE public.instrument (
  instrument_id integer GENERATED ALWAYS AS IDENTITY NOT NULL,
  symbol text,
  full_name text,
  instrument_type text,
  CONSTRAINT instrument_pkey PRIMARY KEY (instrument_id)
);
CREATE TABLE public.holding (
  holding_id integer GENERATED ALWAYS AS IDENTITY NOT NULL,
  order_date timestamp with time zone,
  portfolio_id integer,
  purchased_price numeric,
  quantity numeric,
  order_id integer,
  CONSTRAINT holding_pkey PRIMARY KEY (holding_id),
  CONSTRAINT Holding_holding_id_fkey FOREIGN KEY (holding_id) REFERENCES public.portfolio(portfolio_id),
  CONSTRAINT holding_order_id_fkey FOREIGN KEY (order_id) REFERENCES public.order(order_id)
);
CREATE TABLE public.flyway_schema_history (
  installed_rank integer NOT NULL,
  version character varying,
  description character varying NOT NULL,
  type character varying NOT NULL,
  script character varying NOT NULL,
  checksum integer,
  installed_by character varying NOT NULL,
  installed_on timestamp without time zone NOT NULL DEFAULT now(),
  execution_time integer NOT NULL,
  success boolean NOT NULL,
  CONSTRAINT flyway_schema_history_pkey PRIMARY KEY (installed_rank)
);
CREATE TABLE public.order_status_change (
  order_status_change_id bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
  order_id integer,
  status text,
  message text,
  change_date timestamp with time zone,
  CONSTRAINT order_status_change_pkey PRIMARY KEY (order_status_change_id),
  CONSTRAINT OrderStatusChange_order_id_fkey FOREIGN KEY (order_id) REFERENCES public.order(order_id)
);