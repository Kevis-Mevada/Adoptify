import jwt
import time
import requests

secret = 'dGhpcyBpcyBhIHZlcnkgc2VjdXJlIGFuZCBsb25nIHNlY3JldCBrZXkgZm9yIGp3dA=='
import base64
secret_bytes = base64.b64decode(secret)

payload = {
    "sub": "aastha@gmail.com", # Needs to be an existing NGO email
    "iat": int(time.time()),
    "exp": int(time.time()) + 86400
}

# Need to find an NGO email from db
import pymysql
conn = pymysql.connect(host='localhost', user='root', password='Kevis@123', database='adoptify_db')
cur = conn.cursor(pymysql.cursors.DictCursor)
cur.execute("SELECT email FROM users WHERE role='NGO' LIMIT 1")
ngo = cur.fetchone()

if ngo:
    payload["sub"] = ngo["email"]
    token = jwt.encode(payload, secret_bytes, algorithm="HS384")
    print(token)
    r = requests.put("http://localhost:8080/api/rescue/1/status", json={"status": "EN_ROUTE"}, headers={"Authorization": f"Bearer {token}"})
    print(r.status_code)
    print(r.text)
else:
    print("No NGO found")
