#!/bin/sh
# usage: http-check.sh <url> <attempts>
url="$1"
attempts="${2:-5}"
i=1
while [ "$i" -le "$attempts" ]; do
  if curl -sf --max-time 5 "$url" > /dev/null; then
    echo "OK on attempt $i"
    exit 0
  fi
  echo "attempt $i failed, retrying..."
  i=$((i+1))
  sleep 2
done
echo "FAILED after $attempts attempts"
exit 1