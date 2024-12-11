#!/bin/bash
set -e

echo "Running init_user.sh"

echo "Creating user $DB_USER with password $DB_PASSWORD"

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    DO
    \$\$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '$DB_USER') THEN
            CREATE USER $DB_USER WITH PASSWORD '$DB_PASSWORD';
        END IF;
    END
    \$\$
    ;
EOSQL

echo "User $DB_USER created successfully"