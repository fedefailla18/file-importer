#!/bin/bash

# Eliminar los contenedores con docker-compose que existan
docker-compose down 

# Eliminar la carpeta ./data si existe
if [ -d "./data" ]; then
  echo "Deleting existing data directory..."
  rm -rf ./data
fi

# Crear la carpeta ./data nuevamente
mkdir ./data

# Levantar los contenedores con docker-compose
docker-compose up --build -d
