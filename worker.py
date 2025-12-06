import boto3
import requests
import json
import statistics
import logging
import sys
from botocore.exceptions import ClientError

# 1. 로깅 설정
logging.basicConfig(
    level=logging.INFO,
    format='[%(asctime)s] %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(sys.stdout)
    ]
)
logger = logging.getLogger()

# AWS 설정
sqs = boto3.client('sqs', region_name='us-east-1')
QUEUE_URL = "https://sqs.us-east-1.amazonaws.com/730335221432/smart-sourcing-queue"

# 네이버 API 설정
NAVER_CLIENT_ID = "I1uLlyo_ne_BHszaVd3R"
NAVER_CLIENT_SECRET = "swMHAs3qpq"

# Spring Boot 서버 주소
SPRING_BOOT_API_URL = "http://localhost:8080/api/market/analysis"

def get_naver_shopping_data(keyword):
    if keyword:
        keyword = str(keyword).strip()

    if not keyword:
        return []

    url = "https://openapi.naver.com/v1/search/shop.json"
    headers = {
        "X-Naver-Client-Id": NAVER_CLIENT_ID,
        "X-Naver-Client-Secret": NAVER_CLIENT_SECRET
    }
    params = {"query": keyword, "display": 100, "sort": "sim"}

    try:
        response = requests.get(url, headers=headers, params=params)
        response.raise_for_status()
        return response.json().get('items', [])
    except requests.exceptions.RequestException as e:
        logger.error(f"네이버 API 호출 중 에러 발생: {e}")
        return []

def analyze_market(items):
    if not items:
        return None

    prices = [int(item['lprice']) for item in items]
    avg_price = statistics.mean(prices)
    min_price = min(prices)
    top_title = items[0]['title'].replace('<b>', '').replace('</b>', '')

    return {
        "keyword": items[0].get('category1', 'Unknown'),
        "average_price": int(avg_price),
        "lowest_price": min_price,
        "sample_count": len(items),
        "top_item_name": top_title
    }

def send_to_backend(data):
    try:
        headers = {'Content-Type': 'application/json'}
        response = requests.post(SPRING_BOOT_API_URL, json=data, headers=headers)

        if response.status_code == 200 or response.status_code == 201:
            logger.info(f"🚀 Spring Boot로 데이터 전송 성공! [Status: {response.status_code}]")
        else:
            logger.error(f"❌ 데이터 전송 실패. 서버 응답: {response.status_code} - {response.text}")

    except requests.exceptions.RequestException as e:
        logger.error(f"❌ Spring Boot 서버 연결 실패: {e}")

def process_messages():
    logger.info(f"🚀 워커(Worker)가 시작되었습니다. 대기열 감시 중... [{QUEUE_URL}]")

    while True:
        try:
            # 1. SQS 메시지 수신 (Long Polling)
            response = sqs.receive_message(
                QueueUrl=QUEUE_URL,
                MaxNumberOfMessages=1,
                WaitTimeSeconds=20
            )

            if 'Messages' in response:
                message = response['Messages'][0]
                receipt_handle = message['ReceiptHandle']
                raw_body = message['Body']

                # 키워드 파싱 및 세탁
                keyword = "알수없음"
                try:
                    body = json.loads(raw_body)
                    if isinstance(body, dict):
                        keyword = body.get('keyword', raw_body)
                    else:
                        keyword = str(body)
                except:
                    keyword = raw_body

                keyword = str(keyword).strip()
                logger.info(f"📥 메시지 수신됨! 추출된 키워드: [{keyword}]")

                # 데이터 수집 및 분석
                items = get_naver_shopping_data(keyword)

                if items:
                    result = analyze_market(items)
                    result['search_keyword'] = keyword

                    logger.info(f"✅ 분석 완료: {json.dumps(result, ensure_ascii=False)}")

                    # Spring Boot로 전송
                    send_to_backend(result)
                else:
                    logger.warning(f"⚠️ '{keyword}'에 대한 검색 결과가 없습니다.")

                # 메시지 삭제
                sqs.delete_message(QueueUrl=QUEUE_URL, ReceiptHandle=receipt_handle)
                logger.info("🗑️ 메시지 삭제 완료 (처리 끝)\n")

            else:
                pass

        except ClientError as e:
            logger.error(f"AWS SQS 에러 발생: {e}")
        except Exception as e:
            logger.error(f"알 수 없는 에러 발생: {e}")

if __name__ == "__main__":
    process_messages()
