#!/bin/sh
# Sauvegarde de la base dans backups/ (a lancer chaque nuit par cron sur le serveur) :
#   0 3 * * * cd /opt/madeqrcard && ./scripts/sauvegarde.sh
# Restauration :
#   gunzip -c backups/madeqrcard-AAAAMMJJ-HHMM.sql.gz | docker compose exec -T db psql -U madeqrcard -d madeqrcard
set -eu
cd "$(dirname "$0")/.."
mkdir -p backups
fichier="backups/madeqrcard-$(date +%Y%m%d-%H%M).sql.gz"
docker compose exec -T db pg_dump -U madeqrcard -d madeqrcard --clean --if-exists | gzip > "$fichier"
# On garde les 14 dernieres sauvegardes
ls -1t backups/madeqrcard-*.sql.gz | tail -n +15 | xargs -r rm --
echo "Sauvegarde : $fichier"
