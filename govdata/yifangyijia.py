import requests
import time
import json

# -------------------------- 1. 基础配置 --------------------------
APP_ID = "hzssjkfpt"                  # 平台申请的应用ID
USER_SECRET = "zjzw33008781"          # 用户密钥
INTERFACE_ID = "331111zjhzFN3mq20201024152710137829SynReq"
VERSION = "1.0.0"
CHARSET = "utf-8"
ORIGIN = "0"

# 验签接口
SIGN_URL = "http://data.zjzwfw.gov.cn/jimp/sign/createsign.do"
# 数据接口
DATA_URL = "https://data.zjzwfw.gov.cn/interface/gateway.do"

# 业务参数（二选一：userSecret / appSecret，这里用 userSecret）
biz_content = {
    "pageNumber": 1,
    "pageSize": 10,
    "userSecret": USER_SECRET
}

# -------------------------- 2. 生成时间戳、biz_content 串 --------------------------
timestamp = str(int(time.time() * 1000))
biz_str = json.dumps(biz_content, ensure_ascii=False)

# -------------------------- 3. 调用验签接口获取 sign --------------------------
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
    r.raise_for_status()
    sign = r.text.strip()  # 接口直接返回 sign 字符串
    print("获取 sign 成功：", sign)
except Exception as e:
    print("验签失败：", e)
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
    resp.raise_for_status()
    res = resp.json()
    print("\n数据接口返回：")
    print(json.dumps(res, ensure_ascii=False, indent=2))
except Exception as e:
    print("数据接口调用失败：", e)
