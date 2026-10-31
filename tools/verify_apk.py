#!/usr/bin/env python3
"""Check a built APK without installing it.

Verifies the application ID, the packaged ABIs, and 16 KB ELF segment
alignment, which Android 15+ devices with 16 KB page sizes require.

Usage:
    python3 tools/verify_apk.py APK --package com.prompter.app \
        --abis arm64-v8a,x86_64
"""

from __future__ import annotations

import argparse
import glob
import os
import struct
import subprocess
import sys
import zipfile


def find_aapt2() -> str:
    explicit = os.environ.get("AAPT2")
    if explicit:
        return explicit
    android_home = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    if not android_home:
        raise SystemExit("ANDROID_HOME is not set and AAPT2 was not provided")
    candidates = sorted(glob.glob(os.path.join(android_home, "build-tools", "*", "aapt2")))
    if not candidates:
        raise SystemExit(f"aapt2 not found under {android_home}/build-tools")
    return candidates[-1]


def read_badging(apk: str) -> str:
    return subprocess.check_output([find_aapt2(), "dump", "badging", apk], text=True)


def package_metadata(badging: str) -> dict[str, str]:
    line = next(x for x in badging.splitlines() if x.startswith("package:"))
    # package: name='com.example' versionCode='1' versionName='1.0'
    parts = line[len("package:") :].split(" ")
    return dict(p.split("=", 1) for p in parts if "=" in p)


def load_alignments(data: bytes) -> list[int]:
    if data[:5] != b"\x7fELF\x02":
        return []
    program_offset = struct.unpack_from("<Q", data, 32)[0]
    entry_size, entry_count = struct.unpack_from("<HH", data, 54)
    alignments = []
    for index in range(entry_count):
        base = program_offset + index * entry_size
        if struct.unpack_from("<I", data, base)[0] == 1:  # PT_LOAD
            alignments.append(struct.unpack_from("<Q", data, base + 48)[0])
    return alignments


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("apk")
    parser.add_argument("--package", required=True)
    parser.add_argument("--abis", default="arm64-v8a,x86_64")
    parser.add_argument("--min-page-size", type=int, default=16384)
    args = parser.parse_args()

    expected_abis = [a for a in args.abis.split(",") if a]
    badging = read_badging(args.apk)
    metadata = package_metadata(badging)
    package = metadata["name"].strip("'")
    if package != args.package:
        print(f"unexpected application ID: {package} != {args.package}", file=sys.stderr)
        return 1

    with zipfile.ZipFile(args.apk) as archive:
        names = archive.namelist()
        libs = [n for n in names if n.startswith("lib/") and n.endswith(".so")]
        packaged_abis = sorted({n.split("/")[1] for n in libs})
        if packaged_abis != sorted(expected_abis):
            print(f"unexpected ABIs: {packaged_abis} != {sorted(expected_abis)}", file=sys.stderr)
            return 1
        if not libs:
            print("no native libraries found", file=sys.stderr)
            return 1
        for name in libs:
            alignments = load_alignments(archive.read(name))
            if alignments and min(alignments) < args.min_page_size:
                print(f"{name} is not {args.min_page_size}-byte aligned: {alignments}", file=sys.stderr)
                return 1

    print(f"package: {package}")
    print(f"versionCode: {metadata.get('versionCode', '?').strip(chr(39))}")
    print(f"versionName: {metadata.get('versionName', '?').strip(chr(39))}")
    print(f"abis: {', '.join(packaged_abis)}")
    print(f"native libraries: {len(libs)} (16 KB aligned)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
