#! /bin/sh

WORD_DIR=$(cd $(dirname $0); pwd)
SERVICE_NAME="wvp"

# Check if you are the root user
if [ "$(id -u)" -ne 0 ]; then
  echo "Tip: It is recommended to use the root user to execute this script, otherwise the permissions may be insufficient.！"
  read -p "continue？(y/n) " -n 1 -r
  if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    exit 1
  fi
  echo
fi

# Search directly in the current directory (excluding subdirectories)）
jar_files=(*.jar)

if [ ${#jar_files[@]} -eq 0 ]; then
  echo "There is no JAR file in the current directory！"
  exit 1
fi

# Traverse results
for jar in "${jar_files[@]}"; do
  echo "Find the JAR file: $jar"
done

# write file
# Generate Systemd service file contents
SERVICE_FILE="/etc/systemd/system/${SERVICE_NAME}.service"
cat << EOF | sudo tee "$SERVICE_FILE" > /dev/null
[Unit]
Description=${SERVICE_NAME}
After=syslog.target

[Service]
User=$USER
WorkingDirectory=${WORD_DIR}
ExecStart=java -jar ${jar_files}
SuccessExitStatus=143
Restart=on-failure
RestartSec=10s
Environment=SPRING_PROFILES_ACTIVE=prod

[Install]
WantedBy=multi-user.target
EOF

# Reload Systemd and start the service
sudo systemctl daemon-reload
sudo systemctl enable "$SERVICE_NAME"
sudo systemctl start "$SERVICE_NAME"

# Verify service status
echo "The service is installed! Execute the following command to check the status:"
echo "sudo systemctl status $SERVICE_NAME"
