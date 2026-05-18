import requests
import pymysql

# Get NGO user
conn = pymysql.connect(host='localhost', user='root', password='Kevis@123', database='adoptify_db')
cur = conn.cursor(pymysql.cursors.DictCursor)
cur.execute("SELECT email, password FROM users WHERE role='NGO' LIMIT 1")
ngo = cur.fetchone()

# Get reporter
cur.execute("SELECT email, password FROM users WHERE role='USER' LIMIT 1")
reporter = cur.fetchone()

cur.execute("SELECT id FROM rescue_reports LIMIT 1")
report_id = cur.fetchone()
if not report_id:
    report_id = 1
else:
    report_id = report_id['id']

if ngo:
    print(f"Testing with NGO: {ngo['email']}")
    # Assume password is Password123! or something, we can just fetch a token using direct sql if possible? No, we need to login. Let's just do a POST to /api/auth/login if we know the password. Or generate a token locally in Python.
