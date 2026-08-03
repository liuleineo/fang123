# -*- coding: utf-8 -*-
"""把整理后的学校数据 JSON 生成可导入 school 表的 INSERT SQL"""
import json

JSON_PATH = "/Users/neo/neocode/fang123/govdata/学校数据/整理后的学校数据0802.json"
OUT_PATH = "/Users/neo/neocode/fang123/govdata/学校数据/import_school.sql"

FIELDS = [
    "campus_code", "school_org_code", "school_org_name", "campus_name",
    "school_address", "contact_phone", "school_type", "sponsor_type",
    "longitude", "latitude", "edu_admin_department", "school_district_scope",
    "school_intro", "target_middle_school_name", "target_middle_school_code",
    "community_names", "district_map_image", "map_fence", "photos", "school_logo",
]


def esc(val):
    """转义 SQL 字符串，处理 None/空"""
    if val is None:
        return "NULL"
    s = str(val)
    s = s.replace("\\", "\\\\").replace("'", "''").replace("\n", "\\n")
    return f"'{s}'"


def val_for(field, v):
    if v is None:
        return "NULL"
    if isinstance(v, (dict, list)):
        return esc(json.dumps(v, ensure_ascii=False))
    if field in ("longitude", "latitude"):
        try:
            f = float(v)
            return str(f)
        except (ValueError, TypeError):
            return "NULL"
    return esc(v)


def main():
    with open(JSON_PATH, encoding="utf-8") as f:
        data = json.load(f)

    cols = ", ".join(FIELDS)
    lines = [
        "-- 自动生成：整理后的学校数据0802.json -> school 表",
        "-- 使用 ON DUPLICATE KEY UPDATE，campus_code 重复时覆盖更新",
        f"-- 共 {len(data)} 条",
        "",
        f"INSERT INTO school ({cols}) VALUES",
    ]
    rows = []
    for i, d in enumerate(data):
        values = ", ".join(val_for(field, d.get(field)) for field in FIELDS)
        rows.append(f"({values}),")
    lines.extend(rows)
    lines[-1] = lines[-1].rstrip(",")  # 最后一行去掉末尾逗号
    lines.append("ON DUPLICATE KEY UPDATE")
    lines.append(", ".join(f"{f}=VALUES({f})" for f in FIELDS) + ";")

    with open(OUT_PATH, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))

    print(f"已生成 SQL: {OUT_PATH}（{len(data)} 条）")


if __name__ == "__main__":
    main()
