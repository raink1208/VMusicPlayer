#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
PostgreSQL ingest tool for music_sources / songs / singers
- public_id is ULID
- songs.start_at / end_at in JSON (h:mm:ss / m:ss) -> stored as INTERVAL seconds
- Idempotent-ish: reuses existing rows by natural keys (url / name / (source_id,title,start_at))
"""

import argparse
import json
import re
from dateutil import parser as dtparser

import psycopg
from psycopg.rows import dict_row
import ulid

TIME_RE = re.compile(r"^(?:(\d+):)?(\d{1,2}):(\d{2})$")  # h:mm:ss or m:ss (capturing h optional)

def parse_hms_to_seconds(text: str) -> int:
    """Accepts 'h:mm:ss', 'm:ss', or 'ss' and returns total seconds (int)."""
    t = text.strip()
    m = TIME_RE.match(t)
    if m:
        h = int(m.group(1)) if m.group(1) is not None else 0
        mnt = int(m.group(2))
        sec = int(m.group(3))
        return h * 3600 + mnt * 60 + sec
    if t.isdigit():
        return int(t)
    try:
        dt = dtparser.parse(t)
        return dt.hour * 3600 + dt.minute * 60 + dt.second
    except Exception:
        raise ValueError(f"Unsupported time literal: {text}")

def interval_seconds_literal(seconds: int) -> str:
    """Return a SQL-safe interval text like '123 seconds'."""
    return f"{int(seconds)} seconds"

def get_or_create_source_type(conn, type_name: str) -> int:
    with conn.cursor(row_factory=dict_row) as cur:
        cur.execute("SELECT id FROM music_source_types WHERE name = %s", (type_name,))
        row = cur.fetchone()
        if row:
            return row["id"]
        cur.execute(
            "INSERT INTO music_source_types (name) VALUES (%s) RETURNING id",
            (type_name,),
        )
        return cur.fetchone()["id"]

def get_or_create_singer(conn, name: str) -> int:
    with conn.cursor(row_factory=dict_row) as cur:
        cur.execute("SELECT id FROM singers WHERE name = %s", (name,))
        row = cur.fetchone()
        if row:
            return row["id"]
        pid = str(ulid.new())
        cur.execute(
            "INSERT INTO singers (public_id, name) VALUES (%s, %s) RETURNING id",
            (pid, name),
        )
        return cur.fetchone()["id"]

def get_or_create_music_source(conn, title: str, url: str, upload_date_str: str,
                               type_id: int, thumbnail_url: str | None) -> int:
    with conn.cursor(row_factory=dict_row) as cur:
        cur.execute("SELECT id FROM music_sources WHERE url = %s", (url,))
        row = cur.fetchone()
        if row:
            return row["id"]
        pid = str(ulid.new())
        upload_date = dtparser.parse((upload_date_str or "").strip()).date()
        cur.execute(
            """
            INSERT INTO music_sources
                (public_id, title, url, upload_date, type_id, thumbnail_url)
            VALUES
                (%s, %s, %s, %s, %s, %s)
            RETURNING id
            """,
            (pid, title, url, upload_date, type_id, thumbnail_url),
        )
        return cur.fetchone()["id"]

def get_or_create_song(conn, source_id: int, title: str,
                       start_seconds: int, end_seconds: int) -> int:
    with conn.cursor(row_factory=dict_row) as cur:
        cur.execute(
            """
            SELECT id FROM songs
            WHERE source_id = %s
              AND title = %s
              AND start_at = %s::interval
            """,
            (source_id, title, interval_seconds_literal(start_seconds)),
        )
        row = cur.fetchone()
        if row:
            cur.execute(
                """
                UPDATE songs
                   SET end_at = %s::interval,
                       updated_at = CURRENT_TIMESTAMP
                 WHERE id = %s
                """,
                (interval_seconds_literal(end_seconds), row["id"]),
            )
            return row["id"]

        pid = str(ulid.new())
        cur.execute(
            """
            INSERT INTO songs
                (public_id, source_id, title, start_at, end_at)
            VALUES
                (%s, %s, %s, %s::interval, %s::interval)
            RETURNING id
            """,
            (
                pid, source_id, title,
                interval_seconds_literal(start_seconds),
                interval_seconds_literal(end_seconds),
            ),
        )
        return cur.fetchone()["id"]

def ensure_song_singer_link(conn, song_id: int, singer_id: int):
    with conn.cursor() as cur:
        cur.execute(
            """
            INSERT INTO song_singers (song_id, singer_id)
            VALUES (%s, %s)
            ON CONFLICT (song_id, singer_id) DO NOTHING
            """,
            (song_id, singer_id),
        )

def ingest(json_path: str,
           host: str, port: int, user: str, password: str, dbname: str, sslmode: str | None):
    dsn_parts = [f"host={host}", f"port={port}", f"user={user}", f"password={password}", f"dbname={dbname}"]
    if sslmode:
        dsn_parts.append(f"sslmode={sslmode}")
    dsn = " ".join(dsn_parts)

    with psycopg.connect(dsn, autocommit=True) as conn:
        with open(json_path, "r", encoding="utf-8") as f:
            data = json.load(f)

        with conn.transaction():
            for entry in data:
                title = (entry.get("title") or "").strip()
                url = (entry.get("url") or "").strip()
                upload_date = (entry.get("upload_date") or "").strip()
                source_type_name = (entry.get("sourceType") or "live").strip()
                thumb = entry.get("thumbnail_url")

                type_id = get_or_create_source_type(conn, source_type_name)
                source_id = get_or_create_music_source(conn, title, url, upload_date, type_id, thumb)

                songs = entry.get("songs") or []
                for s in songs:
                    song_title = (s.get("title") or "").strip()
                    start_at = (s.get("start_at") or "").strip()
                    end_at = (s.get("end_at") or "").strip()
                    if not song_title or not start_at or not end_at:
                        continue

                    start_sec = parse_hms_to_seconds(start_at)
                    end_sec = parse_hms_to_seconds(end_at)
                    if end_sec <= start_sec:
                        end_sec = start_sec + 1  # satisfy CHECK (end_at > start_at)

                    song_id = get_or_create_song(conn, source_id, song_title, start_sec, end_sec)

                    for singer_name in (s.get("singers") or []):
                        if not singer_name:
                            continue
                        singer_id = get_or_create_singer(conn, singer_name.strip())
                        ensure_song_singer_link(conn, song_id, singer_id)

    print("Ingest completed successfully.")

def main():
    ap = argparse.ArgumentParser(description="Ingest music JSON into PostgreSQL (no schema step)")
    ap.add_argument("--host", default="localhost")
    ap.add_argument("--port", type=int, default=5432)
    ap.add_argument("--user", default="VMusicPlayer")
    ap.add_argument("--password", default="VMusicPlayer")
    ap.add_argument("--dbname", default="VMusicPlayer")
    ap.add_argument("--sslmode", default=None, help="disable, require, verify-ca, verify-full, etc.")
    ap.add_argument("--json", require=True, help="Path to input JSON file")
    args = ap.parse_args()

    ingest(
        json_path=args.json,
        host=args.host, port=args.port,
        user=args.user, password=args.password, dbname=args.dbname,
        sslmode=args.sslmode,
    )

if __name__ == "__main__":
    main()
