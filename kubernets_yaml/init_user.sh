echo "
#!/bin/bash
psql -v ON_ERROR_STOP=1 --username \"$POSTGRES_USER\" --dbname \"$POSTGRES_DB\" <<-EOSQL
    CREATE USER '$DB_ADMIN_USER' WITH PASSWORD '$DB_ADMIN_PASSWORD';
EOSQL
" > init-user.sh
