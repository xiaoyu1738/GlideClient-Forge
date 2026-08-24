#!/usr/bin/env python3
"""Resolve the Linux client classpath from a legacy Minecraft version JSON."""

import argparse
import json
import os
import platform
import re
import sys


def rule_matches(rule):
    operating_system = rule.get("os", {})
    if operating_system.get("name") not in (None, "linux"):
        return False
    architecture = operating_system.get("arch")
    if architecture and architecture not in platform.machine():
        return False
    version_pattern = operating_system.get("version")
    if version_pattern and not re.search(version_pattern, platform.release()):
        return False
    return not rule.get("features")


def is_allowed(library):
    rules = library.get("rules")
    if not rules:
        return True
    allowed = False
    for rule in rules:
        if rule_matches(rule):
            allowed = rule.get("action") == "allow"
    return allowed


def coordinate_path(name):
    parts = name.split(":")
    if len(parts) != 3:
        return None
    group, artifact, version = parts
    return os.path.join(*group.split("."), artifact, version,
                        "{}-{}.jar".format(artifact, version))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--version-json", required=True)
    parser.add_argument("--libraries", required=True)
    parser.add_argument("--client-jar", required=True)
    parser.add_argument("--extra", action="append", default=[])
    arguments = parser.parse_args()

    with open(arguments.version_json, encoding="utf-8-sig") as stream:
        version = json.load(stream)

    classpath = []
    missing = []
    for library in version.get("libraries", []):
        if not is_allowed(library):
            continue
        relative = (library.get("downloads", {})
                    .get("artifact", {})
                    .get("path")) or coordinate_path(library.get("name", ""))
        if not relative:
            continue
        path = os.path.join(arguments.libraries, relative)
        if os.path.isfile(path):
            if path not in classpath:
                classpath.append(path)
        elif not library.get("natives"):
            missing.append(path)

    for path in [arguments.client_jar] + arguments.extra:
        if not os.path.isfile(path):
            missing.append(path)
        elif path not in classpath:
            classpath.append(path)

    if missing:
        for path in missing:
            print("Missing classpath entry: {}".format(path), file=sys.stderr)
        return 1

    print(os.pathsep.join(classpath))
    return 0


if __name__ == "__main__":
    sys.exit(main())
