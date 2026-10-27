#!/usr/bin/env python3
"""Minimal zip-align for APKs: pad local headers so uncompressed entries
(especially resources.arsc) start on a 4-byte boundary.

Android 11+ requires resources.arsc to be stored uncompressed and 4-byte aligned
for apps targeting API 30+. This script rebuilds the zip without touching data.
"""
import struct
import sys
import zipfile

ALIGNMENT = 4
EXTRA_ID = 0xD935  # Android ZIP alignment extra field


def alignment_extra(offset, name_len):
    """Return a 4..7 byte extra field so the data offset is 4-byte aligned."""
    for total in range(4, 4 + ALIGNMENT):
        if (offset + 30 + name_len + total) % ALIGNMENT == 0:
            payload = total - 4
            return struct.pack("<HH", EXTRA_ID, payload) + (b"\x00" * payload)
    raise RuntimeError("cannot align entry")


def main(src, dst):
    with zipfile.ZipFile(src, "r") as zin, zipfile.ZipFile(dst, "w", allowZip64=True) as zout:
        for info in zin.infolist():
            if info.is_dir():
                continue
            data = zin.read(info.filename)
            out = zipfile.ZipInfo(info.filename, info.date_time)
            out.compress_type = info.compress_type
            out.external_attr = info.external_attr
            out.internal_attr = info.internal_attr
            out.create_system = info.create_system
            out.comment = info.comment

            if info.compress_type == zipfile.ZIP_STORED:
                offset = zout.fp.tell()
                name_len = len(info.filename.encode("utf-8"))
                out.extra = alignment_extra(offset, name_len)
            zout.writestr(out, data)

    # verify
    bad = 0
    with zipfile.ZipFile(dst) as z:
        for info in z.infolist():
            if info.compress_type != zipfile.ZIP_STORED:
                continue
            data_off = info.header_offset + 30 + len(info.filename.encode("utf-8")) + len(info.extra)
            aligned = data_off % ALIGNMENT == 0
            print("   %-32s offset=%-7d aligned=%s" % (info.filename, data_off, aligned))
            if not aligned:
                bad += 1
    if bad:
        raise SystemExit("alignment failed for %d entries" % bad)


if __name__ == "__main__":
    if len(sys.argv) != 3:
        raise SystemExit("usage: align_apk.py <input.apk> <output.apk>")
    main(sys.argv[1], sys.argv[2])
