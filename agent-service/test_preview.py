import json, requests

resp = requests.post("http://localhost:8000/api/v1/preview",
    json={"documentId": 1, "filePath": "/tmp/test_chinese.docx"})
data = resp.json()

print("HTTP Status:", resp.status_code)
print("success:", data.get("success"))
print("fileType:", data.get("fileType"))
print("fileName:", data.get("fileName"))
print("content length:", len(data.get("content", "")))
print("content preview (200 chars):")
print(repr(data.get("content", "")[:200]))
print()
print("metadata:", json.dumps(data.get("metadata", {}), ensure_ascii=False))