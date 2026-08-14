# WVP-GB28181-PRO Demo Run Guide

This guide explains how to build, run, and test the translated WVP (Web Video Platform) project, including both the backend and frontend web interface.

---

## 🛠️ Prerequisites

Before running the project, make sure you have the following installed:

*   **Node.js** (v18+ recommended) and **npm** (v9+ recommended) — to build the frontend.
*   **Java 21 JDK** — to build and run the backend.
*   **Docker & Docker Compose** (highly recommended) — to run the full stack with all database and media server dependencies in a single command.

---

## 🚀 Running the Demo

A unified script `run.sh` has been created in the root directory. To run it, make sure it is executable:

```bash
chmod +x run.sh
./run.sh
```

The script will check your environment dependencies and prompt you to choose one of two execution modes:

### Mode 1: Run with Docker Compose (Recommended)

This mode runs the entire stack inside Docker containers. It automatically sets up and configures:
1.  **MySQL Database**: Automatically initialized with the schema and seed data.
2.  **Redis Cache**: Used for real-time status and message queues.
3.  **ZLMediaKit**: The high-performance streaming media server.
4.  **WVP Service**: The Spring Boot backend.
5.  **Nginx**: Web server that hosts the compiled frontend and proxies API/stream requests.

**Steps:**
1.  Run `./run.sh` and select **Option 1**.
2.  The script will trigger `docker compose up -d --build` to compile the code and launch the services.
3.  Once completed, access the web interface at: **[http://localhost:8080](http://localhost:8080)**.

### Mode 2: Run Locally (Native Development)

This mode builds the frontend assets locally, compiles the Java backend, and runs it on your host system.

> [!IMPORTANT]
> This mode assumes you have local instances of Redis, MySQL, and ZLMediaKit already running on your host machine.

**Steps:**
1.  Ensure your local Redis, MySQL, and ZLMediaKit are running.
2.  Configure your databases and media server IPs in `src/main/resources/application-dev.yml`.
3.  Run `./run.sh` and select **Option 2**.
4.  The script will:
    *   Navigate to the `web/` folder, run `npm install`, and build the frontend (`npm run build:prod`).
    *   Output the compiled static assets directly into the backend folder (`src/main/resources/static`).
    *   Compile the backend Java project with Maven (`mvn clean package`).
    *   Launch the Spring Boot backend jar.
5.  Once the backend starts, access the interface at your configured HTTP port (default is **[http://localhost:18080](http://localhost:18080)**).

---

## 🔐 Credentials & Initial Configuration

*   **Default Web Console URL**: [http://localhost:8080](http://localhost:8080) (if running via Docker Compose)
*   **Default Credentials**:
    *   **Username**: `admin`
    *   **Password**: `admin`
*   Upon logging in, you will be greeted by the English Web Console, where you can configure media server nodes, add GB28181 camera channels, start live previews, or view server logs.
