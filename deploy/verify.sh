#!/bin/sh
# Verification of the deployed STC Platform on this host.
BASE=http://127.0.0.1:18080

echo "=== service ==="
systemctl is-active sstc
systemctl show sstc -p MemoryCurrent --value | awk '{printf "memory=%.1f MB\n", $1/1024/1024}'

echo "=== health ==="
curl -s --max-time 10 "$BASE/api/health"
echo

echo "=== login + protected call ==="
TOKEN=$(curl -s --max-time 10 -X POST "$BASE/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"username":"lead","password":"Lead@123"}' \
  | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
echo "token length: ${#TOKEN}"
echo -n "board columns: "
curl -s --max-time 10 "$BASE/api/tasks/board" -H "Authorization: Bearer $TOKEN" \
  | grep -o '"status":"[A-Z_]*"' | wc -l
echo -n "open tasks: "
curl -s --max-time 10 "$BASE/api/tasks/summary" -H "Authorization: Bearer $TOKEN" \
  | sed -n 's/.*"total":\([0-9]*\).*/\1/p'
echo -n "channels: "
curl -s --max-time 10 "$BASE/api/channels" -H "Authorization: Bearer $TOKEN" \
  | grep -o '"id":' | wc -l
echo -n "meeting summary decisions: "
curl -s --max-time 10 "$BASE/api/meetings/1/summary" -H "Authorization: Bearer $TOKEN" \
  | sed -n 's/.*"decisions":\[\(.*\)\].*/\1/p' | tr ',' '\n' | wc -l

echo "=== unauthenticated call must be rejected ==="
curl -s -o /dev/null -w "status=%{http_code}\n" --max-time 10 "$BASE/api/tasks/board"

echo "=== listening sockets ==="
ss -tlnp 2>/dev/null | grep -E '18080|:80 |:443 ' || echo "only 18080 expected"

echo "=== firewall ==="
if command -v ufw >/dev/null 2>&1; then ufw status; else echo "no ufw"; fi
if command -v firewall-cmd >/dev/null 2>&1; then firewall-cmd --list-ports; else echo "no firewalld"; fi
iptables -L INPUT -n 2>/dev/null | head -6

echo "=== h2 console must be closed ==="
curl -s -o /dev/null -w "h2-console status=%{http_code}\n" --max-time 10 "$BASE/h2-console"

echo "=== frontend bundle ==="
curl -s --max-time 10 "$BASE/" | grep -o 'assets/index-[A-Za-z0-9_-]*\.js' | head -1

echo "=== seeding ==="
grep -c 'Demo data ready' /opt/sstc/logs/app.log
tail -3 /opt/sstc/logs/app.log
