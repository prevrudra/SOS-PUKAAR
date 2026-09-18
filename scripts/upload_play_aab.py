#!/usr/bin/env python3
"""Upload a signed AAB to Google Play (draft release on chosen track)."""
from __future__ import annotations

import argparse
import json
import mimetypes
import sys
import time
import base64
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SA_PATH = ROOT / "deploy" / "secrets" / "play-publisher.json"


def b64url(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode()


def access_token() -> str:
    from cryptography.hazmat.primitives import hashes, serialization
    from cryptography.hazmat.primitives.asymmetric import padding

    sa = json.loads(SA_PATH.read_text())
    header = b64url(json.dumps({"alg": "RS256", "typ": "JWT"}).encode())
    now = int(time.time())
    claim = {
        "iss": sa["client_email"],
        "sub": sa["client_email"],
        "aud": "https://oauth2.googleapis.com/token",
        "iat": now,
        "exp": now + 3600,
        "scope": "https://www.googleapis.com/auth/androidpublisher",
    }
    payload = b64url(json.dumps(claim).encode())
    signing_input = f"{header}.{payload}".encode()
    key = serialization.load_pem_private_key(sa["private_key"].encode(), password=None)
    sig = key.sign(signing_input, padding.PKCS1v15(), hashes.SHA256())
    jwt = f"{header}.{payload}.{b64url(sig)}"
    body = urllib.parse.urlencode(
        {"grant_type": "urn:ietf:params:oauth:grant-type:jwt-bearer", "assertion": jwt}
    ).encode()
    req = urllib.request.Request("https://oauth2.googleapis.com/token", data=body, method="POST")
    with urllib.request.urlopen(req, timeout=60) as r:
        return json.loads(r.read())["access_token"]


def api(token: str, method: str, url: str, data: bytes | None = None, headers: dict | None = None):
    h = {"Authorization": f"Bearer {token}"}
    if headers:
        h.update(headers)
    req = urllib.request.Request(url, data=data, method=method, headers=h)
    try:
        with urllib.request.urlopen(req, timeout=300) as r:
            raw = r.read()
            return json.loads(raw) if raw else {}
    except urllib.error.HTTPError as e:
        err = e.read().decode("utf-8", errors="replace")
        raise SystemExit(f"{method} {url}\nHTTP {e.code}: {err}") from e


def upload_aab(package: str, aab: Path, track: str, notes: str, status: str) -> None:
    if not aab.is_file():
        raise SystemExit(f"AAB not found: {aab}")
    token = access_token()
    base = f"https://androidpublisher.googleapis.com/androidpublisher/v3/applications/{package}"
    edit = api(token, "POST", f"{base}/edits", data=b"{}", headers={"Content-Type": "application/json"})
    edit_id = edit["id"]
    print(f"Edit {edit_id}")

    # Upload bundle via media upload
    upload_url = (
        f"https://androidpublisher.googleapis.com/upload/androidpublisher/v3/"
        f"applications/{package}/edits/{edit_id}/bundles?uploadType=media"
    )
    aab_bytes = aab.read_bytes()
    print(f"Uploading {aab.name} ({len(aab_bytes)} bytes)…")
    bundle = api(
        token,
        "POST",
        upload_url,
        data=aab_bytes,
        headers={"Content-Type": "application/octet-stream"},
    )
    version_code = bundle.get("versionCode")
    print(f"Uploaded versionCode={version_code}")

    track_body = {
        "track": track,
        "releases": [
            {
                "name": f"{aab.stem} ({version_code})",
                "versionCodes": [str(version_code)],
                "status": status,
                "releaseNotes": [{"language": "en-GB", "text": notes}],
            }
        ],
    }
    api(
        token,
        "PUT",
        f"{base}/edits/{edit_id}/tracks/{track}",
        data=json.dumps(track_body).encode(),
        headers={"Content-Type": "application/json"},
    )
    commit = api(token, "POST", f"{base}/edits/{edit_id}:commit")
    print(f"Committed edit {commit.get('id', edit_id)} → track={track} status={status}")
    print(f"https://play.google.com/console/developers → {package}")


def main() -> None:
    p = argparse.ArgumentParser()
    p.add_argument("--package", required=True)
    p.add_argument("--aab", required=True, type=Path)
    p.add_argument("--track", default="internal", choices=["internal", "alpha", "beta", "production"])
    p.add_argument("--status", default="draft", choices=["draft", "completed", "halted"])
    p.add_argument(
        "--notes",
        default="Reliability update: offline SOS SMS fallback, FCM data-only wake, delivery retries, High Alert alarm-clock ringing.",
    )
    args = p.parse_args()
    upload_aab(args.package, args.aab, args.track, args.notes, args.status)


if __name__ == "__main__":
    main()
