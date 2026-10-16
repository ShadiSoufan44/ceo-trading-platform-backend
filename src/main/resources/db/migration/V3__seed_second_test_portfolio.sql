-- Add a second portfolio to the deterministic local test account.
INSERT INTO public.portfolio (portfolio_id, type, client_id)
VALUES (
    '6e5f6e67-b8d8-43fc-8f3c-0e91b64d8d2b',
    'RETIREMENT',
    '2c522b92-7151-4daa-92db-81a2253c772a'
)
ON CONFLICT (portfolio_id) DO NOTHING;