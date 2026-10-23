-- src/main/resources/db/migration/V2__seed_data.sql

-- Insert test users
INSERT INTO public.user_account (user_id, full_name, email, password_hash, join_date, role)
VALUES 
    ('2c522b92-7151-4daa-92db-81a2253c772a', 'John Doe', 'johndoe@email.com', 'HASHED_PASSWORD', NOW(), 'CLIENT');

-- Insert portfolios
INSERT INTO public.portfolio (portfolio_id, type, client_id)
VALUES 
    ('edda8da3-fab9-4b3c-b792-c833abe8e146', 'BROKERAGE', '2c522b92-7151-4daa-92db-81a2253c772a');

-- Insert instruments
INSERT INTO public.instrument (instrument_id, symbol, full_name, instrument_type)
VALUES 
    ('327636f8-36d0-4052-80be-0c3e58ed6c41', 'USD', 'United States Dollar', 'CASH'),
    ('f795c0de-4e4c-4a63-9695-865b41856d25', 'AAPL', 'Apple, Inc.', 'EQUITY'),
    ('3c3f7a56-0a5d-4a47-b3d4-7c5f70f8d1c1', 'MSFT', 'Microsoft Corporation', 'EQUITY'),
    ('41d4c0c5-3c63-4b41-8c9d-7f61b8b6d2af', 'NVDA', 'NVIDIA Corporation', 'EQUITY'),
    ('5f5e4f8f-1d7d-4d2e-a52e-07d7d5e6a2ab', 'AMZN', 'Amazon.com, Inc.', 'EQUITY'),
    ('6eaa4d1a-3d44-4b66-8a2a-2ed0fce1a7c2', 'GOOGL', 'Alphabet Inc.', 'EQUITY'),
    ('7f0c7b73-1b49-4f86-8c62-5b4f7d0c7f93', 'VOO', 'Vanguard S&P 500 ETF', 'EQUITY'),
    ('8a5d0e94-2c19-4f6f-9f9b-6d8d38a7af2a', 'META', 'Meta Platforms, Inc.', 'EQUITY');

-- Insert orders
INSERT INTO public.order (order_id, client_id, add_instrument_id, subtract_instrument_id, portfolio_id, quantity, change_threshold, side, market)
VALUES
    ('fcd87c91-9e4e-403c-ab92-8f8da4722e4d', '2c522b92-7151-4daa-92db-81a2253c772a', '327636f8-36d0-4052-80be-0c3e58ed6c41', NULL, 'edda8da3-fab9-4b3c-b792-c833abe8e146', '1000000', '0', 'DEPOSIT', 'America/New_York');

-- Insert holdings
INSERT INTO public.holding (holding_id, order_id, portfolio_id, instrument_id, quantity, purchased_price)
VALUES
    ('250464cd-392b-4d6c-b16c-1f55a47ee91c', 'fcd87c91-9e4e-403c-ab92-8f8da4722e4d', 'edda8da3-fab9-4b3c-b792-c833abe8e146', '327636f8-36d0-4052-80be-0c3e58ed6c41', '1000000', '1'),
    ('c7db0d44-4b24-4a82-b45d-7e0a3c03fb11', NULL, 'edda8da3-fab9-4b3c-b792-c833abe8e146', 'f795c0de-4e4c-4a63-9695-865b41856d25', '25', '168.32'),
    ('3e7d4b6d-0b0a-4a32-8e12-4d2f7f7b5c28', NULL, 'edda8da3-fab9-4b3c-b792-c833abe8e146', '3c3f7a56-0a5d-4a47-b3d4-7c5f70f8d1c1', '12', '310.10'),
    ('6a54a8f8-2b61-4b49-b0e5-b86f7cf2f7ac', NULL, 'edda8da3-fab9-4b3c-b792-c833abe8e146', '41d4c0c5-3c63-4b41-8c9d-7f61b8b6d2af', '40', '45.02'),
    ('1f7f4d8a-2d86-47bf-bb1a-57d7d2d7f4d9', NULL, 'edda8da3-fab9-4b3c-b792-c833abe8e146', '5f5e4f8f-1d7d-4d2e-a52e-07d7d5e6a2ab', '8', '145.60');