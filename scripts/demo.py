"""Create an order and verify its status with Python's standard library."""
import json
import time
from urllib.error import URLError
from urllib.request import Request, urlopen

BASE = "http://127.0.0.1:18080"


def json_request(path, data=None):
    body = json.dumps(data).encode() if data is not None else None
    request = Request(BASE + path, data=body, headers={"Content-Type": "application/json"})
    with urlopen(request, timeout=5) as response:
        return json.load(response)


def main():
    for _ in range(90):
        try:
            if json_request("/actuator/health")["status"] == "UP":
                break
        except URLError:
            pass
        time.sleep(1)
    else:
        raise SystemExit("App did not become healthy. Check docker compose logs app.")

    order = json_request("/orders", {"sku": "BOOK-1", "quantity": 2})
    print("Created:", order)
    if order["status"] != "PENDING":
        raise SystemExit("Expected PENDING on creation.")
    for _ in range(30):
        result = json_request("/orders/" + order["id"])
        if result["status"] == "PROCESSED" and result["id"] == order["id"]:
            print("Processed:", result)
            return
        time.sleep(1)
    raise SystemExit("Order is still pending. Check docker compose logs kafka app.")


if __name__ == "__main__":
    main()
