import requests
import time
import hmac
import hashlib
from datetime import datetime


# Binance API credentials
API_KEY = '4PmqYTo2sxRWOze7GSGucVj3xytO2dgAoRfo3bXoIAUHKj9vooLQDnkPjl0ulQ5f'
SECRET_KEY = 'srTePTkgNUhxQPVpeLrTqEC57CQfYoxcOxiTuNLokA1y6Sy4WAMdLW5mf9OuVssA'


BASE_URL = "https://api.binance.com"

def get_received_binance_history():
    endpoint = "/sapi/v1/pay/transactions"
    timestamp = int(time.time() * 1000)
    
    query_params = f"timestamp={timestamp}"
    signature = hmac.new(
        SECRET_KEY.encode('utf-8'), 
        query_params.encode('utf-8'), 
        hashlib.sha256
    ).hexdigest()
    
    url = f"{BASE_URL}{endpoint}?{query_params}&signature={signature}"
    headers = {
        'X-MBX-APIKEY': API_KEY
    }
    
    try:
        response = requests.get(url, headers=headers)
        data = response.json()
        
        if response.status_code == 200:
            print("=" * 60)
            print("       BINANCE PAY HISTORY (ONLY RECEIVED)")
            print("=" * 60)
            
            for tx in data.get('data', []):
                amount_str = tx.get('amount', '0')
                amount_float = float(amount_str)
                
                # যদি অ্যামাউন্ট ০ বা মাইনাস হয় (Send), তাহলে এটি স্কিপ করবে
                if amount_float <= 0:
                    continue
                
                # বেসিক ডেটা এক্সট্র্যাক্ট করা
                order_id = tx.get('orderId', 'N/A')
                uid = tx.get('uid', 'N/A')
                transaction_id = tx.get('transactionId', 'N/A')
                currency = tx.get('currency', 'N/A')
                
                # সময় ঠিক করা
                tx_time_ms = tx.get('transactionTime', 0)
                dt_object = datetime.fromtimestamp(tx_time_ms / 1000.0)
                formatted_time = dt_object.strftime('%Y-%m-%d %H:%M:%S')
                
                # Payer এর তথ্য (কে পাঠিয়েছে)
                payer_info = tx.get('payerInfo', {})
                payer_name = payer_info.get('name', 'N/A')
                payer_binance_id = payer_info.get('binanceId', 'N/A')

                # টার্মিনালে প্রিন্ট করা
                print(f"Status         : 🟢 Received")
                print(f"Order ID       : {order_id}")
                print(f"Transaction ID : {transaction_id}")
                print(f"Amount         : +{amount_str} {currency}")
                print(f"Date & Time    : {formatted_time}")
                print(f"From (Name)    : {payer_name}")
                print(f"Payer ID       : {payer_binance_id}")
                print(f"Your UID       : {uid}")
                print("-" * 60)
                
        else:
            print(f"Error: {data.get('msg')}")
            
    except Exception as e:
        print(f"An error occurred: {e}")

if __name__ == "__main__":
    get_received_binance_history()