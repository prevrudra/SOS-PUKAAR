#!/usr/bin/env python3
"""Upload High Alert Play Store listing + graphics."""
from __future__ import annotations

import json
import time
import base64
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SA_PATH = ROOT / "deploy" / "secrets" / "play-publisher.json"
ASSETS = ROOT / "release" / "play-highalert"
PACKAGE = "com.pukaar.highalert"
LANG = "en-GB"


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
    key = serialization.load_pem_private_key(sa["private_key"].encode(), password=None)
    sig = key.sign(f"{header}.{payload}".encode(), padding.PKCS1v15(), hashes.SHA256())
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


def upload_image(token: str, base: str, edit_id: str, image_type: str, path: Path):
    url = (
        f"https://androidpublisher.googleapis.com/upload/androidpublisher/v3/"
        f"applications/{PACKAGE}/edits/{edit_id}/listings/{LANG}/{image_type}"
        f"?uploadType=media"
    )
    print(f"  upload {image_type}: {path.name} ({path.stat().st_size} bytes)")
    return api(
        token,
        "POST",
        url,
        data=path.read_bytes(),
        headers={"Content-Type": "image/png"},
    )


def main() -> None:
    title = (ASSETS / "title.txt").read_text().strip()
    short = (ASSETS / "short-description.txt").read_text().strip()
    full = (ASSETS / "full-description.txt").read_text().strip()
    # Play limits
    if len(short) > 80:
        short = short[:80]
    if len(title) > 50:
        title = title[:50]
    if len(full) > 4000:
        full = full[:4000]

    token = access_token()
    base = f"https://androidpublisher.googleapis.com/androidpublisher/v3/applications/{PACKAGE}"
    edit = api(token, "POST", f"{base}/edits", b"{}", {"Content-Type": "application/json"})
    eid = edit["id"]
    print(f"Edit {eid}")

    listing = {
        "language": LANG,
        "title": title,
        "shortDescription": short,
        "fullDescription": full,
    }
    api(
        token,
        "PUT",
        f"{base}/edits/{eid}/listings/{LANG}",
        data=json.dumps(listing).encode(),
        headers={"Content-Type": "application/json"},
    )
    print("Listing text updated")

    # Clear existing images of each type then upload fresh
    for image_type in ("icon", "featureGraphic", "phoneScreenshots"):
        try:
            api(token, "DELETE", f"{base}/edits/{eid}/listings/{LANG}/{image_type}")
            print(f"  cleared {image_type}")
        except SystemExit as e:
            if "404" not in str(e):
                print(f"  clear {image_type}: {e}")

    upload_image(token, base, eid, "icon", ASSETS / "icon-512.png")
    upload_image(token, base, eid, "featureGraphic", ASSETS / "feature-graphic.png")
    for shot in ("phone-1.png", "phone-2.png", "phone-3.png"):
        upload_image(token, base, eid, "phoneScreenshots", ASSETS / shot)

    # Contact / privacy if endpoints exist
    try:
        details = {
            "contactEmail": "support@pukaaralert.com",
            "contactWebsite": "https://pukaaralert.com",
            "defaultLanguage": LANG,
        }
        api(
            token,
            "PUT",
            f"{base}/edits/{eid}/details",
            data=json.dumps(details).encode(),
            headers={"Content-Type": "application/json"},
        )
        print("App details updated")
    except SystemExit as e:
        print(f"details skip: {e}")

    commit = api(token, "POST", f"{base}/edits/{eid}:commit")
    print(f"Committed {commit.get('id', eid)}")
    print(f"https://play.google.com/console/developers → {PACKAGE} → Store listing")


if __name__ == "__main__":
    main()
