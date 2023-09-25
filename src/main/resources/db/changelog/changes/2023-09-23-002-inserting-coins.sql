--liquibase formatted sql
--changeset ffailla:2023-09-23-002-inserting-coins.sql
--comment: inserting coins

INSERT INTO symbol(name, symbol, creation_date, created, created_by, modified, modified_by)
values ('Bitcoin', 'BTC', TO_TIMESTAMP(
                            '2009-01-03', 'YYYY-MM-DD'), now(),
                            'SCRIPT|2023-09-23-002-inserting-coins.sql', null, null
                          );
INSERT INTO symbol(name, symbol, creation_date, created, created_by, modified, modified_by)
values ('Ethereum', 'ETH', TO_TIMESTAMP(
                            '2015-07-30', 'YYYY-MM-DD'), now(),
                            'SCRIPT|2023-09-23-002-inserting-coins.sql', null, null
                          );
INSERT INTO symbol(name, symbol, creation_date, created, created_by, modified, modified_by)
values ('Tether', 'USDT', TO_TIMESTAMP(
                            '2015-03-02', 'YYYY-MM-DD'), now(),
                            'SCRIPT|2023-09-23-002-inserting-coins.sql', null, null
                          );
INSERT INTO symbol(name, symbol, creation_date, created, created_by, modified, modified_by)
values ('Binance USD', 'BUSD', TO_TIMESTAMP(
                            '2019-09-10', 'YYYY-MM-DD'), now(),
                            'SCRIPT|2023-09-23-002-inserting-coins.sql', null, null
                          );