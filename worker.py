import time
import json
import requests
import statistics
import logging
import sys
from datetime import datetime, timedelta
import os
import warnings # ⭐ 경고를 제어하기 위해 추가

# ---------------------------------------------------------
# 🛠️ 라이브러리 체크
# ---------------------------------------------------------
try:
    import boto3
    from botocore.exceptions import ClientError
except ImportError:
    print("❌ boto3 라이브러리가 없습니다. requirements.txt에 boto3를 추가했는지 확인하세요.")
    sys.exit(1)

# ---------------------------------------------------------
# 📝 로깅 설정 (Docker 로그 확인용)
# ---------------------------------------------------------
logging.basicConfig(
    level=logging.INFO,
    format='[%(asctime)s] %(levelname)s - %(message)s',
    handlers=[logging.StreamHandler(sys.stdout)] # 표준 출력으로 내보내야 도커 로그에서 보임
)
logger = logging.getLogger()

# =========================================================
# ⚙️ 설정값 (AWS & API & Backend)
# =========================================================

# 1. AWS SQS 설정
QUEUE_URL = "https://sqs.us-east-1.amazonaws.com/730335221432/smart-sourcing-queue"
AWS_REGION = 'us-east-1'

# 2. 네이버 API 키 (하드코딩 됨 - 보안상 환경변수 권장하나 편의를 위해 유지)
NAVER_CLIENT_ID = "I1uLlyo_ne_BHszaVd3R"
NAVER_CLIENT_SECRET = "swMHAs3qpq"

# 3. Spring Boot 백엔드 주소 (ALB 주소 적용)
# Docker 내부에서는 localhost가 아닌 외부 ALB 주소를 바라봐야 합니다.
DEFAULT_API_URL = "http://smartsourcing-alb-new-409803492.us-east-1.elb.amazonaws.com"
SPRING_BOOT_API = os.getenv("SPRING_BOOT_API_URL", DEFAULT_API_URL).rstrip('/') # 끝에 /가 있으면 제거

# SQS 클라이언트 생성
try:
    sqs_client = boto3.client('sqs', region_name=AWS_REGION)
except Exception as e:
    logger.error(f"❌ AWS SQS 클라이언트 초기화 실패: {e}")
    sys.exit(1)


# =========================================================
# 📡 외부 API 연동 함수들
# =========================================================

# 1. 네이버 쇼핑 검색 API
def get_naver_shopping_data(keyword):
    url = "https://openapi.naver.com/v1/search/shop.json"
    headers = {
        "X-Naver-Client-Id": NAVER_CLIENT_ID,
        "X-Naver-Client-Secret": NAVER_CLIENT_SECRET
    }
    params = {"query": keyword, "display": 100, "sort": "sim"}

    try:
        response = requests.get(url, headers=headers, params=params, timeout=5)
        response.raise_for_status()
        return response.json()
    except requests.exceptions.HTTPError:
        logger.error(f"❌ [네이버 쇼핑 API 에러] 키워드: {keyword} | 상태코드: {response.status_code}")
        return {"items": []}
    except Exception as e:
        logger.error(f"❌ [네이버 쇼핑 API 연결 실패]: {e}")
        return {"items": []}

# 2. 네이버 데이터랩 트렌드 API
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
        res = requests.post(url, headers=headers, json=body, timeout=5)
        res.raise_for_status()
        data = res.json()

        # 최근 7일 평균값 계산
        if "results" in data and data["results"]:
            recent = [d["ratio"] for d in data["results"][0]["data"][-7:]]
            return int(statistics.mean(recent)) if recent else 0
        return 0
    except Exception as e:
        logger.warning(f"⚠️ [데이터랩 API 실패] 키워드: {keyword} (기본값 0 처리) - {e}")
        return 0


# =========================================================
# 🧠 비즈니스 로직 (분석 및 랭킹)
# =========================================================

CATEGORY_KEYWORD_MAP = {
    "패션의류": "겨울옷",
    "화장품/미용": "화장품",
    "식품": "건강식품",
}

# 1. 카테고리 랭킹 생성
def fetch_naver_shopping_ranking():
    ranking_list = []
    rank_counter = 1
    logger.info("📊 카테고리별 랭킹 데이터 수집 시작...")

    for category_name, keyword in CATEGORY_KEYWORD_MAP.items():
        logger.debug("카테고리 %s: 키워드 %s 수집 시작", category_name, keyword)
        shopping_data = get_naver_shopping_data(keyword)
        items = shopping_data.get("items", [])

        if not items:
            ranking_list.append({
                "rank": rank_counter,
                "keyword": f"[{category_name}] 데이터 없음",
                "searchRatio": 0
            })
            rank_counter += 1
            continue

        for item in items[:10]:
            clean_title = item["title"].replace("<b>", "").replace("</b>", "")
            ranking_list.append({
                "rank": rank_counter,
                "keyword": f"[{category_name}] {clean_title}",
                "searchRatio": 0
            })
            rank_counter += 1

    return ranking_list

# 2. 키워드 시장성 분석
def analyze_market(items, keyword, total_results, avg_search_ratio):
    if not items:
        return None

    try:
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
    except Exception as e:
        logger.error(f"❌ 데이터 분석 계산 오류: {e}")
        return None


# =========================================================
# 📤 백엔드 전송 함수
# =========================================================

def send_ranking_to_backend(data):
    url = f"{SPRING_BOOT_API}/market/ranking/receive"
    try:
        logger.info("📤 랭킹 전송 시도 - URL=%s, 항목=%d", url, len(data))
        # ⚠️ (추가) 랭킹 전송 시에도 HTTPS 환경이라면 verify=False 추가를 권장합니다.
        response = requests.post(url, json=data, timeout=5, verify=False)
        response.raise_for_status()
        logger.info("✅ 랭킹 데이터 전송 완료 - status=%s", response.status_code)
    except Exception as e:
        logger.error(f"❌ 랭킹 전송 실패 ({url}): {e}")

def send_analysis_to_backend(data):
    url = f"{SPRING_BOOT_API}/market/analysis"
    try:
        logger.info(f"📤 백엔드로 전송 시도: {url}")
        # ⭐ 최종 수정: SSL 경고 무시 옵션 (verify=False) 유지
        res = requests.post(url, json=data, timeout=5, verify=False)
        res.raise_for_status()
        logger.info("🚀 분석 결과 저장 성공!")
    except Exception as e:
        logger.error(f"❌ 전송 실패: {e}")


# =========================================================
# 🚀 메인 실행 루프
# =========================================================
if __name__ == "__main__":

    # ⭐⭐ 발표를 위해 InsecureRequestWarning 숨기기 ⭐⭐
    try:
        # urllib3 경고를 필터링하기 위해 requests.packages를 통해 import
        from requests.packages.urllib3.exceptions import InsecureRequestWarning
        warnings.filterwarnings("ignore", category=InsecureRequestWarning)
    except ImportError:
        # requests 2.x 버전에서는 requests.packages를 사용합니다.
        pass
    # -------------------------------------------------

    print("\n" + "="*60)
    print("   🚀 SMART SOURCING WORKER (DOCKER/AWS)")
    print(f"   📡 Target Backend: {SPRING_BOOT_API}")
    print(f"   📬 SQS Queue: {QUEUE_URL}")
    print("="*60 + "\n")

    # 1. 시작 시 랭킹 데이터 1회 갱신
    try:
        ranking = fetch_naver_shopping_ranking()
        send_ranking_to_backend(ranking)
    except Exception as e:
        logger.error(f"⚠️ 초기 랭킹 작업 중 에러 발생 (무시하고 진행): {e}")

    logger.info("⚡ SQS 메시지 수신 대기 시작 (Polling)...")

    # 2. 무한 루프 (메시지 처리)
    while True:
        try:
            # 20초 롱 폴링 (비용 절감 및 효율성)
            response = sqs_client.receive_message(
                QueueUrl=QUEUE_URL,
                MaxNumberOfMessages=1,
                WaitTimeSeconds=20
            )

            messages = response.get("Messages", [])
            logger.debug("SQS 폴링 결과 - 메시지 수: %d", len(messages))

            if not messages:
                continue # 메시지 없으면 조용히 다시 대기

            for message in messages:
                receipt_handle = message['ReceiptHandle']
                body_str = message['Body']

                logger.info(f"\n📩 [SQS] 메시지 도착: {body_str}")

                try:
                    # JSON 파싱
                    body = json.loads(body_str)
                    keyword = body.get('keyword')

                    if not keyword:
                        logger.error("❌ 키워드가 없는 잘못된 메시지입니다.")
                        sqs_client.delete_message(QueueUrl=QUEUE_URL, ReceiptHandle=receipt_handle)
                        continue

                    logger.info(f"🔍 분석 시작: [{keyword}]")

                    # 데이터 수집 및 분석
                    shopping = get_naver_shopping_data(keyword)
                    items = shopping.get("items", [])
                    total = shopping.get("total", 0)
                    trend = get_naver_datalab_trend(keyword)

                    result = analyze_market(items, keyword, total, trend)

                    # 백엔드 전송Í
                    if result:
                        logger.debug("분석 결과 payload: %s", result)
                        send_analysis_to_backend(result)
                    else:
                        logger.warning("⚠️ 분석 결과가 유효하지 않아 전송하지 않았습니다.")

                    # 처리 완료 후 메시지 삭제 (필수)
                    sqs_client.delete_message(
                        QueueUrl=QUEUE_URL,
                        ReceiptHandle=receipt_handle
                    )
                    logger.info("🗑️ 메시지 처리 완료 및 삭제됨")

                except json.JSONDecodeError:
                    logger.error("❌ JSON 형식이 아닙니다. 메시지를 삭제합니다.")
                    sqs_client.delete_message(QueueUrl=QUEUE_URL, ReceiptHandle=receipt_handle)
                except Exception as e:
                    logger.error(f"❌ 메시지 처리 중 내부 오류 발생: {e}")
                    # 여기서 메시지를 지우지 않으면, 일정 시간 후 다시 큐에 나타나 재시도됩니다 (Visibility Timeout)

        except KeyboardInterrupt:
            print("\n🛑 워커를 종료합니다.")
            sys.exit(0)
        except Exception as e:
            logger.critical(f"🔥 SQS 연결 또는 네트워크 치명적 오류: {e}")
            time.sleep(10) # 에러 발생 시 10초 대기 후 재시도