--liquibase formatted sql
--changeset ffailla:2024-07-20-18-integration-test-transactions.sql
--comment: Adding test transactions for integration tests

alter table transactions
    drop column id;

insert into portfolio (id, name, symbol, creation_date, created, created_by, modified, modified_by)
values ('1c56e6eb-c0eb-46a6-a4a6-acd770d08c88', 'Test','Test', '2024-02-26 02:37:34',
        now(), 'MIGRATION|2024-02-25-008-creating-portfolio-distribution.sql',
        now(), 'MIGRATION|2024-02-25-008-creating-portfolio-distribution.sql');

INSERT INTO transactions (date_utc, side, pair, symbol, payed_with, fee, fee_symbol, created, created_by, modified, modified_by, price, payed_amount, fee_amount, executed, portfolio_id)
VALUES ('2022-10-20 00:00:00.000000', 'BUY', 'BANDBTC', 'BAND', 'BTC', null, null, '2024-07-26 12:00:00.000000', 'Process File', '2024-07-26 12:00:00.000000', 'Process File', 0.0000618000, 0.0050965000000, null, 82.5, '1c56e6eb-c0eb-46a6-a4a6-acd770d08c88');
-- the test chooses priceInStable = 3, executed = 82.5 and paidAmount 247.5

INSERT INTO transactions (date_utc, side, pair, symbol, payed_with, fee, fee_symbol, created, created_by, modified, modified_by, price, payed_amount, fee_amount, executed, portfolio_id)
VALUES ('2022-10-20 00:00:00.000000', 'BUY', 'BANDBTC', 'BAND', 'BTC', null, null, '2024-07-26 12:00:00.000000', 'Process File', '2024-07-26 12:00:00.000000', 'Process File', 0.0000936000, 0.0051480000000, null, 55.0, '1c56e6eb-c0eb-46a6-a4a6-acd770d08c88');
-- the test chooses priceInStable = 3, executed = 55 and paidAmount 165
    
INSERT INTO transactions (date_utc, side, pair, symbol, payed_with, fee, fee_symbol, created, created_by, modified, modified_by, price, payed_amount, fee_amount, executed, portfolio_id)
VALUES ('2022-10-20 00:00:00.000000', 'SELL', 'BANDBTC', 'BAND', 'BTC', null, null, '2024-07-26 12:00:00.000000', 'Process File', '2024-07-26 12:00:00.000000', 'Process File', 0.0001311000, 0.0099832200000, null, 76.2, '1c56e6eb-c0eb-46a6-a4a6-acd770d08c88');
-- the test chooses priceInStable = 3, executed = 76.2 and paidAmount 228.600000000000000000

INSERT INTO transactions (date_utc, side, pair, symbol, payed_with, fee, fee_symbol, created, created_by, modified, modified_by, price, payed_amount, fee_amount, executed, portfolio_id)
VALUES ('2022-10-20 00:00:00.000000', 'BUY', 'BANDUSDT', 'BAND', 'USDT', null, null, '2024-07-26 12:00:00.000000', 'Process File', '2024-07-26 12:00:00.000000', 'Process File', 1.209, 28.5624000000, null, 23.6, '1c56e6eb-c0eb-46a6-a4a6-acd770d08c88');

INSERT INTO transactions (date_utc, side, pair, symbol, payed_with, fee, fee_symbol, created, created_by, modified, modified_by, price, payed_amount, fee_amount, executed, portfolio_id)
VALUES ('2023-10-20 00:00:00.000000', 'SELL', 'BANDUSDT', 'BAND', 'USDT', null, null, '2024-07-26 12:00:00.000000', 'Process File', '2024-07-26 12:00:00.000000', 'Process File', 1.79, 100.2400000000, null, 56.0, '1c56e6eb-c0eb-46a6-a4a6-acd770d08c88');

INSERT INTO transactions (date_utc, side, pair, symbol, payed_with, fee, fee_symbol, created, created_by, modified, modified_by, price, payed_amount, fee_amount, executed, portfolio_id)
VALUES ('2023-10-20 00:00:00.000000', 'BUY', 'BANDUSDT', 'BAND', 'USDT', null, null, '2024-07-26 12:00:00.000000', 'Process File', '2024-07-26 12:00:00.000000', 'Process File', 1.209, 55.1544000000, null, 45.6, '1c56e6eb-c0eb-46a6-a4a6-acd770d08c88');

-- USDT Price
INSERT INTO price_history (id, name, time, pair, symbol, symbolpair, high, low, open, close, volumeto, volumefrom, created, created_by, modified, modified_by)
VALUES ('e1957d19-681b-422d-af95-58d7d9442722', null, '2022-10-20 00:00:00.000000', 'BANDUSDT', 'BAND', 'USDT', 3, 2, 1.93500, 1.90700, 137175.61000, 71195.10000, '2024-01-08 13:09:00.736039', 'Request to CryptoCompare when Processing File', '2024-01-08 13:09:00.736054', 'Request to CryptoCompare when Processing File');
INSERT INTO price_history (id, name, time, pair, symbol, symbolpair, high, low, open, close, volumeto, volumefrom, created, created_by, modified, modified_by)
VALUES ('e1957d19-681b-422d-af95-58d7d9442742', null, '2022-10-20 00:00:00.000000', 'BANDUSDT', 'BAND', 'USDT', 3, 2, 1.93500, 1.90700, 137175.61000, 71195.10000, '2024-01-08 13:09:00.736039', 'Request to CryptoCompare when Processing File', '2024-01-08 13:09:00.736054', 'Request to CryptoCompare when Processing File');
INSERT INTO price_history (id, name, time, pair, symbol, symbolpair, high, low, open, close, volumeto, volumefrom, created, created_by, modified, modified_by)
VALUES ('e1957d19-681b-422d-af95-58d7d9442732', null, '2022-10-20 00:00:00.000000', 'BANDUSDT', 'BAND', 'USDT', 3, 2, 1.93500, 1.90700, 137175.61000, 71195.10000, '2024-01-08 13:09:00.736039', 'Request to CryptoCompare when Processing File', '2024-01-08 13:09:00.736054', 'Request to CryptoCompare when Processing File');
INSERT INTO price_history (id, name, time, pair, symbol, symbolpair, high, low, open, close, volumeto, volumefrom, created, created_by, modified, modified_by)
VALUES ('e1957d19-681b-422d-af95-58d7d9442712', null, '2022-10-20 00:00:00.000000', 'BANDUSDT', 'BAND', 'USDT', 3, 2, 1.93500, 1.90700, 137175.61000, 71195.10000, '2024-01-08 13:09:00.736039', 'Request to CryptoCompare when Processing File', '2024-01-08 13:09:00.736054', 'Request to CryptoCompare when Processing File');

INSERT INTO price_history (id, name, time, pair, symbol, symbolpair, high, low, open, close, volumeto, volumefrom, created, created_by, modified, modified_by)
VALUES ('1a58f251-bf36-4ab7-ad60-d99a54e99b2b', null, '2022-10-19 21:00:00.000000', 'BANDUSDT', 'BAND', 'USDT', 21, 3, 1.22300, 1.17300, 757806.36000, 647679.06000, '2024-04-18 02:26:39.829672', 'Request to CryptoCompare when Processing File', '2024-04-18 02:26:39.829673', 'Request to CryptoCompare when Processing File');
INSERT INTO price_history (id, name, time, pair, symbol, symbolpair, high, low, open, close, volumeto, volumefrom, created, created_by, modified, modified_by)
VALUES ('1a58f251-bf36-4ab7-ad60-d99a54e99b2c', null, '2022-10-19 20:00:00.000000', 'BANDUSDT', 'BAND', 'USDT', 20, 3, 1.22300, 1.17300, 757806.36000, 647679.06000, '2024-04-18 02:26:39.829672', 'Request to CryptoCompare when Processing File', '2024-04-18 02:26:39.829673', 'Request to CryptoCompare when Processing File');
INSERT INTO price_history (id, name, time, pair, symbol, symbolpair, high, low, open, close, volumeto, volumefrom, created, created_by, modified, modified_by)
VALUES ('d25a7dcb-27b1-4034-8602-5c130500bcbd', null, '2022-10-19 19:00:00.000000', 'BANDUSDT', 'BAND', 'USDT', 19, 4, 1.23300, 1.22300, 64274.75000, 52439.71000, '2024-04-18 02:26:39.829668', 'Request to CryptoCompare when Processing File', '2024-04-18 02:26:39.829669', 'Request to CryptoCompare when Processing File');
INSERT INTO price_history (id, name, time, pair, symbol, symbolpair, high, low, open, close, volumeto, volumefrom, created, created_by, modified, modified_by)
VALUES ('1a58f251-bf36-4ab7-ad60-d99a54e99b2e', null, '2022-10-19 18:00:00.000000', 'BANDUSDT', 'BAND', 'USDT', 3, 3, 1.22300, 1.17300, 757806.36000, 647679.06000, '2024-04-18 02:26:39.829672', 'Request to CryptoCompare when Processing File', '2024-04-18 02:26:39.829673', 'Request to CryptoCompare when Processing File');
INSERT INTO price_history (id, name, time, pair, symbol, symbolpair, high, low, open, close, volumeto, volumefrom, created, created_by, modified, modified_by)
VALUES ('d25a7dcb-27b1-4034-8602-5c130500bcbf', null, '2022-10-19 17:00:00.000000', 'BANDUSDT', 'BAND', 'USDT', 4, 4, 1.23300, 1.22300, 64274.75000, 52439.71000, '2024-04-18 02:26:39.829668', 'Request to CryptoCompare when Processing File', '2024-04-18 02:26:39.829669', 'Request to CryptoCompare when Processing File');


-- BTC Price
INSERT INTO price_history (id, name, time, pair, symbol, symbolpair, high, low, open, close, volumeto, volumefrom, created, created_by, modified, modified_by)
VALUES ('7af73c39-ef51-4bc0-af89-fb5cd859f54c', null, '2022-10-19 21:00:00.000000', 'BANDBTC', 'BAND', 'BTC', 0.00021, 0.00006, 0.00006, 0.00006, 2.22900, 37552.40000, '2024-03-18 01:14:42.879512', 'Request to CryptoCompare when Processing File', '2024-03-18 01:14:42.879515', 'Request to CryptoCompare when Processing File');
INSERT INTO price_history (id, name, time, pair, symbol, symbolpair, high, low, open, close, volumeto, volumefrom, created, created_by, modified, modified_by)
VALUES ('7af73c39-ef51-4bc0-af89-fb5cd859f55c', null, '2022-10-19 20:00:00.000000', 'BANDBTC', 'BAND', 'BTC', 0.00020, 0.00006, 0.00006, 0.00006, 2.22900, 37552.40000, '2024-03-18 01:14:42.879512', 'Request to CryptoCompare when Processing File', '2024-03-18 01:14:42.879515', 'Request to CryptoCompare when Processing File');
INSERT INTO price_history (id, name, time, pair, symbol, symbolpair, high, low, open, close, volumeto, volumefrom, created, created_by, modified, modified_by)
VALUES ('7af73c39-ef51-4bc0-af89-fb5cd859f56c', null, '2022-10-19 19:00:00.000000', 'BANDBTC', 'BAND', 'BTC', 0.00019, 0.00006, 0.00006, 0.00006, 2.22900, 37552.40000, '2024-03-18 01:14:42.879512', 'Request to CryptoCompare when Processing File', '2024-03-18 01:14:42.879515', 'Request to CryptoCompare when Processing File');
INSERT INTO price_history (id, name, time, pair, symbol, symbolpair, high, low, open, close, volumeto, volumefrom, created, created_by, modified, modified_by)
VALUES ('7af73c39-ef51-4bc0-af89-fb5cd859f57c', null, '2022-10-19 18:00:00.000000', 'BANDBTC', 'BAND', 'BTC', 0.00018, 0.00006, 0.00006, 0.00006, 2.22900, 37552.40000, '2024-03-18 01:14:42.879512', 'Request to CryptoCompare when Processing File', '2024-03-18 01:14:42.879515', 'Request to CryptoCompare when Processing File');
INSERT INTO price_history (id, name, time, pair, symbol, symbolpair, high, low, open, close, volumeto, volumefrom, created, created_by, modified, modified_by)
VALUES ('7af73c39-ef51-4bc0-af89-fb5cd859f58c', null, '2022-10-19 17:00:00.000000', 'BANDBTC', 'BAND', 'BTC', 0.00006, 0.00006, 0.00006, 0.00006, 2.22900, 37552.40000, '2024-03-18 01:14:42.879512', 'Request to CryptoCompare when Processing File', '2024-03-18 01:14:42.879515', 'Request to CryptoCompare when Processing File');
