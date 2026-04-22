#!/usr/bin/env python3
import sys
study_id = ""
for i, arg in enumerate(sys.argv):
    if arg == "--study-id" and i + 1 < len(sys.argv):
        study_id = sys.argv[i + 1]

var = "MOCK_VALIDATE_EXIT_" + study_id.upper()
import os
sys.exit(int(os.environ.get(var, os.environ.get("MOCK_VALIDATE_EXIT", "0"))))
