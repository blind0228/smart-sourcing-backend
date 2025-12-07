import time
import json
import requests
import statistics
import logging
import sys
from datetime import datetime, timedelta

try:
    import boto3
    from botocore.exceptions import ClientError
except ImportError:
    print("boto3 설치 필요")
    sys.exit(1)

logging.basicConfig(
    level=logging.INFO,
    format='[%(asctime)s] %(levelname)s - %(message)s',
    handlers=[logging.StreamHandler(sys.stdout)]
)
logger = logging.getLogger()

QUEUE_URL = "https://sqs.us-east-1.amazonaws.com/730335221432/smart-sourcing-queue"
sqs_client = boto3.client('sqs', region_name='us-east-1')

NAVER_CLIENT_ID = "I1uLlyo_ne_BHszaVd3R"
NAVER_CLIENT_SECRET = "swMHAs3qpq"
SPRING_BOOT_API = "http://http://54.152.105.176:8080/"


# ---------------------------------------------------------
# 🔥 네이버 쇼핑 검색 (키워드 기반)
# ---------------------------------------------------------
def get_naver_shopping_data(keyword):
    url = "https://openapi.naver.com/v1/search/shop.json"
    headers = {
        "X-Naver-Client-Id": NAVER_CLIENT_ID,
        "X-Naver-Client-Secret": NAVER_CLIENT_SECRET
    }
    params = {"query": keyword, "display": 100, "sort": "sim"}

    try:
        response = requests.get(url, headers=headers, params=params)
        response.raise_for_status()
        return response.json()
    except:
        return {"items": []}


# ---------------------------------------------------------
# 🔥 카테고리별 대표 검색어
# ---------------------------------------------------------
CATEGORY_KEYWORD_MAP = {
    "패션의류": "겨울옷",
    "화장품/미용": "화장품",
    "식품": "건강식품",
}


# ---------------------------------------------------------
# 🔥 카테고리별 TOP10 랭킹 생성
# ---------------------------------------------------------
def fetch_naver_shopping_ranking():
    ranking_list = []
    rank_counter = 1

    for category_name, keyword in CATEGORY_KEYWORD_MAP.items():

        logger.info(f"🔥 카테고리 {category_name} 대표 검색어 = {keyword}")

        shopping_data = get_naver_shopping_data(keyword)
        items = shopping_data.get("items", [])

        if not items:
            ranking_list.append({
                "rank": rank_counter,
                "keyword": f"[{category_name}] 대표 상품 없음",
                "searchRatio": 0
            })
            rank_counter += 1
            continue

        # 🔥 TOP10 상품 저장
        for item in items[:10]:
            clean_title = item["title"].replace("<b>", "").replace("</b>", "")
            ranking_list.append({
                "rank": rank_counter,
                "keyword": f"[{category_name}] {clean_title}",
                "searchRatio": 0
            })
            rank_counter += 1

    logger.info(f"🎯 최종 생성된 카테고리 TOP10 랭킹 개수: {len(ranking_list)}")
    return ranking_list


# ---------------------------------------------------------
# 🔥 상세 분석 (기존 기능 그대로)
# ---------------------------------------------------------
def get_naver_datalab_trend(keyword):
    today = datetime.now()
    start_date = (today - timedelta(days=30)).strftime('%Y-%m-%d')
    end_date = today.strftime('%Y-%m-%d')

    url = "https://openapi.naver.com/v1/datalab/search"
    headers = {
        "X-Naver-Client-Id": NAVER_CLIENT_ID,
        "X-Naver-Client-Secret": NAVER_CLIENT_SECRET,
        "Content-Type": "application/json"
    }
    body = {
        "startDate": start_date,
        "endDate": end_date,
        "timeUnit": "date",
        "keywordGroups": [{"groupName": keyword, "keywords": [keyword]}],
    }

    try:
        res = requests.post(url, headers=headers, json=body)
        res.raise_for_status()
        data = res.json()

        recent = [d["ratio"] for d in data["results"][0]["data"][-7:]]
        return int(statistics.mean(recent)) if recent else 0
    except:
        return 0


def analyze_market(items, keyword, total_results, avg_search_ratio):
    if not items:
        return None

    prices = [int(item['lprice']) for item in items]
    avg_price = int(statistics.mean(prices))
    min_price = min(prices)
    top_item = items[0]['title'].replace("<b>", "").replace("</b>", "")

    return {
        "searchKeyword": keyword,
        "category": items[0].get("category1", "Unknown"),
        "averagePrice": avg_price,
        "lowestPrice": min_price,
        "sampleCount": len(items),
        "topItemName": top_item,
        "totalListings": total_results,
        "competitionLevel": "보통",
        "searchVolumeRatio": avg_search_ratio,
        "marketAttractiveness": "높음",
        "sourcingScore": avg_search_ratio
    }


# ---------------------------------------------------------
# 🔥 백엔드 전송
# ---------------------------------------------------------
def send_ranking_to_backend(data):
    try:
        url = f"{SPRING_BOOT_API}/api/market/ranking/receive"
        requests.post(url, json=data).raise_for_status()
        logger.info("🚀 랭킹 전송 성공")
    except:
        logger.error("❌ 랭킹 전송 실패")

def send_analysis_to_backend(data):
    try:
        url = f"{SPRING_BOOT_API}/api/market/analysis"
        requests.post(url, json=data).raise_for_status()
        logger.info("🚀 분석 전송 성공")
    except:
        logger.error("❌ 분석 전송 실패")


# ---------------------------------------------------------
# 🔥 Worker 실행
# ---------------------------------------------------------
if __name__ == "__main__":

    print("=== 카테고리별 TOP10 랭킹 수집 시작 ===")
    ranking = fetch_naver_shopping_ranking()
    send_ranking_to_backend(ranking)
    print("=== 랭킹 완료 ===")

    logger.info("⚡ SQS 메시지 처리 시작")

    while True:
        try:
            res = sqs_client.receive_message(
                QueueUrl=QUEUE_URL,
                MaxNumberOfMessages=1,
                WaitTimeSeconds=20
            )

            msgs = res.get("Messages", [])
            if not msgs:
                logger.info("📭 메시지 없음 대기 중…")
                continue

            for m in msgs:
                body = json.loads(m["Body"])
                keyword = body["keyword"]
                logger.info(f"🔍 키워드 수신: {keyword}")

                shopping = get_naver_shopping_data(keyword)
                items = shopping.get("items", [])
                total = shopping.get("total", 0)
                trend = get_naver_datalab_trend(keyword)

                result = analyze_market(items, keyword, total, trend)
                if result:
                    send_analysis_to_backend(result)

                sqs_client.delete_message(
                    QueueUrl=QUEUE_URL,
                    ReceiptHandle=m["ReceiptHandle"]
                )

        except Exception as e:
            logger.error(f"❗ 오류 발생: {e}")
            time.sleep(5)
