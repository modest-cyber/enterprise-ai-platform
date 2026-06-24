import json, requests

resp = requests.post("http://localhost:8000/api/v1/preview",
    json={"documentId": 1, "filePath": "/tmp/test_chinese.docx"})
d = resp.json()

print("success:", d["success"])
print("fileType:", d["fileType"])
print("content_len:", len(d["content"]))
lines = d["content"].split("\n")
print("lines:", len(lines))
for i, line in enumerate(lines[:5]):
    print(f"  L{i}: {repr(line[:60])}")
print("metadata:", d.get("metadata", {}).get("loaderType"))