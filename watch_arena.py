import json, time, urllib.request

TOKEN = open(r"C:\Users\Ma\.github_token", encoding="utf-8").read().strip()
REPO = "17mohammad1718-png/Android-App"
HDR = {"Authorization": "token " + TOKEN, "User-Agent": "watcher"}

def api(path):
    req = urllib.request.Request("https://api.github.com" + path, headers=HDR)
    return json.load(urllib.request.urlopen(req))

deadline = time.time() + 40 * 60
seen_branches = {"arena/01a01f99-android-app", "arena/01a0201b-android-app", "main"}
while time.time() < deadline:
    try:
        branches = {b["name"] for b in api("/repos/%s/branches?per_page=100" % REPO)}
        new_b = branches - seen_branches
        prs = api("/repos/%s/pulls?state=open" % REPO)
        wf = api("/repos/%s/contents/.github/workflows/android-ci.yml" % REPO)
        has_wf_on_main = isinstance(wf, dict) and wf.get("name") == "android-ci.yml"
        if new_b or prs or has_wf_on_main:
            print("ACTIVITY DETECTED")
            print("new branches:", sorted(new_b))
            print("open PRs:", [(p["number"], p["title"]) for p in prs])
            print("workflow on main:", has_wf_on_main)
            break
    except urllib.error.HTTPError as e:
        if e.code != 404:
            print("http error:", e.code)
    except Exception as e:
        print("poll error:", e)
    time.sleep(45)
else:
    print("TIMEOUT: no activity in 40 minutes")
