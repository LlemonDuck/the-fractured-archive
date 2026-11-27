#!/usr/bin/env python3
"""
Regenerates the list of autocast-capable weapon item ids used by StaffUtil.

Source: weirdgloop/osrs-dps-calc, cdn/json/equipment.json
Includes weapons categorised as "Staff" (staves and wands) or "Bladed Staff".
Powered staves ("Powered Staff"), "Polestaff" and "Powered Book" are excluded.

Output: a sorted, de-duplicated JSON array of item ids and nothing else.

Usage: python scripts/generate_staff_ids.py [path/to/equipment.json]
  With no argument, equipment.json is downloaded from GitHub.
"""
import json
import sys
import urllib.request
from pathlib import Path

SOURCE_URL = "https://raw.githubusercontent.com/weirdgloop/osrs-dps-calc/main/cdn/json/equipment.json"
OUTPUT = Path(__file__).resolve().parent.parent / "src/main/resources/com/duckblade/osrs/tfa/util/autocast-staves.json"

CATEGORIES = {"Staff", "Bladed Staff"}


def load_equipment(path):
	if path is not None:
		return json.loads(Path(path).read_text(encoding="utf-8"))
	with urllib.request.urlopen(SOURCE_URL) as response:
		return json.load(response)


def main():
	equipment = load_equipment(sys.argv[1] if len(sys.argv) > 1 else None)
	ids = sorted({
		item["id"]
		for item in equipment
		if item.get("slot") == "weapon" and item.get("category") in CATEGORIES
	})

	OUTPUT.parent.mkdir(parents=True, exist_ok=True)
	OUTPUT.write_text(json.dumps(ids) + "\n", encoding="utf-8")
	print(f"Wrote {len(ids)} ids to {OUTPUT}")


if __name__ == "__main__":
	main()
