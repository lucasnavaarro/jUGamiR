"""Fail CI if Maven silently skips the disposable PostgreSQL regressions."""
import sys
from pathlib import Path
import xml.etree.ElementTree as ET

reports = list(Path(sys.argv[1]).glob("TEST-*.xml"))
totals = dict.fromkeys(("tests", "failures", "errors", "skipped"), 0)
required = {"SharedImagesPolicyTest", "DeadlineBoundaryTest", "AcceptanceCorrectionsTest",
            "FunctionalRegressionTest", "HttpFunctionalRegressionTest", "ImageAcceptanceBoundaryTest"}
seen = set()
for report in reports:
    suite = ET.parse(report).getroot()
    seen.add(suite.attrib.get("name", "").rsplit(".", 1)[-1])
    for key in totals:
        totals[key] += int(suite.attrib.get(key, 0))
print(totals)
if not required.issubset(seen) or not totals["tests"] or any(totals[k] for k in ("failures", "errors", "skipped")):
    raise SystemExit("Missing, failed or skipped regression tests: " + ", ".join(sorted(required - seen)))
