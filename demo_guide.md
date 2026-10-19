# WVP-GB28181-PRO Demo Run Guide

This guide explains how to build, run, and test the translated WVP (Web Video Platform) project, including both the backend and frontend web interface.

---

## 🛠️ Prerequisites

Before running the project, make sure you have the following installed:

*   **Node.js** (v18+ recommended) and **npm** or **yarn** — to build the frontend.
*   **Java 21 JDK** — to build and run the backend locally.
*   **Docker & Docker Compose** (highly recommended) — to run the stack in containerized mode.

---

## 🚀 Running the Demo

A unified script `run.sh` is available in the root directory:

```bash
chmod +x run.sh
./run.sh
```

The script will check your environment dependencies and prompt you to choose one of three execution modes:

### Mode 1: Run WVP Master ONLY (Recommended - Pure Control Plane)

This mode runs the pure WVP Control Plane (MySQL, Redis, WVP Spring Boot Backend, Nginx Web Admin) without a local ZLMediaKit. Media streaming is handled by external ZLMediaKit edge nodes (using the `zlm-node` worker package).

*   **Web Dashboard (Nginx)**: [http://localhost](http://localhost) (Port 80)
*   **WVP REST API**: [http://localhost:18978](http://localhost:18978)
*   **SIP Signaling (GB28181)**: 8116 UDP/TCP

### Mode 2: Run All-in-One Full Stack (Docker Compose)

This mode runs the entire stack inside Docker containers on a single host (MySQL, Redis, ZLMediaKit, WVP, Nginx).

*   **Web Dashboard**: [http://localhost](http://localhost) (Port 80)
*   **ZLMediaKit Media Ports**: 10935 (RTMP), 5540 (RTSP), 10000 (RTP single-port), 8000 (WebRTC), 30000-30500 (RTP multi-port range).

### Mode 3: Run Locally (Native Development)

This mode builds the frontend assets locally, compiles the Java backend, and runs it on your host machine against local Redis, MySQL, and ZLM.

1. Ensure local Redis, MySQL, and ZLMediaKit are running.
2. Configure credentials in `src/main/resources/application-dev.yml`.
3. Run `./run.sh` and select **Option 3**.

---

## 🔐 Credentials & Initial Configuration

*   **Default Web Console URL**: [http://localhost](http://localhost)
*   **Default Credentials**:
    *   **Username**: `admin`
    *   **Password**: `admin`
*   Upon logging in, you can configure media server nodes, add GB28181 camera channels, start live previews, or view server logs.
