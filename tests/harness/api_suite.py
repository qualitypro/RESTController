#!/usr/bin/env python3
"""Black-box integration suite for the packaged RESTController server."""
import argparse
import json
import sys
import urllib.error
import urllib.request

def request(base, method, path, payload=None, token=None, content_type="application/json"):
    headers = {}
    data = None
    if payload is not None:
        data = payload.encode("utf-8")
        headers["Content-Type"] = content_type
    if token is not None:
        headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request(base + path, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=8) as res:
            return res.status, res.read().decode(), res.headers
    except urllib.error.HTTPError as err:
        return err.code, err.read().decode(), err.headers

def check(condition, message):
    if not condition:
        raise AssertionError(message)
    print("PASS " + message, flush=True)

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--base", required=True)
    parser.add_argument("--token", required=True)
    parser.add_argument("--phase", choices=["initial", "restart"], default="initial")
    args = parser.parse_args()
    base = args.base.rstrip("/")
    key = args.token
    status, body, headers = request(base, "GET", "/health")
    check(status == 200 and json.loads(body)["status"] == "ok", "health endpoint")
    check(bool(headers.get("X-Request-ID")), "correlation ID")
    check(headers.get("Cache-Control") == "no-store", "no-store response header")
    status, body, _ = request(base, "GET", "/api/people")
    check(status == 401 and "Unauthorized" in body, "anonymous requests denied")
    check(request(base, "GET", "/api/people", token="wrong")[0] == 401, "wrong token denied")
    check(request(base, "POST", "/api/echo", "{}", token="wrong")[0] == 401,
          "echo endpoint protected")
    if args.phase == "restart":
        status, body, _ = request(base, "GET", "/api/people/2", token=key)
        check(status == 200 and json.loads(body)["lastName"] == "Hopper",
              "person survives restart")
        check(request(base, "GET", "/api/people/1", token=key)[0] == 404,
              "deleted person remains deleted")
        status, body, _ = request(base, "POST", "/api/people",
                                  '{"firstName":"Katherine","lastName":"Johnson"}', key)
        check(status == 201 and json.loads(body)["id"] == 3, "IDs do not reset on restart")
        return

    check(request(base, "GET", "/api/people", token=key)[0] == 200, "authorized listing")
    check(request(base, "GET", "/api/people/abc", token=key)[0] == 404, "invalid ID")
    check(request(base, "GET", "/api/people/999", token=key)[0] == 404, "missing record")
    check(request(base, "POST", "/api/people", "{broken", key)[0] == 400, "malformed JSON")
    check(request(base, "POST", "/api/people", '{"firstName":""}', key)[0] == 422,
          "required fields")
    check(request(base, "POST", "/api/people", '{"firstName":"Ada","lastName":"L","admin":true}', key)[0] == 422,
          "unknown fields rejected")
    check(request(base, "POST", "/api/people", "hello", key, "text/plain")[0] == 415,
          "content type validation")
    check(request(base, "POST", "/api/people", "x" * (1024 * 1024 + 1), key)[0] == 413,
          "payload size limit")
    check(request(base, "PATCH", "/api/people", "{}", key)[0] == 405,
          "collection method validation")
    status, body, headers = request(base, "POST", "/api/people",
                                    '{"firstName":"Ada","lastName":"Lovelace"}', key)
    check(status == 201 and json.loads(body)["id"] == 1, "create record")
    check(headers.get("Location") == "/api/people/1", "location header")
    status, body, _ = request(base, "POST", "/api/people",
                              '{"firstName":"Grace","lastName":"Hopper"}', key)
    check(status == 201 and json.loads(body)["id"] == 2, "second record")
    status, body, _ = request(base, "GET", "/api/people", token=key)
    check(status == 200 and len(json.loads(body)) == 2, "list records")
    status, body, _ = request(base, "GET", "/api/people/1", token=key)
    check(status == 200 and json.loads(body)["firstName"] == "Ada", "read record")
    status, body, _ = request(base, "PUT", "/api/people/1",
                              '{"firstName":"Augusta","lastName":"Lovelace"}', key)
    check(status == 200 and json.loads(body)["firstName"] == "Augusta", "replace record")
    check(request(base, "PUT", "/api/people/999",
                  '{"firstName":"Missing","lastName":"Person"}', key)[0] == 404,
          "replace missing record")
    check(request(base, "PATCH", "/api/people/1", "{}", key)[0] == 405,
          "item method validation")
    check(request(base, "DELETE", "/api/people/1", token=key)[0] == 204, "delete record")
    check(request(base, "DELETE", "/api/people/1", token=key)[0] == 404,
          "repeat deletion returns not found")
    status, body, _ = request(base, "POST", "/api/echo", '{"hello":"world"}', key)
    check(status == 200 and json.loads(body)["hello"] == "world", "echo JSON")
    check(request(base, "GET", "/api/echo", token=key)[0] == 405, "echo method validation")
    check(request(base, "GET", "/health/extra")[0] == 404, "unknown health route")
    print("ALL BLACK-BOX TESTS PASSED", flush=True)

if __name__ == "__main__":
    try:
        main()
    except Exception as exc:
        print("FAIL " + str(exc), file=sys.stderr, flush=True)
        sys.exit(1)
