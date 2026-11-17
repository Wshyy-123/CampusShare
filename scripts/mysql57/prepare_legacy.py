"""Render historical 001-008 migrations for a fresh MySQL 5.7 database.

Original migrations remain unchanged. New installations should use the final
schema and demo/snapshot files under campus-share-backend/docs/datebase/mysql57.
"""
import argparse
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "campus-share-backend/docs/datebase"


def render(database: str):
    if not re.fullmatch(r"[A-Za-z][A-Za-z0-9_]{0,63}", database):
        raise ValueError("Invalid database identifier")
    for path in sorted(SOURCE.glob("00*.sql")):
        sql = path.read_text(encoding="utf-8")
        sql = sql.replace("utf8mb4_0900_ai_ci", "utf8mb4_unicode_ci")
        sql = sql.replace("CREATE DATABASE IF NOT EXISTS campusshare", f"CREATE DATABASE IF NOT EXISTS `{database}`")
        sql = re.sub(r"(?mi)^USE\s+campusshare\s*;", f"USE `{database}`;", sql)
        yield path.name, "SET NAMES utf8mb4; SET time_zone='+08:00'; SET SESSION default_storage_engine='InnoDB';\n" + sql


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--database", default="campusshare")
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    for name, content in render(args.database):
        (args.output / name).write_text(content, encoding="utf-8")
    print("Rendered historical migration copies; original files unchanged")
