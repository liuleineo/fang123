import requests
import time
import json
import csv
from datetime import datetime

# -------------------------- 1. 基础配置 --------------------------
APP_ID = "hzssjkfpt"
USER_SECRET = "zjzw33008781"
INTERFACE_ID = "331111zjprovince10506SynReq"
VERSION = "1.0.0"
CHARSET = "utf-8"
ORIGIN = "0"

SIGN_URL = "http://data.zjzwfw.gov.cn/jimp/sign/createsign.do"
DATA_URL = "https://data.zjzwfw.gov.cn/interface/gateway.do"

# 业务参数
biz_content = {
    "pageNumber": 415,
    "pageSize": 100,
    "userSecret": USER_SECRET
}

# -------------------------- 2. 生成时间戳 --------------------------
timestamp = str(int(time.time() * 1000))
biz_str = json.dumps(biz_content, ensure_ascii=False)

# -------------------------- 3. 获取签名 --------------------------
sign_params = {
    "app_id": APP_ID,
    "interface_id": INTERFACE_ID,
    "version": VERSION,
    "charset": CHARSET,
    "timestamp": timestamp,
    "origin": ORIGIN,
    "biz_content": biz_str
}

try:
    r = requests.post(SIGN_URL, data=sign_params, timeout=15)
    sign = r.text.strip()
    print("✅ 获取 sign 成功：", sign)
except Exception as e:
    print("❌ 验签失败：", e)
    exit()

# -------------------------- 4. 调用数据接口 --------------------------
data_params = {
    "app_id": APP_ID,
    "interface_id": INTERFACE_ID,
    "version": VERSION,
    "charset": CHARSET,
    "timestamp": timestamp,
    "origin": ORIGIN,
    "sign": sign,
    "biz_content": biz_str
}

try:
    resp = requests.post(DATA_URL, data=data_params, timeout=15)
    res = resp.json()
    print("\n✅ 接口调用成功")
except Exception as e:
    print("❌ 数据接口调用失败：", e)
    exit()

# -------------------------- 5. 【核心】解析嵌套JSON --------------------------
try:
    # 把返回的 data 字符串转成 JSON
    data_json = json.loads(res["data"])
    print(data_json)

    total = data_json["total"]
    message = data_json["message"]
    rows = data_json["rows"]

    print(f"\n📊 总记录数：{total}")
    print(f"📶 返回状态：{message}")
    print("-" * 80)

    # 表头 + 数据
    headers = rows[0]
    datas = rows[1:]

    print("📋 表头：")
    print(headers)
    print("-" * 80)

    print("📄 数据列表：")
    for i, row in enumerate(datas, 1):
        print(f"{i:2d}. {row}")

    # -------------------------- 6. 保存为 CSV 文件 --------------------------
    filename = f"杭州商品房成交数据_{datetime.now().strftime('%Y%m%d_%H%M%S')}.csv"
    with open(filename, "w", newline="", encoding="utf-8-sig") as f:
        writer = csv.writer(f)
        writer.writerows(rows)

    print(f"\n💾 文件已保存：{filename}")

except Exception as e:
    print("❌ 解析数据失败：", e)
    print("原始返回：", json.dumps(res, ensure_ascii=False, indent=2))