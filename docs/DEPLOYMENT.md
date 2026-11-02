# Deployment guide

How to run the STC Platform on a server, and how much machine it actually needs.

The figures below are **measured**, not guessed: they come from the release build running on
Windows with `-Xms128m -Xmx256m -XX:+UseSerialGC` (see *Measured footprint*), and from the live
deployment described in *Reference deployment*.

---

## 0. Reference deployment (this project's live demo)

| Item | Value |
| --- | --- |
| URL | **<http://113.44.246.242:18080>** |
| Host | 2 vCPU / 1.8 GB RAM / 40 GB disk, Debian 12 (bookworm), x86_64 |
| Runtime | OpenJDK 17 (`openjdk-17-jre-headless`), jar built for Java 8 |
| Process manager | systemd unit `/etc/systemd/system/sstc.service` (`Restart=always`) |
| Layout | `/opt/sstc/sstc-platform.jar`, `/opt/sstc/data` (H2), `/opt/sstc/logs/app.log` |
| JVM flags | `-Xms128m -Xmx512m -XX:+UseSerialGC -XX:MaxMetaspaceSize=192m -Dfile.encoding=UTF-8` |
| Flags | `--server.port=18080 --spring.h2.console.enabled=false --sstc.seed.reset=false` |
| Measured resident memory | **266 MB** at rest with the demo data loaded |
| Domain | none registered, so the service is exposed on a high port over plain HTTP |

Exposure note: the host answers on port 18080 directly (no reverse proxy, no TLS). That is
acceptable for a teaching demo whose credentials are public, but any real use should put nginx and
a certificate in front of it and stop publishing the high port.

Update procedure used on that host:

```bash
# from the workstation
mvn -DskipTests package
pscp backend/target/sstc-platform.jar root@HOST:/opt/sstc/sstc-platform.jar

# on the server
systemctl restart sstc && curl -s localhost:18080/api/health
```

---

## 1. How much server do you need?

### Measured footprint

| Metric | Value | How it was measured |
| --- | --- | --- |
| Idle resident memory (Windows, capped heap) | **≈ 300 MB** | JVM with a 256 MB heap cap, after startup and seeding |
| Resident memory on the Linux demo host | **266 MB** | `systemctl show sstc -p MemoryCurrent`, demo data loaded |
| Resident memory after 250 requests | **≈ 420 MB** | 250 sequential calls across 5 endpoints |
| Memory growth under load | ≈ +20 MB per 200 concurrent requests | No leak visible over the measured window |
| Heap you should allow | 256–512 MB (`-Xmx`) | The workload is CRUD over a small dataset |
| CPU at rest | effectively 0 | One scheduled snapshot job per day |
| CPU under load | 15 ms average latency per request, single threaded client | ~65 req/s from a single Windows PowerShell client; the server was not the bottleneck |
| Disk: jar | 47 MB | `backend/target/sstc-platform.jar` |
| Disk: database | ~0.5 MB for the seeded demo | `data/sstc.mv.db`; grows with real usage only |
| Disk: frontend bundle | 330 KB (JS + CSS) | `backend/src/main/resources/static/assets` |
| Threads | ~49 | Tomcat pool + H2 + scheduler |

### Recommended sizes

| Scenario | vCPU | RAM | Disk | Verdict |
| --- | --- | --- | --- | --- |
| **Classroom demo / personal use** | 1 | **1 GB** | 10 GB | **Enough.** Run `-Xmx512m`, expect ~450–550 MB resident. A 1 GB VPS with nothing else on it is fine. |
| **Small team, always on (recommended)** | 2 | **2 GB** | 20 GB | **Comfortable.** `-Xmx768m`, room for the OS, nginx and a database backup. This is the sweet spot. |
| **Course/team showcase with CI or several services** | 2 | 4 GB | 40 GB | Plenty of headroom; you can co-host the reverse proxy, a build agent or a second environment. |
| 1 vCPU / 512 MB | 1 | 512 MB | 10 GB | **Not recommended.** The JVM plus the OS will swap or get OOM-killed. Only viable with `-Xmx256m`, a swap file, and no reverse proxy. |
| 1 vCPU / 256 MB | 1 | 256 MB | — | Will not run reliably. |

**Bottom line: 2 vCPU + 2 GB RAM + 20 GB disk is the recommendation.** 1 vCPU + 1 GB works for a
demo or a handful of users. Below 1 GB you will fight the JVM.

Cheap options that satisfy this (China-friendly, prices change — verify before buying): the entry
tiers of Alibaba Cloud / Tencent Cloud lightweight servers (2 vCPU 2 GB, often promoted around
¥99/year), or any 1–2 GB VPS from a provider you already use. Bandwidth needs are trivial: the whole
frontend is 330 KB and the API payloads are JSON in the kilobyte range.

### JVM flags used for the measurements

```bash
java -Xms128m -Xmx512m -XX:+UseSerialGC -XX:MaxMetaspaceSize=192m \
     -jar backend/target/sstc-platform.jar
```

`UseSerialGC` keeps the resident set smaller for a single-instance teaching deployment; switch to
the default G1 if you give the container 2 GB or more.

---

## 2. Option A — systemd on a plain Linux server (simplest)

```bash
# 1. runtime
sudo apt update && sudo apt install -y openjdk-17-jre-headless nginx

# 2. application
sudo mkdir -p /opt/sstc && cd /opt/sstc
sudo cp /path/to/sstc-platform.jar .
sudo mkdir -p data

# 3. service account
sudo useradd --system --home /opt/sstc --shell /usr/sbin/nologin sstc
sudo chown -R sstc:sstc /opt/sstc
```

`/etc/systemd/system/sstc.service`:

```ini
[Unit]
Description=STC Platform (Small Team Collaboration)
After=network.target

[Service]
Type=simple
User=sstc
Group=sstc
WorkingDirectory=/opt/sstc
ExecStart=/usr/bin/java -Xms128m -Xmx512m -XX:+UseSerialGC \
          -jar /opt/sstc/sstc-platform.jar \
          --server.port=8080
SuccessExitStatus=143
Restart=always
RestartSec=5
StandardOutput=append:/opt/sstc/logs/app.log
StandardError=append:/opt/sstc/logs/app.log

# hardening
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=full
ProtectHome=true

[Install]
WantedBy=multi-user.target
```

```bash
sudo mkdir -p /opt/sstc/logs && sudo chown sstc:sstc /opt/sstc/logs
sudo systemctl daemon-reload
sudo systemctl enable --now sstc
sudo systemctl status sstc
curl -s localhost:8080/api/health
```

Update procedure:

```bash
sudo systemctl stop sstc
sudo cp sstc-platform.jar.new /opt/sstc/sstc-platform.jar
sudo chown sstc:sstc /opt/sstc/sstc-platform.jar
sudo systemctl start sstc && curl -s localhost:8080/api/health
```

### Reverse proxy (nginx) with a domain

```nginx
server {
    listen 80;
    server_name demo.example.com;

    client_max_body_size 30m;          # keep above sstc.toolbox.storage.max-upload-mb

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 120s;
    }
}
```

Then add TLS: `sudo certbot --nginx -d demo.example.com`.

> **Do not expose the H2 console publicly.** Disable it in production with
> `--spring.h2.console.enabled=false` (it is a database console with no authentication beyond the
> JDBC credentials).

---

## 3. Option B — Docker

`Dockerfile` (multi-stage: the frontend is built first, then packaged into the jar):

```dockerfile
# ---- stage 1: frontend ----
FROM node:20-alpine AS web
WORKDIR /web
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build            # emits into ../backend/src/main/resources/static

# ---- stage 2: backend ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY pom.xml ./
COPY backend/pom.xml backend/
RUN mvn -q -pl backend -am dependency:go-offline
COPY backend/ backend/
COPY --from=web /backend/src/main/resources/static backend/src/main/resources/static
RUN mvn -q -pl backend -am -DskipTests package

# ---- stage 3: runtime ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN adduser -D -u 1001 sstc && mkdir -p /app/data && chown -R sstc /app
COPY --from=build /src/backend/target/sstc-platform.jar /app/app.jar
USER sstc
EXPOSE 8080
ENV JAVA_OPTS="-Xms128m -Xmx512m -XX:+UseSerialGC"
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s \
  CMD wget -qO- http://127.0.0.1:8080/api/health || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
```

`docker-compose.yml`:

```yaml
services:
  sstc:
    build: .
    image: sstc-platform:1.0.0
    container_name: sstc
    restart: unless-stopped
    ports:
      - "127.0.0.1:8080:8080"      # only the reverse proxy reaches it
    volumes:
      - ./data:/app/data           # the H2 database file
    environment:
      JAVA_OPTS: "-Xms128m -Xmx512m -XX:+UseSerialGC"
      SPRING_H2_CONSOLE_ENABLED: "false"
    mem_limit: 900m                # leaves room for the JVM overhead on a 2 GB host
    healthcheck:
      test: ["CMD", "wget", "-qO-", "http://127.0.0.1:8080/api/health"]
      interval: 30s
      timeout: 5s
      retries: 5
```

```bash
docker compose up -d --build
docker compose logs -f
```

> **Not verified locally.** Docker is not installed on the machine this project was developed on,
> so the Dockerfile and the compose file are written from the verified build steps
> (`npm run build`, `mvn -pl backend -am -DskipTests package`) but have not been executed. The
> environment variables they set *were* verified: starting the jar with `SERVER_PORT=8081`,
> `SPRING_H2_CONSOLE_ENABLED=false` and `SSTC_TOOLBOX_STORAGE_MAXUPLOADMB=25` moved the service to
> port 8081 and returned `404` for `/h2-console`.
>
> The Maven stage pulls its dependencies from Maven Central, because the repository does not ship
> the project-local `.m2repo` cache that the offline build helper (`mvn-local.cmd`) uses.

---

## 4. Runtime configuration

Override any setting from `application.yml` on the command line (`--key=value`) or through
environment variables (`SPRING_DATASOURCE_URL`, `SERVER_PORT`, …).

| Setting | Production suggestion | Why |
| --- | --- | --- |
| `server.port` | `8080` (behind the proxy) | Keep the app off port 80 |
| `spring.datasource.url` | `jdbc:h2:file:/opt/sstc/data/sstc;AUTO_SERVER=TRUE` | Absolute path so the working directory does not matter |
| `sstc.seed.enabled` | `true` for a demo, `false` once real data exists | Avoid recreating demo accounts |
| `sstc.seed.reset` | always `false` in production | `true` would re-seed over existing data |
| `spring.h2.console.enabled` | `false` | No public database console |
| `sstc.toolbox.storage.max-upload-mb` | match `client_max_body_size` in nginx | Otherwise large uploads fail with an unclear error |
| `sstc.security.token-ttl-hours` | `12` (or lower) | Shorter sessions on a public host |

---

## 5. Backup and restore

The entire state is one H2 file. Stop the service (or use `AUTO_SERVER` and copy consistently),
then archive:

```bash
# backup
sudo systemctl stop sstc
sudo tar czf /var/backups/sstc-$(date +%F).tar.gz -C /opt/sstc data
sudo systemctl start sstc

# restore
sudo systemctl stop sstc
sudo tar xzf /var/backups/sstc-2026-11-03.tar.gz -C /opt/sstc
sudo chown -R sstc:sstc /opt/sstc/data
sudo systemctl start sstc
```

Cron example (daily at 03:30, keep 14 days):

```cron
30 3 * * * systemctl stop sstc && tar czf /var/backups/sstc-$(date +\%F).tar.gz -C /opt/sstc data && systemctl start sstc && find /var/backups -name 'sstc-*.tar.gz' -mtime +14 -delete
```

Because `sstc.seed.enabled=true` recreates the demo data on an empty database, a fresh server plus
the jar alone is always a working demo — the backup matters only once real content exists.

---

## 6. Health, monitoring and logs

| What | How |
| --- | --- |
| Liveness | `GET /api/health` → `{"status":"UP","time":"…"}` |
| Docker healthcheck | included in the compose file above |
| systemd | `Restart=always`, `systemctl status sstc`, `journalctl -u sstc -f` |
| Application log | `/opt/sstc/logs/app.log` (systemd) or `docker compose logs` |
| Resource watch | `systemctl status` shows memory; `docker stats` for containers |
| Access evidence | the platform's own audit screens: `/identity/audit`, `/operations` |

Useful probe for an uptime monitor:

```bash
curl -fsS http://127.0.0.1:8080/api/health | grep -q '"status":"UP"'
```

---

## 7. Post-deployment checklist

1. `curl -s localhost:8080/api/health` returns `UP`.
2. The dashboard loads at the domain and the sign-in page lists the demo accounts.
3. Sign in as `lead` / `Lead@123`, open **Identity & access → Accounts & roles**, confirm the six
   accounts and the permission counts.
4. Open **Architecture map** and check that the backend status shows `UP`.
5. `GET /swagger-ui.html` renders the API documentation.
6. The H2 console is **not** reachable from outside.
7. Upload a file in **Toolbox → Project files** (checks the reverse proxy body size limit).
8. A scheduled snapshot appears in **OKR & performance → Indicator trend** after the first day, or
   press **Capture snapshot now**.
9. Change the demo passwords, or create your own accounts and deactivate the demo ones.
