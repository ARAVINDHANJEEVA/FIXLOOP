"""
FixLoop mDNS Publisher — start-mdns.bat calls this script.

Registers the laptop as 'fixloop.local' on the local network via Zeroconf (mDNS/Bonjour).
The Android app (in mDNS mode) resolves this hostname to the laptop's current IP automatically.

This means:
  - No need to enter the laptop's IP in the app.
  - Survives DHCP IP changes (router reboot, reconnect to hotspot, etc.).

Requirements:
    pip install zeroconf

Usage (run from project root):
    python mdns_publisher.py
    -- or --
    start-mdns.bat
"""

import socket
import time
import sys
import logging

try:
    from zeroconf import Zeroconf, ServiceInfo
except ImportError:
    print("\n[ERROR] 'zeroconf' library not installed.")
    print("  Run:  pip install zeroconf\n")
    sys.exit(1)

logging.basicConfig(level=logging.WARNING)

SERVICE_NAME = "fixloop"
SERVICE_TYPE = "_http._tcp.local."
PORT = 8080
HOSTNAME = "fixloop.local."


def get_local_ip() -> str:
    """Get the laptop's IP on the active network (hotspot / Wi-Fi)."""
    try:
        # Connect to a remote address (doesn't actually send data) to find the
        # outbound interface IP — this picks the hotspot network IP automatically.
        with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s:
            s.connect(("8.8.8.8", 80))
            ip = s.getsockname()[0]
        return ip
    except Exception:
        return socket.gethostbyname(socket.gethostname())


def main():
    local_ip = get_local_ip()
    ip_bytes = socket.inet_aton(local_ip)

    print("=" * 55)
    print("  FixLoop mDNS Publisher")
    print("=" * 55)
    print(f"  Laptop IP  : {local_ip}")
    print(f"  Port       : {PORT}")
    print(f"  mDNS name  : fixloop.local")
    print(f"  App URL    : http://fixloop.local:{PORT}/")
    print("=" * 55)
    print()
    print("  The Android app (mDNS mode) will find this laptop")
    print("  automatically — even if the IP changes.")
    print()
    print("  Keep this window open while the app is running.")
    print("  Press Ctrl+C to stop.\n")

    info = ServiceInfo(
        type_=SERVICE_TYPE,
        name=f"{SERVICE_NAME}.{SERVICE_TYPE}",
        addresses=[ip_bytes],
        port=PORT,
        properties={
            "path": "/",
            "description": "FixLoop Spring Boot Backend",
        },
        server=HOSTNAME,
    )

    zeroconf = Zeroconf()
    try:
        zeroconf.register_service(info)
        print(f"  [OK] Registered  fixloop.local  →  {local_ip}:{PORT}")
        print()
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        print("\n  [STOP] Unregistering service...")
    finally:
        zeroconf.unregister_service(info)
        zeroconf.close()
        print("  [DONE] mDNS publisher stopped.\n")


if __name__ == "__main__":
    main()
