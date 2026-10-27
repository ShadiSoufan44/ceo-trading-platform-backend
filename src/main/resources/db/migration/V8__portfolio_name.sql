ALTER TABLE public.portfolio
  ADD COLUMN name varchar(100);

UPDATE public.portfolio
SET name = CASE
  WHEN type IS NULL OR btrim(type) = '' THEN 'My Portfolio'
  ELSE initcap(lower(btrim(type))) || ' Portfolio'
END;

ALTER TABLE public.portfolio
  ALTER COLUMN name SET NOT NULL,
  ALTER COLUMN name SET DEFAULT 'Portfolio',
  ADD CONSTRAINT portfolio_name_not_blank CHECK (btrim(name) <> '');