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
    ('f795c0de-4e4c-4a63-9695-865b41856d25', 'AAPL', 'Apple, Inc.', 'EQUITY');

-- Insert orders
INSERT INTO public.order (order_id, client_id, add_instrument_id, subtract_instrument_id, portfolio_id, quantity, change_threshold, side)
VALUES
    ('fcd87c91-9e4e-403c-ab92-8f8da4722e4d', '2c522b92-7151-4daa-92db-81a2253c772a', '327636f8-36d0-4052-80be-0c3e58ed6c41', NULL, 'edda8da3-fab9-4b3c-b792-c833abe8e146', '1000000', '0', 'DEPOSIT');

-- Insert holdings
INSERT INTO public.holding (holding_id, order_id, portfolio_id, instrument_id, quantity, purchased_price)
VALUES
    ('250464cd-392b-4d6c-b16c-1f55a47ee91c', 'fcd87c91-9e4e-403c-ab92-8f8da4722e4d', 'edda8da3-fab9-4b3c-b792-c833abe8e146', '327636f8-36d0-4052-80be-0c3e58ed6c41', '1000000', '1');