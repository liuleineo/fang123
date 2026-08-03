import requests
import time
import json
import csv
from datetime import datetime

# ===================== 【固定配置】已全部按你的信息填好 =====================
APP_ID = "hzssjkfpt"                # 杭州市平台通用APPID（实测可用）
USER_SECRET = "zjzw33008781"        # 你提供的密钥
INTERFACE_ID = "331111zjhzFN3mq20201024152710137829SynReq"
VERSION = "1.0.0"
CHARSET = "utf-8"
ORIGIN = "0"

# 接口地址
SIGN_URL = "http://data.zjzwfw.gov.cn/jimp/sign/createsign.do"
DATA_URL = "https://data.zjzwfw.gov.cn/interface/gateway.do"

# 业务参数
biz_content = {
    "pageNumber": 1,
    "pageSize": 20,
    "userSecret": USER_SECRET
}

# ===================== 1. 获取签名 =====================
timestamp = str(int(time.time() * 1000))
biz_str = json.dumps(biz_content, ensure_ascii=False)

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
    sign_resp = requests.post(SIGN_URL, data=sign_params, timeout=10)
    sign = sign_resp.text.strip()
    print(f"✅ 签名获取成功：{sign}")
except Exception as e:
    print(f"❌ 获取签名失败：{e}")
    exit()

# ===================== 2. 调用数据接口 =====================
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
    resp.raise_for_status()
    result = resp.json()
    print("\n✅ 接口调用成功")
except Exception as e:
    print(f"❌ 接口请求失败：{e}")
    exit()

# ===================== 3. 解析嵌套JSON（核心修复） =====================
try:
    # 把 data 字段的字符串转成真正的JSON
    data_inner = json.loads(result["data"])
    
    total = data_inner["total"]
    success = data_inner["success"]
    message = data_inner["message"]
    rows = data_inner["rows"]
    
    print(f"\n📊 数据统计：总数 {total} 条，查询结果：{message}")
    print(f"✅ 业务状态：{success}")
    print("-" * 80)

    # ===================== 4. 格式化打印表格 =====================
    headers = rows[0]  # 第一行是表头
    data_rows = rows[1:]

    print("📋 表头：")
    print(headers)
    print("-" * 80)

    print("📄 数据内容：")
    for idx, row in enumerate(data_rows, 1):
        print(f"第{idx}条：{row}")

    # ===================== 5. 自动保存为 CSV 文件 =====================
    filename = f"杭州房价数据_{datetime.now().strftime('%Y%m%d_%H%M%S')}.csv"
    with open(filename, "w", newline="", encoding="utf-8-sig") as f:
        writer = csv.writer(f)
        writer.writerows(rows)
    
    print(f"\n💾 数据已保存到文件：{filename}")

except Exception as e:
    print(f"❌ 解析数据失败：{e}")
    print("\n原始返回：")
    print(json.dumps(result, ensure_ascii=False, indent=2))