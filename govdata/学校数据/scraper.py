#!/usr/bin/env python3
"""
杭州市学校数据增强脚本
从"入学早知道" (rxyj.hzedu.gov.cn) API 抓取补充字段：

7 个 JSON 目标字段：
  school_detail              — 学校介绍 (appSchoolInfoEntity.schoolDetail)
  direct_middle_school_name  — 小学对口初中名称 (appSchoolInfoEntity.directMiddleSchoolName)
  direct_middle_school_code     — 小学对口初中代码 (appSchoolInfoEntity.directMiddleSchoolCode)
  photos                     — 学校图片，多张URL逗号分隔 (appSchoolPhotoInfoEntityList[].thumbnailUrl)
  map_info                   — 学区范围地图围栏，高德格式 lng,lat;lng,lat (appSchoolMapInfoEntities)
  xqdt                       — 学区地图图片 (result.xqdt)
  xqmc_all                   — 学区范围内的小区，逗号分隔 (appSchoolDistrictInfoEntityList[].xqmc)

附加字段：
  school_logo
"""

import json
import time
import requests
import os

API_URL = "https://rxyj.hzedu.gov.cn/hzjyAppServer/api/AppSchoolInfo/getSchoolInfo"
YEAR = "2026"
INPUT_FILE = "杭州市小学和初中学校校区信息20260721.json"
OUTPUT_FILE = "杭州市小学和初中学校校区信息20260721_enriched.json"
PROGRESS_FILE = "scraper_progress.json"

HEADERS = {
    "User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36",
    "Accept": "application/json",
    "Referer": "https://rxyj.hzedu.gov.cn/",
}


def load_input():
    with open(INPUT_FILE, "r", encoding="utf-8") as f:
        return json.load(f)


def build_campus_list(records):
    """
    Build task list from records using their existing xqbsm field.
    Returns list of (record_index, xqbsm) tuples.
    """
    campus_tasks = []
    for i, record in enumerate(records):
        xqbsm = record.get("xqbsm", "")
        if xqbsm:
            campus_tasks.append((i, xqbsm))
    return campus_tasks


def load_progress():
    if os.path.exists(PROGRESS_FILE):
        with open(PROGRESS_FILE, "r") as f:
            return json.load(f)
    return {"completed": {}, "errors": {}}


def save_progress(progress):
    with open(PROGRESS_FILE, "w") as f:
        json.dump(progress, f, ensure_ascii=False, indent=2)


def fetch_school_info(xqbsm, retries=3):
    """Fetch school info from API."""
    params = {"year": YEAR, "schoolName": xqbsm, "source": "undefined"}
    for attempt in range(retries):
        try:
            resp = requests.get(API_URL, params=params, headers=HEADERS, timeout=30)
            resp.raise_for_status()
            data = resp.json()
            if data.get("success") and data.get("result"):
                return data["result"]
            else:
                print(f"  API returned non-success for {xqbsm}: {data.get('message')}")
                return None
        except requests.exceptions.Timeout:
            print(f"  Timeout for {xqbsm}, attempt {attempt+1}/{retries}")
            time.sleep(2)
        except requests.exceptions.RequestException as e:
            print(f"  Request error for {xqbsm}: {e}, attempt {attempt+1}/{retries}")
            time.sleep(2)
    return None


def build_gaode_fence(map_entities):
    """
    Build 高德地图围栏 format from map entities.
    Format: "经度1,纬度1;经度2,纬度2;经度3,纬度3"
    Sorted by sequence field.
    """
    if not map_entities:
        return None
    sorted_points = sorted(map_entities, key=lambda x: x.get("sequence", 0))
    points = [f"{p['lngGd']},{p['latGd']}" for p in sorted_points]
    return ";".join(points)


def extract_enriched_fields(result):
    """Extract the fields we care about from API result.

    Maps to the 7 JSON target fields:
        school_detail, direct_middle_school_name, direct_middle_school_code,
        photos, map_info, xqdt, xqmc_all
    """
    info = result.get("appSchoolInfoEntity", {}) or {}

    enriched = {}

    # === 7 core JSON target fields ===

    # 1. 学校介绍 (school_detail)
    enriched["school_detail"] = info.get("schoolDetail") or ""

    # 2. 对口初中名字 (direct_middle_school_name)
    enriched["direct_middle_school_name"] = info.get("directMiddleSchoolName") or ""

    # 3. 对口初中代码 (direct_middle_school_code)
    enriched["direct_middle_school_code"] = info.get("directMiddleSchoolCode") or ""

    # 4. 学校图片 photos — 多张图片URL用逗号分隔
    photos = result.get("appSchoolPhotoInfoEntityList") or []
    photo_urls = [p.get("thumbnailUrl", "") for p in photos if p.get("thumbnailUrl")]
    enriched["photos"] = ",".join(photo_urls) if photo_urls else ""

    # 5. 学区范围地图围栏 map_info — 高德格式 lng,lat;lng,lat (appSchoolMapInfoEntities[].lngGd,latGd)
    map_entities = result.get("appSchoolMapInfoEntities") or []
    enriched["map_info"] = build_gaode_fence(map_entities) or ""

    # 6. 学区地图图片 xqdt (单张，位于 result 顶层)
    enriched["xqdt"] = result.get("xqdt") or info.get("xqdt") or ""

    # 7. 学区范围内的小区 xqmc_all — 小区名称逗号分隔 (appSchoolDistrictInfoEntityList[].xqmc)
    districts = result.get("appSchoolDistrictInfoEntityList") or []
    district_names = [d.get("xqmc", "") or d.get("name", "") for d in districts]
    enriched["xqmc_all"] = ",".join(d for d in district_names if d) if district_names else ""

    # === Supplementary enrichment fields ===

    # 学校Logo
    enriched["school_logo"] = info.get("schoolLogo") or ""

    # API 原始 xqbsm / schoolCode 用于校验
    enriched["_api_xqbsm"] = info.get("xqbsm")
    enriched["_api_school_code"] = info.get("schoolCode")

    return enriched


def main():
    print("=" * 60)
    print("杭州市学校数据增强脚本")
    print("=" * 60)

    # Load input
    print("\n[1/4] 加载输入数据...")
    data = load_input()
    header = data[0]
    records = data[1:]
    print(f"  总记录数: {len(records)}")

    # Build campus task list
    print("\n[2/4] 构建校区映射...")
    campus_tasks = build_campus_list(records)
    print(f"  校区任务数: {len(campus_tasks)}")

    # Load progress
    progress = load_progress()
    completed = progress["completed"]
    errors = progress["errors"]
    print(f"  已完成: {len(completed)}, 错误: {len(errors)}")

    # Prepare output records
    enriched_records = []

    print(f"\n[3/4] 开始抓取数据...")
    start_time = time.time()
    batch_start = time.time()

    for i, (rec_idx, xqbsm) in enumerate(campus_tasks):
        record = records[rec_idx].copy()
        school_name = record["XXJGMC"]
        campus_name = record.get("XQMC", "") or "主校区"

        # Skip if already completed
        if xqbsm in completed:
            record.update(completed[xqbsm])
            enriched_records.append((rec_idx, record))
            continue

        # Skip if previously errored (optional: could retry)
        if xqbsm in errors:
            pass  # Will retry

        # Progress display
        pct = (i + 1) / len(campus_tasks) * 100
        elapsed = time.time() - start_time
        eta = (elapsed / (i + 1)) * (len(campus_tasks) - i - 1) if i > 0 else 0
        print(f"  [{i+1}/{len(campus_tasks)}] {pct:.1f}% | {school_name}({campus_name}) | xqbsm={xqbsm} | ETA={eta:.0f}s", end=" ")

        # Fetch from API
        result = fetch_school_info(xqbsm)

        if result:
            enriched = extract_enriched_fields(result)
            record.update(enriched)
            completed[xqbsm] = enriched
            errors.pop(xqbsm, None)
            print("✓")
        else:
            errors[xqbsm] = "API returned no data"
            print("✗")

        enriched_records.append((rec_idx, record))

        # Save progress every 50 records
        if (i + 1) % 50 == 0:
            save_progress({"completed": completed, "errors": errors})
            batch_elapsed = time.time() - batch_start
            print(f"  --- 已保存进度 ({50}/{batch_elapsed:.1f}s={50/batch_elapsed:.2f} req/s) ---")
            batch_start = time.time()

        # Rate limiting: 100ms between requests (max ~10 req/s)
        time.sleep(0.1)

    # Final progress save
    save_progress({"completed": completed, "errors": errors})

    # Sort results back to original order
    print(f"\n[4/4] 整理输出数据...")
    enriched_records.sort(key=lambda x: x[0])
    final_records = [r for _, r in enriched_records]

    # Build output
    output = [header] + final_records

    # Save
    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        json.dump(output, f, ensure_ascii=False, indent=2)

    # Statistics
    total_elapsed = time.time() - start_time
    success_count = len(completed)
    error_count = len(errors)
    print(f"\n{'='*60}")
    print(f"完成！")
    print(f"  成功: {success_count}")
    print(f"  失败: {error_count}")
    print(f"  总耗时: {total_elapsed:.1f}s ({total_elapsed/60:.1f}min)")
    print(f"  输出文件: {OUTPUT_FILE}")
    print(f"{'='*60}")

    # Print errors if any
    if errors:
        print(f"\n失败列表:")
        for xqbsm, reason in list(errors.items())[:20]:
            print(f"  {xqbsm}: {reason}")
        if len(errors) > 20:
            print(f"  ... 及其他 {len(errors)-20} 条")


if __name__ == "__main__":
    main()
