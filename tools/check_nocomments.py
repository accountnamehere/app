import re
import sys
from pathlib import Path

PATTERNS = {
    ".kt": [r"^\s*//", r"/\*"],
    ".java": [r"^\s*//", r"/\*"],
    ".gradle": [r"^\s*//", r"/\*"],
    ".js": [r"^\s*//", r"/\*"],
    ".css": [r"/\*"],
    ".xml": [r"<!--"],
    ".html": [r"<!--"],
    ".py": [r"^\s*#(?!!)"],
    ".sh": [r"^\s*#(?!!)"],
}


def main(root):
    bad = 0
    for path in Path(root).rglob("*"):
        if not path.is_file() or path.suffix not in PATTERNS:
            continue
        if any(part in {"build", ".gradle", "node_modules"} for part in path.parts):
            continue
        for number, line in enumerate(path.read_text(errors="ignore").splitlines(), 1):
            if any(re.search(p, line) for p in PATTERNS[path.suffix]):
                print(f"{path}:{number}: {line.strip()[:80]}")
                bad += 1
    print(f"violations: {bad}")
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1] if len(sys.argv) > 1 else "."))
