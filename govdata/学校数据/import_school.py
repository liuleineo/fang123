# -*- coding: utf-8 -*-
"""导入整理后的学校数据到 school 表"""
import json
import pymysql

JSON_PATH = "/Users/neo/neocode/fang123/govdata/学校数据/整理后的学校数据0802.json"
DB = {
    "host": "localhost",
    "port": 3306,
    "user": "root",
    "password": "liulei0304",
    "database": "fang123",
    "charset": "utf8mb4",
}

FIELDS = [
    "campus_code", "school_org_code", "school_org_name", "campus_name",
    "school_address", "contact_phone", "school_type", "sponsor_type",
    "longitude", "latitude", "edu_admin_department", "school_district_scope",
    "school_intro", "target_middle_school_name", "target_middle_school_code",
    "community_names", "district_map_image", "map_fence", "photos", "school_logo",
]

# 经纬度单独处理为浮点
NUM_FIELDS = {"longitude", "latitude"}


def to_sql_value(val):
    """将任意类型转为可入库的字符串/数值，兼容 list/dict"""
    if val is None:
        return None
    if isinstance(val, (dict, list)):
        # 列表/字典转 JSON 字符串
        return json.dumps(val, ensure_ascii=False)
    return str(val)


def main():
    with open(JSON_PATH, encoding="utf-8") as f:
        data = json.load(f)

    conn = pymysql.connect(**DB)
    cur = conn.cursor()

    # 清空旧数据
    cur.execute("DELETE FROM school")
    conn.commit()
    print(f"已清空旧数据，开始导入 {len(data)} 条...")

    # 用 INSERT ON DUPLICATE KEY UPDATE 处理主键重复
    cols = ", ".join(FIELDS)
    placeholders = ", ".join(["%s"] * len(FIELDS))
    updates = ", ".join([f"{f}=VALUES({f})" for f in FIELDS])
    sql = f"INSERT INTO school ({cols}) VALUES ({placeholders}) ON DUPLICATE KEY UPDATE {updates}"

    inserted = 0
    skipped = 0
    for d in data:
        try:
            row = []
            for field in FIELDS:
                val = d.get(field, "")
                if field in NUM_FIELDS:
                    v = to_sql_value(val)
                    row.append(float(v) if v and v.strip() else None)
                else:
                    v = to_sql_value(val)
                    row.append(v if v else "")
            cur.execute(sql, row)
            inserted += 1
        except Exception as e:
            skipped += 1
            print(f"跳过 {d.get('campus_code')} {d.get('school_org_name')}: {e}")

    conn.commit()
    cur.close()
    conn.close()
    print(f"完成：成功导入 {inserted} 条，跳过 {skipped} 条")


if __name__ == "__main__":
    main()
