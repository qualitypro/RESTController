#!/usr/bin/env python3
"""Concurrent HTTP reliability checks against an isolated RESTController instance."""
import argparse
import concurrent.futures
import json
import urllib.error
import urllib.request

def call(base, token, method, path, body=None):
    headers = {"Authorization": "Bearer " + token}
    data = None
    if body is not None:
        data = json.dumps(body).encode()
        headers["Content-Type"] = "application/json"
    req = urllib.request.Request(base + path, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=20) as response:
            raw = response.read()
            return response.status, json.loads(raw) if raw else None
    except urllib.error.HTTPError as error:
        raw = error.read()
        return error.code, json.loads(raw) if raw else None

def require(condition, message):
    if not condition:
        raise AssertionError(message)
    print("PASS " + message, flush=True)

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--base", required=True)
    parser.add_argument("--token", required=True)
    parser.add_argument("--count", type=int, default=64)
    parser.add_argument("--workers", type=int, default=12)
    parser.add_argument("--phase", choices=["initial", "restart"], default="initial")
    args = parser.parse_args()
    if not 2 <= args.count <= 500 or not 1 <= args.workers <= 64:
        parser.error("count must be 2..500 and workers 1..64")
    base = args.base.rstrip("/")
    token = args.token
    status, records = call(base, token, "GET", "/api/people")
    require(status == 200 and isinstance(records, list), "list available")
    if args.phase == "restart":
        expected = args.count - (args.count // 2)
        require(len(records) == expected, "surviving records retained after restart")
        ids = [record["id"] for record in records]
        require(len(ids) == len(set(ids)), "unique IDs retained after restart")
        status, created = call(base, token, "POST", "/api/people",
                               {"firstName": "After", "lastName": "Restart"})
        require(status == 201 and created["id"] > args.count,
                "monotonic ID after restart")
        return

    require(len(records) == 0, "isolated store starts empty")
    def create(i):
        return call(base, token, "POST", "/api/people",
                    {"firstName": "Concurrent", "lastName": "Person" + str(i)})
    with concurrent.futures.ThreadPoolExecutor(max_workers=args.workers) as pool:
        created = list(pool.map(create, range(args.count)))
    require(all(status == 201 for status, _ in created), "all concurrent creates succeed")
    ids = [record["id"] for _, record in created]
    require(len(set(ids)) == args.count, "no duplicate IDs under concurrent creates")
    status, records = call(base, token, "GET", "/api/people")
    require(status == 200 and len(records) == args.count, "no lost creates")

    def update(i):
        return call(base, token, "PUT", "/api/people/" + str(ids[i]),
                    {"firstName": "Updated", "lastName": "Person" + str(i)})
    with concurrent.futures.ThreadPoolExecutor(max_workers=args.workers) as pool:
        updated = list(pool.map(update, range(args.count)))
    require(all(status == 200 for status, _ in updated), "all concurrent updates succeed")
    status, records = call(base, token, "GET", "/api/people")
    require(status == 200 and all(r["firstName"] == "Updated" for r in records),
            "all updates visible")

    remove_ids = ids[:args.count // 2]
    def delete(id_):
        return call(base, token, "DELETE", "/api/people/" + str(id_))
    with concurrent.futures.ThreadPoolExecutor(max_workers=args.workers) as pool:
        deleted = list(pool.map(delete, remove_ids))
    require(all(status == 204 for status, _ in deleted), "concurrent deletes succeed")
    status, records = call(base, token, "GET", "/api/people")
    require(status == 200 and len(records) == args.count - len(remove_ids),
            "concurrent deletes do not lose surviving records")
    require(not (set(remove_ids) & {r["id"] for r in records}),
            "deleted records absent")
    print("ALL CONCURRENCY CHECKS PASSED", flush=True)

if __name__ == "__main__":
    main()
