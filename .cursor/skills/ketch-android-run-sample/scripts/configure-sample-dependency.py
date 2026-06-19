#!/usr/bin/env python3
"""Switch sample apps between the local :ketchsdk project and a published Maven artifact."""

from __future__ import annotations

import os
import re
import sys
from pathlib import Path

GROUP = "com.ketch.android"
ARTIFACT = "ketchsdk"
DEFAULT_VERSION = "3.0"

SAMPLE_GRADLE_FILES = (
    "sample-app-compose/build.gradle",
    "sample-app-standard/build.gradle",
)

LOCAL_DEP = "implementation project(':ketchsdk')"
REMOTE_DEP_TEMPLATE = "implementation '{group}:{artifact}:{version}'"


def remote_dep(version: str) -> str:
    return REMOTE_DEP_TEMPLATE.format(group=GROUP, artifact=ARTIFACT, version=version)


def configure_file(path: Path, mode: str, version: str) -> bool:
    text = path.read_text(encoding="utf-8")
    target = LOCAL_DEP if mode == "local" else remote_dep(version)
    pattern = re.compile(
        r"implementation\s+(?:project\(':ketchsdk'\)|'"
        + re.escape(GROUP)
        + r":"
        + re.escape(ARTIFACT)
        + r":[^']+')"
    )
    if not pattern.search(text):
        raise SystemExit(f"Could not find ketchsdk dependency in {path}")

    new_text, count = pattern.subn(target, text, count=1)
    if count != 1:
        raise SystemExit(f"Expected one ketchsdk dependency in {path}, replaced {count}")

    if new_text == text:
        print(f"Already {mode} in {path.name}")
        return False

    path.write_text(new_text, encoding="utf-8")
    print(f"Updated {path.name} to {mode} ({target})")
    return True


def main() -> None:
    if len(sys.argv) != 3:
        raise SystemExit(f"Usage: {sys.argv[0]} <local|remote> <repo_root>")

    mode = sys.argv[1]
    if mode not in {"local", "remote"}:
        raise SystemExit("mode must be local or remote")

    repo_root = Path(sys.argv[2]).resolve()
    version = os.environ.get("KETCH_ANDROID_SDK_VERSION", DEFAULT_VERSION)

    changed = False
    for rel in SAMPLE_GRADLE_FILES:
        path = repo_root / rel
        if not path.is_file():
            raise SystemExit(f"Missing {path}")
        if configure_file(path, mode, version):
            changed = True

    if not changed:
        print(f"No changes needed (already {mode})")


if __name__ == "__main__":
    main()
