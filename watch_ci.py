import json, time, urllib.request

TOKEN = open(r"C:\Users\Ma\.github_token", encoding="utf-8").read().strip()
REPO = "17mohammad1718-png/Android-App"
HDR = {"Authorization": "token " + TOKEN, "User-Agent": "watcher"}

def api(path):
    req = urllib.request.Request("https://api.github.com" + path, headers=HDR)
    return json.load(urllib.request.urlopen(req))

deadline = time.time() + 35 * 60
run_id = None
while time.time() < deadline:
    try:
        runs = api("/repos/%s/actions/runs?per_page=3" % REPO).get("workflow_runs", [])
        if runs:
            r = runs[0]
            run_id = r["id"]
            print("run %s | status=%s | conclusion=%s" % (run_id, r["status"], r["conclusion"]))
            if r["status"] == "completed":
                print("FINAL:", r["conclusion"])
                print("URL:", r["html_url"])
                break
    except Exception as e:
        print("poll error:", e)
    time.sleep(30)
else:
    print("TIMEOUT waiting for CI")
