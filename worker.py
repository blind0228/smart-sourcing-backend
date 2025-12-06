import boto3
import json
import time
import pymysql
from botocore.exceptions import ClientError

# --- [설정 정보] ---
# 1. 아까 복사한 SQS URL 붙여넣기
SQS_QUEUE_URL = "https://sqs.us-east-1.amazonaws.com/730335221432/smart-sourcing-queue"

# 2. RDS 정보 입력 (Spring Boot랑 똑같이)
DB_HOST = "database-1.cnwayga6k6j6.us-east-1.rds.amazonaws.com"
DB_USER = "admin"
DB_PASSWORD = "wjddnjswns"
DB_NAME = "smart_sourcing_db"

# 3. AWS 클라이언트 연결 (Learner Lab이라서 권한은 자동 처리됨)
sqs = boto3.client('sqs', region_name='us-east-1')
translate = boto3.client('translate', region_name='us-east-1')

def get_db_connection():
    return pymysql.connect(host=DB_HOST, user=DB_USER, password=DB_PASSWORD, db=DB_NAME, charset='utf8mb4', cursorclass=pymysql.cursors.DictCursor)

def process_message(message_body):
    print(f"🕷️ 작업 시작: {message_body}")
    data = json.loads(message_body)
    keyword = data.get('keyword', 'default')

    # 1. (가짜) 크롤링
    print(f"   Searching 1688 for: {keyword}...")
    time.sleep(1) 
    
    # 2. 번역 (AWS Translate 대신 임시 처리)
    # AWS Academy 권한 문제로 Translate를 사용할 수 없으므로,
    # 단순히 'Translated_' 라는 말을 앞에 붙여서 테스트합니다.
    original_text = f"{keyword} 的优质产品"
    price_cny = 100
    
    print("   Translating (Simulated)...")
    # result = translate.translate_text(...)  <-- 이 부분이 에러 원인이라 주석 처리
    translated_text = f"한국어로 번역된_{keyword}_상품" # 가짜 번역 결과
    
    # 3. DB 저장
    print(f"   Saving to DB: {translated_text}")
    
    try:
        conn = get_db_connection()
        with conn.cursor() as cursor:
            # 상태를 'COMPLETED'로 저장
            sql = "INSERT INTO products (name, price, status) VALUES (%s, %s, %s)"
            cursor.execute(sql, (translated_text, price_cny * 190, 'COMPLETED'))
        conn.commit()
        conn.close()
        print("✅ DB 저장 완료! 작업 끝!")
    except Exception as db_err:
        print(f"❌ DB 저장 실패: {db_err}")

def run_worker():
    print("🚀 Python Worker 가동! SQS 감시 중...")
    while True:
        try:
            # SQS에서 메시지 가져오기 (Long Polling)
            response = sqs.receive_message(
                QueueUrl=SQS_QUEUE_URL,
                MaxNumberOfMessages=1,
                WaitTimeSeconds=10
            )

            if 'Messages' in response:
                for msg in response['Messages']:
                    receipt_handle = msg['ReceiptHandle']
                    body = msg['Body']
                    
                    try:
                        process_message(body)
                        # 성공하면 큐에서 삭제
                        sqs.delete_message(QueueUrl=SQS_QUEUE_URL, ReceiptHandle=receipt_handle)
                    except Exception as e:
                        print(f"❌ 처리 실패: {e}")
            else:
                print(".", end="", flush=True) # 대기 중 표시

        except Exception as e:
            print(f"Error: {e}")
            time.sleep(5)

if __name__ == "__main__":
    run_worker()