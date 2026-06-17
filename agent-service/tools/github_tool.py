"""GitHub 操作工具 — 仓库管理、PR、Issue 等"""

import os
from github import Github, GithubIntegration


class GitHubTool:
    def __init__(self):
        token = os.getenv("GITHUB_TOKEN", "")
        self.client = Github(token) if token else None

    async def list_repos(self, org: str = "") -> list[dict]:
        """列出组织的仓库"""
        if not self.client:
            raise RuntimeError("GitHub Token 未配置")

        if org:
            repos = self.client.get_organization(org).get_repos()
        else:
            repos = self.client.get_user().get_repos()

        return [{"name": r.name, "url": r.html_url, "stars": r.stargazers_count} for r in repos]

    async def get_file_content(self, repo: str, path: str, ref: str = "main") -> str:
        """读取仓库文件内容"""
        if not self.client:
            raise RuntimeError("GitHub Token 未配置")

        repo_obj = self.client.get_repo(repo)
        content = repo_obj.get_contents(path, ref=ref)
        return content.decoded_content.decode("utf-8")

    async def create_pr(
        self,
        repo: str,
        title: str,
        body: str,
        head: str,
        base: str = "main",
    ) -> dict:
        """创建 Pull Request"""
        if not self.client:
            raise RuntimeError("GitHub Token 未配置")

        repo_obj = self.client.get_repo(repo)
        pr = repo_obj.create_pull(title=title, body=body, head=head, base=base)
        return {"url": pr.html_url, "number": pr.number}
