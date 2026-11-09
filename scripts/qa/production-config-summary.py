"""Read Compose JSON on stdin; print an allowlist only, never credentials."""
import json
import sys
from pathlib import PurePosixPath

services = json.load(sys.stdin)["services"]
backend = services["backend"]
frontend = services["frontend"]
env = backend.get("environment", {})
print(json.dumps({
    "backend": {
        "production_profile": env.get("SPRING_PROFILES_ACTIVE") == "production",
        "public_origin_matches": env.get("APP_URL", "").rstrip("/") == "https://jugamir.es",
        "read_only": backend.get("read_only", False),
        "published_ports": len(backend.get("ports", [])),
        "tmpfs": backend.get("tmpfs", []),
        "server_port_8080": str(env.get("SERVER_PORT")) == "8080",
        "db_internal_port_5432": str(env.get("DB_PORT", "5432")) == "5432",
        "required_settings_present": {key: bool(env.get(key)) for key in ["DB_HOST", "DB_NAME", "DB_USER", "DB_PASSWORD", "JWT_SECRET", "MAIL_USERNAME", "MAIL_PASSWORD"]},
        "cookie_secure_override": env.get("APP_COOKIE_SECURE", "unset") if env.get("APP_COOKIE_SECURE") in (None, "true", "false") else "nonstandard",
    },
    "frontend": {
        "published_ports": [{key: port.get(key) for key in ["host_ip", "published", "target"]} for port in frontend.get("ports", [])],
        "mounts": [{"file": PurePosixPath(mount.get("source", "")).name, "target": mount.get("target"), "read_only": mount.get("read_only", False)} for mount in frontend.get("volumes", [])],
    },
    "database": {"image": services["db"].get("image"), "published_ports": len(services["db"].get("ports", []))},
}, indent=2))
