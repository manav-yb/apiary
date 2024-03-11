#!/bin/bash
set -ex

SCRIPT_DIR=$(dirname $(realpath $0))

# Start Postgres Docker image.
docker pull postgres:14.5-bullseye

PGCONFIG=""  # Postgres config file.
NAME="apiary-prov"
PORT=5433
if [[ $# -eq 1 ]]; then
    PGCONFIG="$PWD/$1"
fi

if [[ $# -eq 3 ]]; then
    PGCONFIG="$PWD/$1"
    NAME="$2"
    PORT="$3"
fi

# Set the password to dbos, default user is postgres.
if [[ -z "$PGCONFIG" ]]; then
    docker run -p 5433:5432 -d --rm --name="$NAME" --env PGDATA=/var/lib/postgresql-static/data --env POSTGRES_PASSWORD=dbos postgres:14.5-bullseye
else
    # Use customized config file
    docker run -d --rm --name="$NAME" --env PGDATA=/var/lib/postgresql-static/data --env POSTGRES_PASSWORD=dbos \
    -v "$PGCONFIG":/etc/postgresql/postgresql.conf \
    postgres:14.5-bullseye -c 'config_file=/etc/postgresql/postgresql.conf'
fi

sleep 10

#docker exec -i $NAME psql -h localhost -U postgres -p $PORT 
 psql -h localhost -U postgres -p $PORT 
