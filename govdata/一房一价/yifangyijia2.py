# -*- coding: utf-8 -*-
"""
杭州一房一价全量抓取脚本（并发模式）
- 总条数约 2192284 条，pageSize=100，共约 21923 页
- 并发抓取，限速每秒 5 个请求
- 全部写入同一个 CSV 文件（线程安全追加）
- 终端实时输出进度（页数/条数/百分比/速率/预计剩余时间）
- 支持断点续传：progress 文件记录已抓取页码
"""
import requests
import time
import json
import csv
import os
import queue
import threading
import math
from concurrent.futures import ThreadPoolExecutor

# ===================== 【固定配置】 =====================
APP_ID = "hzssjkfpt"
USER_SECRET = "zjzw33008781"
INTERFACE_ID = "331111zjhzFN3mq20201024152710137829SynReq"
VERSION = "1.0.0"
CHARSET = "utf-8"
ORIGIN = "0"

SIGN_URL = "http://data.zjzwfw.gov.cn/jimp/sign/createsign.do"
DATA_URL = "https://data.zjzwfw.gov.cn/interface/gateway.do"

PAGE_SIZE = 100           # 每次获取条数
TARGET_TOTAL = 2192284    # 数据总条数
MAX_PER_SEC = 5           # 每秒最大请求数（并发限速）
WORKERS = 5               # 并发线程数

OUT_CSV = "杭州房价数据_全量.csv"
PROGRESS_FILE = "yifangyijia_progress.json"

# ===================== 限速器（令牌桶，每秒最多 max_per_sec 个请求） =====================
class RateLimiter:
    def __init__(self, max_per_sec):
        self.max_per_sec = max_per_sec
        self._times = []
        self._lock = threading.Lock()

    def wait(self):
        while True:
            with self._lock:
                now = time.time()
                self._times = [t for t in self._times if now - t < 1.0]
                if len(self._times) < self.max_per_sec:
                    self._times.append(now)
                    return
            time.sleep(0.05)


# ===================== 全局状态 =====================
limiter = RateLimiter(MAX_PER_SEC)
csv_lock = threading.Lock()
stats_lock = threading.Lock()

headers = []          # CSV 表头
file_exists = False   # CSV 是否已存在（含表头）

# 统计
done_pages = 0
done_rows = 0
fail_pages = 0
start_time = time.time()

TOTAL_PAGES = math.ceil(TARGET_TOTAL / PAGE_SIZE)


# ===================== 请求 =====================
def get_sign(biz_str):
    timestamp = str(int(time.time() * 1000))
    sign_params = {
        "app_id": APP_ID, "interface_id": INTERFACE_ID, "version": VERSION,
        "charset": CHARSET, "timestamp": timestamp, "origin": ORIGIN,
        "biz_content": biz_str
    }
    resp = requests.post(SIGN_URL, data=sign_params, timeout=10)
    return timestamp, resp.text.strip()


def fetch_page(page_no, retry=3):
    """获取指定页数据，返回 (total, rows)；失败返回 (None, None)"""
    biz_content = {"pageNumber": page_no, "pageSize": PAGE_SIZE, "userSecret": USER_SECRET}
    biz_str = json.dumps(biz_content, ensure_ascii=False)
    for attempt in range(retry):
        try:
            timestamp, sign = get_sign(biz_str)
            data_params = {
                "app_id": APP_ID, "interface_id": INTERFACE_ID, "version": VERSION,
                "charset": CHARSET, "timestamp": timestamp, "origin": ORIGIN,
                "sign": sign, "biz_content": biz_str
            }
            resp = requests.post(DATA_URL, data=data_params, timeout=15)
            resp.raise_for_status()
            result = resp.json()
            data_inner = json.loads(result["data"])
            total = data_inner.get("total")
            rows = data_inner.get("rows") or []
            return total, rows
        except Exception as e:
            if attempt < retry - 1:
                time.sleep(1)
            else:
                print(f"   ❌ 第{page_no}页最终失败：{e}")
    return None, None


# ===================== 断点续传 =====================
def load_progress():
    if os.path.exists(PROGRESS_FILE):
        try:
            with open(PROGRESS_FILE, encoding="utf-8") as f:
                return json.load(f).get("last_page", 0)
        except Exception:
            pass
    return 0


def save_progress(page):
    with open(PROGRESS_FILE, "w", encoding="utf-8") as f:
        json.dump({"last_page": page}, f, ensure_ascii=False)


# ===================== worker：抓取一页并写入 CSV =====================
def process_page(page_no):
    global file_exists, headers, done_pages, done_rows, fail_pages
    limiter.wait()  # 限速
    total, rows = fetch_page(page_no)
    if rows is None:
        with stats_lock:
            fail_pages += 1
        return

    with csv_lock:
        with open(OUT_CSV, "a", newline="", encoding="utf-8-sig") as f:
            if rows:
                if not headers:
                    headers = list(rows[0].keys()) if isinstance(rows[0], dict) else []
                if not file_exists and headers:
                    writer = csv.DictWriter(f, fieldnames=headers, extrasaction="ignore")
                    writer.writeheader()
                    file_exists = True
                if headers:
                    writer = csv.DictWriter(f, fieldnames=headers, extrasaction="ignore")
                    writer.writerows(rows)

    with stats_lock:
        done_pages += 1
        done_rows += len(rows)

    # 定期保存进度（每 20 页保存一次）
    if page_no % 20 == 0:
        save_progress(page_no)


# ===================== 进度输出 =====================
def print_progress(stop_flag):
    while not stop_flag.is_set():
        with stats_lock:
            dp, dr, fp = done_pages, done_rows, fail_pages
        pct = dp / TOTAL_PAGES * 100 if TOTAL_PAGES else 0
        elapsed = time.time() - start_time
        rate = dp / elapsed if elapsed > 0 else 0
        remain_pages = max(TOTAL_PAGES - dp, 0)
        eta = remain_pages / rate if rate > 0 else 0
        print(f"▶ 进度: {dp}/{TOTAL_PAGES} 页 ({pct:.1f}%) | 已抓取 {dr} 条 | "
              f"失败 {fp} 页 | 速率 {rate:.1f} 页/s | 已用 {elapsed/60:.1f}分 | 预计剩余 {eta/60:.1f}分")
        stop_flag.wait(2)


# ===================== 主流程 =====================
def main():
    global TOTAL_PAGES
    start_page = load_progress() + 1
    if start_page > TOTAL_PAGES:
        start_page = 1
    print(f"🔄 并发抓取开始，从第 {start_page} 页，共 {TOTAL_PAGES} 页（pageSize={PAGE_SIZE}，每秒{MAX_PER_SEC}个请求）")

    task_queue = queue.Queue()
    for p in range(start_page, TOTAL_PAGES + 1):
        task_queue.put(p)

    stop_flag = threading.Event()
    progress_thread = threading.Thread(target=print_progress, args=(stop_flag,), daemon=True)
    progress_thread.start()

    def worker():
        while True:
            try:
                page_no = task_queue.get_nowait()
            except queue.Empty:
                return
            process_page(page_no)
            task_queue.task_done()

    threads = [threading.Thread(target=worker, daemon=True) for _ in range(WORKERS)]
    for t in threads:
        t.start()
    for t in threads:
        t.join()

    task_queue.join()
    stop_flag.set()
    progress_thread.join(timeout=3)

    with stats_lock:
        dp, dr, fp = done_pages, done_rows, fail_pages
    save_progress(dp + start_page - 1)
    print(f"\n✅ 全部完成！累计写入 {dr} 条 → {OUT_CSV}（失败页 {fp}）")
    print(f"   进度已保存到 {PROGRESS_FILE}，若中断可重新运行续传。")


if __name__ == "__main__":
    main()
