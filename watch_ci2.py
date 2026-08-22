import json, time, urllib.request

TOKEN = open(r"C:\Users\Ma\.github_token", encoding="utf-8").read().strip()
REPO = "17mohammad1718-png/Android-App"
HDR = {"Authorization": "token " + TOKEN, "User-Agent": "watcher"}

def api(path):
    req = urllib.request.Request("https://api.github.com" + path, headers=HDR)
    return json.load(urllib.request.urlopen(req))

deadline = time.time() + 40 * 60
# wait for the run triggered by commit 51a246d (the newest push)
while time.time() < deadline:
    try:
        runs = api("/repos/%s/actions/runs?per_page=1" % REPO).get("workflow_runs", [])
        if runs:
            r = runs[0]
            head = r.get("head_sha", "")[:7]
            print("run %s | head=%s | status=%s | conclusion=%s" % (r["id"], head, r["status"], r["conclusion"]))
            if head == "51a246d" and r["status"] == "completed":
                print("FINAL:", r["conclusion"])
                break
    except Exception as e:
        print("poll error:", e)
    time.sleep(30)
else:
    print("TIMEOUT waiting for CI")
