import requests

# 1. Login to get token
r = requests.post("http://localhost:8080/api/auth/login", json={"email": "kevis206400307170@gmail.com", "password": "password"})
print(r.status_code)
# Actually I don't know the password. Let's just create a test NGO or modify DB directly.
