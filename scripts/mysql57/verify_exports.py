"""Verify both export modes and historical migrations in NEW isolated databases.

Uses the mysql CLI. Credentials belong in MYSQL_PWD or the client's local
configuration. MYSQL_COMMAND_JSON may provide a docker-exec command array in CI.
Refuses to reuse an existing database and only cleans databases it created.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import unicodedata
from prepare_legacy import render

ROOT = Path(__file__).resolve().parents[2]
EXPORTS = ROOT / "campus-share-backend/docs/datebase/mysql57"
TABLES = {"app_user", "resource", "resource_opening", "resource_block", "reservation", "reservation_event", "violation_record", "resource_schedule_event", "resource_catalog_guard"}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--mysql", default="mysql")
    parser.add_argument("--user", default="root")
    parser.add_argument("--prefix", default="campusshare_exports_check")
    parser.add_argument("--report", type=Path)
    args = parser.parse_args()
    if not re.fullmatch(r"[a-z][a-z0-9_]{0,47}", args.prefix):
        raise ValueError("Invalid isolated-database prefix")
    command = json.loads(os.environ.get("MYSQL_COMMAND_JSON", json.dumps([args.mysql])))
    created = []
    report = {"databases": {}, "checks": []}

    def sql(content, database=None):
        call = command + ["--user=" + args.user, "--default-character-set=utf8mb4", "--batch", "--skip-column-names"]
        if database:
            call += ["--database=" + database]
        result = subprocess.run(call, input=("SET NAMES utf8mb4; SET time_zone='+08:00';\n" + content).encode(), capture_output=True)
        if result.returncode:
            raise RuntimeError(result.stderr.decode("utf-8", "replace"))
        return result.stdout.decode("utf-8").strip()

    def counts(database):
        return {table: int(sql(f"SELECT COUNT(*) FROM `{table}`", database)) for table in sorted(TABLES)}

    def columns(database):
        return sql("SELECT TABLE_NAME,COLUMN_NAME,COLUMN_TYPE,IS_NULLABLE,COALESCE(COLUMN_DEFAULT,'<NULL>'),EXTRA FROM information_schema.COLUMNS "
                   f"WHERE TABLE_SCHEMA='{database}' ORDER BY TABLE_NAME,ORDINAL_POSITION")

    def normalized(value):
        return re.sub(r"\s+", " ", unicodedata.normalize("NFKC", value)).strip().lower()

    try:
        report["serverVersion"] = sql("SELECT VERSION()")
        assert report["serverVersion"].startswith("5.7."), report["serverVersion"]
        manifest = json.loads((EXPORTS / "manifest.json").read_text(encoding="utf-8"))
        for name, digest in manifest["sha256Lf"].items():
            assert hashlib.sha256((EXPORTS / name).read_bytes().replace(b"\r\n", b"\n")).hexdigest() == digest, name
        for mode in ("fresh", "snapshot", "legacy"):
            database = args.prefix + "_" + mode
            if sql(f"SELECT COUNT(*) FROM information_schema.SCHEMATA WHERE SCHEMA_NAME='{database}'") != "0":
                raise RuntimeError("Refusing to overwrite existing database: " + database)
            sql(f"CREATE DATABASE `{database}` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
            created.append(database)
            if mode == "legacy":
                for _, content in render(database):
                    sql(content)
            else:
                sql((EXPORTS / "001_schema.sql").read_text(encoding="utf-8"), database)
                data = "002_demo_data.sql" if mode == "fresh" else "snapshot_2026-11-17.sql"
                sql((EXPORTS / data).read_text(encoding="utf-8"), database)
            assert set(sql("SHOW TABLES", database).splitlines()) == TABLES
            assert sql(f"SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='{database}' AND ENGINE='InnoDB'") == "9"
            assert sql("SELECT COUNT(*) FROM resource_catalog_guard WHERE id=1", database) == "1"
            report["databases"][mode] = {"rowCounts": counts(database)}
            foreign_keys = sql("SELECT TABLE_NAME,COLUMN_NAME,REFERENCED_TABLE_NAME,REFERENCED_COLUMN_NAME FROM information_schema.KEY_COLUMN_USAGE "
                               f"WHERE TABLE_SCHEMA='{database}' AND REFERENCED_TABLE_NAME IS NOT NULL")
            for line in foreign_keys.splitlines():
                child, column, parent, primary = line.split("\t")
                assert sql(f"SELECT COUNT(*) FROM `{child}` c LEFT JOIN `{parent}` p ON c.`{column}`=p.`{primary}` WHERE c.`{column}` IS NOT NULL AND p.`{primary}` IS NULL", database) == "0", line
            if mode == "snapshot":
                assert counts(database) == manifest["snapshotRowCounts"]
            if mode == "fresh":
                current = counts(database)
                assert current["resource"] == manifest["cleanDemo"]["resources"]
                assert current["resource_opening"] == manifest["cleanDemo"]["openings"]
                assert current["resource_block"] == sum(manifest["cleanDemo"]["blocksByType"].values())
                assert current["reservation"] == current["reservation_event"] == current["violation_record"] == current["resource_schedule_event"] == 0
                assert sql("SELECT COUNT(*) FROM resource WHERE resource_type='CLASSROOM'", database) == "7"
                identities = [tuple(normalized(x) for x in line.split("\t")) for line in sql("SELECT location,resource_type,unit_label FROM resource", database).splitlines()]
                assert len(set(identities)) == len(identities)
                assert sql("SELECT COUNT(DISTINCT DATE(start_at)) FROM resource_opening", database) == "7"
                assert sql("SELECT COUNT(*) FROM app_user WHERE account IN ('student01','student02','teacher01','admin01')", database) == "4"
        assert columns(created[0]) == columns(created[2]), "Fresh final schema differs from migrated historical schema"
        report["checks"] = ["LF checksums", "fresh import", "full snapshot row counts", "all tables InnoDB", "foreign-key integrity", "clean demo identity uniqueness", "7 classrooms and rolling 7-day schedules", "no stale bookings in fresh mode", "001-008 legacy compatibility", "final schema matches historical migrations"]
        report["status"] = "passed"
    finally:
        # Only names registered immediately after this run created them can be removed.
        for database in reversed(created):
            assert database.startswith(args.prefix + "_") and database != "campusshare"
            sql(f"DROP DATABASE `{database}`")
        report["isolatedDatabasesCleaned"] = len(created)
        if args.report:
            args.report.parent.mkdir(parents=True, exist_ok=True)
            args.report.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, ensure_ascii=True))


if __name__ == "__main__":
    main()
