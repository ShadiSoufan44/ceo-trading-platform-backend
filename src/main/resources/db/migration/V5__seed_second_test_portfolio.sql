-- Add a second portfolio to the deterministic local test account.
INSERT INTO public.portfolio (portfolio_id, type, client_id)
VALUES (
    '6e5f6e67-b8d8-43fc-8f3c-0e91b64d8d2b',
    'RETIREMENT',
    '2c522b92-7151-4daa-92db-81a2253c772a'
)
ON CONFLICT (portfolio_id) DO NOTHING;

-- Seed holdings for the second portfolio.
INSERT INTO public.holding (holding_id, order_id, portfolio_id, instrument_id, quantity, purchased_price)
VALUES
    ('2c4c4c9c-7c77-4c55-9d7f-77f11a2d1a21', NULL, '6e5f6e67-b8d8-43fc-8f3c-0e91b64d8d2b', '6eaa4d1a-3d44-4b66-8a2a-2ed0fce1a7c2', '60', '98.75'),
    ('8d2a9f22-5a8b-4db0-9c0c-7c0f5a8d7d31', NULL, '6e5f6e67-b8d8-43fc-8f3c-0e91b64d8d2b', '7f0c7b73-1b49-4f86-8c62-5b4f7d0c7f93', '150', '380.00'),
    ('4f2d9b1f-2d3e-4a7c-b2e1-5a1b7e3c3c44', NULL, '6e5f6e67-b8d8-43fc-8f3c-0e91b64d8d2b', '8a5d0e94-2c19-4f6f-9f9b-6d8d38a7af2a', '30', '210.45')
ON CONFLICT (holding_id) DO NOTHING;