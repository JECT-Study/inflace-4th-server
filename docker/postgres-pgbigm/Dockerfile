FROM postgres:17

ARG PGBIGM_REF=v1.2-20250903

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        build-essential \
        ca-certificates \
        git \
        libicu-dev \
        postgresql-server-dev-17 \
    && git clone --depth 1 --branch "${PGBIGM_REF}" https://github.com/pgbigm/pg_bigm.git /tmp/pg_bigm \
    && make -C /tmp/pg_bigm USE_PGXS=1 \
    && make -C /tmp/pg_bigm USE_PGXS=1 install \
    && apt-get purge -y --auto-remove \
        build-essential \
        git \
        libicu-dev \
        postgresql-server-dev-17 \
    && rm -rf /var/lib/apt/lists/* /tmp/pg_bigm
