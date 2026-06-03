#!/usr/bin/env python3
import argparse
import json
import struct
import sys
import zipfile

TARGET = "screenResolution_target_field"
ACC_STATIC = 0x0008


def u2(data, off):
    return struct.unpack_from("<H", data, off)[0]


def u4(data, off):
    return struct.unpack_from("<I", data, off)[0]


def read_uleb128(data, off):
    result = 0
    shift = 0
    while True:
        b = data[off]
        off += 1
        result |= (b & 0x7F) << shift
        if (b & 0x80) == 0:
            return result, off
        shift += 7


def read_string(data, off):
    _, off = read_uleb128(data, off)
    end = off
    while data[end] != 0:
        end += 1
    return data[off:end].decode("utf-8", errors="replace")


def java_type(descriptor):
    if descriptor.startswith("L") and descriptor.endswith(";"):
        return descriptor[1:-1].replace("/", ".")
    return descriptor


def parse_dex(data, dex_name):
    if not data.startswith(b"dex\n"):
        return []

    string_count = u4(data, 0x38)
    string_off = u4(data, 0x3C)
    type_count = u4(data, 0x40)
    type_off = u4(data, 0x44)
    proto_count = u4(data, 0x48)
    proto_off = u4(data, 0x4C)
    field_count = u4(data, 0x50)
    field_off = u4(data, 0x54)
    method_count = u4(data, 0x58)
    method_off = u4(data, 0x5C)
    class_count = u4(data, 0x60)
    class_off = u4(data, 0x64)

    strings = []
    target_indexes = set()
    for i in range(string_count):
        value = read_string(data, u4(data, string_off + i * 4))
        strings.append(value)
        if value == TARGET:
            target_indexes.add(i)
    if not target_indexes:
        return []

    types = [strings[u4(data, type_off + i * 4)] for i in range(type_count)]

    protos = []
    for i in range(proto_count):
        off = proto_off + i * 12
        return_type = types[u4(data, off + 4)]
        parameters_off = u4(data, off + 8)
        parameters = []
        if parameters_off:
            size = u4(data, parameters_off)
            parameters = [
                types[u2(data, parameters_off + 4 + p * 2)]
                for p in range(size)
            ]
        protos.append((return_type, parameters))

    methods = []
    for i in range(method_count):
        off = method_off + i * 8
        class_type = types[u2(data, off)]
        proto = protos[u2(data, off + 2)]
        name = strings[u4(data, off + 4)]
        methods.append({
            "index": i,
            "class": class_type,
            "name": name,
            "return": proto[0],
            "params": proto[1],
            "access": 0,
            "code_off": 0,
        })

    results = []
    for i in range(field_count):
        off = field_off + i * 8
        class_type = types[u2(data, off)]
        field_type = types[u2(data, off + 2)]
        name_index = u4(data, off + 4)
        if name_index in target_indexes:
            results.append({
                "route": "field",
                "confidence": "medium",
                "score": 55,
                "dex": dex_name,
                "class": java_type(class_type),
                "member": strings[name_index],
                "descriptor": field_type,
                "reasons": ["field-name"],
            })

    for i in range(class_count):
        off = class_off + i * 32
        class_data_off = u4(data, off + 24)
        if class_data_off == 0:
            continue
        cursor = class_data_off
        static_fields_size, cursor = read_uleb128(data, cursor)
        instance_fields_size, cursor = read_uleb128(data, cursor)
        direct_methods_size, cursor = read_uleb128(data, cursor)
        virtual_methods_size, cursor = read_uleb128(data, cursor)
        for _ in range(static_fields_size + instance_fields_size):
            _, cursor = read_uleb128(data, cursor)
            _, cursor = read_uleb128(data, cursor)
        method_index = 0
        for _ in range(direct_methods_size + virtual_methods_size):
            diff, cursor = read_uleb128(data, cursor)
            method_index += diff
            access, cursor = read_uleb128(data, cursor)
            code_off, cursor = read_uleb128(data, cursor)
            if code_off == 0:
                continue
            if method_index < 0 or method_index >= len(methods):
                continue
            method = methods[method_index]
            method["access"] = access
            method["code_off"] = code_off
            if not code_references_target_string(data, code_off, target_indexes):
                continue
            results.append(method_result(dex_name, method))

    return results


def code_references_target_string(data, code_off, target_indexes):
    insns_size = u4(data, code_off + 12)
    insns_off = code_off + 16
    unit = 0
    while unit < insns_size:
        instruction = u2(data, insns_off + unit * 2)
        opcode = instruction & 0xFF
        if opcode == 0x1A and unit + 1 < insns_size:
            string_index = u2(data, insns_off + (unit + 1) * 2)
            if string_index in target_indexes:
                return True
        elif opcode == 0x1B and unit + 2 < insns_size:
            string_index = u4(data, insns_off + (unit + 1) * 2)
            if string_index in target_indexes:
                return True
        unit += 1
    return False


def method_result(dex_name, method):
    is_static = (method["access"] & ACC_STATIC) != 0
    params = method["params"]
    ret = method["return"]
    route = "inspect"
    score = 25
    reasons = ["references-target-string"]
    if is_static:
        score += 20
        reasons.append("static")
    if is_static and ret == "I" and not params:
        route = "getter"
        score += 45
        reasons += ["returns-int", "no-args"]
    elif is_static and ret == "V" and params == ["I"]:
        route = "setter"
        score += 30
        reasons.append("void-int")
    confidence = "high" if score >= 80 else "medium" if score >= 50 else "low"
    descriptor = f"({''.join(params)}){ret}"
    return {
        "route": route,
        "confidence": confidence,
        "score": score,
        "dex": dex_name,
        "class": java_type(method["class"]),
        "member": method["name"],
        "descriptor": descriptor,
        "reasons": reasons,
    }


def locate(apk_path):
    results = []
    with zipfile.ZipFile(apk_path) as apk:
        dex_names = sorted(
            name for name in apk.namelist()
            if name.startswith("classes") and name.endswith(".dex")
        )
        for dex_name in dex_names:
            data = apk.read(dex_name)
            if TARGET.encode("ascii") not in data:
                continue
            results.extend(parse_dex(data, dex_name))
    return sorted(results, key=lambda r: (-r["score"], r["dex"], r["class"], r["member"]))


def print_table(results):
    if not results:
        print(f"No route candidates found for {TARGET!r}.")
        return
    headers = ["route", "confidence", "score", "dex", "class", "member", "descriptor", "reasons"]
    rows = []
    for item in results:
        rows.append([
            item["route"],
            item["confidence"],
            str(item["score"]),
            item["dex"],
            item["class"],
            item["member"],
            item["descriptor"],
            ",".join(item["reasons"]),
        ])
    widths = [
        max(len(headers[i]), *(len(row[i]) for row in rows))
        for i in range(len(headers))
    ]
    print("  ".join(headers[i].ljust(widths[i]) for i in range(len(headers))))
    print("  ".join("-" * widths[i] for i in range(len(headers))))
    for row in rows:
        print("  ".join(row[i].ljust(widths[i]) for i in range(len(headers))))


def main():
    parser = argparse.ArgumentParser(
        description="Locate WeChat screenResolution_target_field hook candidates in an APK."
    )
    parser.add_argument("apk")
    parser.add_argument("--json", action="store_true")
    args = parser.parse_args()
    results = locate(args.apk)
    if args.json:
        print(json.dumps(results, ensure_ascii=False, indent=2))
    else:
        print_table(results)


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:
        print(f"error: {exc}", file=sys.stderr)
        sys.exit(1)
