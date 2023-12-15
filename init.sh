#!/bin/bash
network_name="ldap-network"
if ! docker network ls | grep -q "$network_name"; then
    echo "Creating network: $network_name"
    docker network create "$network_name"
else
    echo "Network $network_name already exists, skipping creation."
fi
echo "Starting $service..."
(cd "$service" && docker compose up -d)