#!/bin/sh
# Sauvegarde de la base dans backups/ (a lancer chaque nuit par cron sur le serveur) :
#   0 3 * * * cd /opt/qrcard && ./scripts/sauvegarde.sh
# Restauration :
#   gunzip -c backups/qrcard-AAAAMMJJ-HHMM.sql.gz | docker compose exec -T db psql -U qrcard -d qrcard
set -eu
cd "$(dirname "$0")/.."
mkdir -p backups
fichier="backups/qrcard-$(date +%Y%m%d-%H%M).sql.gz"
docker compose exec -T db pg_dump -U qrcard -d qrcard --clean --if-exists | gzip > "$fichier"
# On garde les 14 dernieres sauvegardes
ls -1t backups/qrcard-*.sql.gz | tail -n +15 | xargs -r rm --
echo "Sauvegarde : $fichier"
