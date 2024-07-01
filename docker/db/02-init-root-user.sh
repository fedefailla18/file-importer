#!/bin/bash

set -e

echo "Creating role root with password 'password'..."
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE USER root WITH SUPERUSER PASSWORD 'password';
EOSQL

echo "Role root created successfully"
