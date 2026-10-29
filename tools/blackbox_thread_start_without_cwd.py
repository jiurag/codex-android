#!/usr/bin/env python3
"""Verify that Codex app-server can start a thread without an explicit cwd."""

from __future__ import annotations

import argparse
import json
import queue
import subprocess
import threading
import time
from pathlib import Path
from typing import Any


class AppServerClient:
    def __init__(self, codex: str, cwd: Path) -> None:
        self.process = subprocess.Popen(
            [codex, "app-server", "--listen", "stdio://"],
            cwd=cwd,
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            encoding="utf-8",
            bufsize=1,
        )
        self.messages: queue.Queue[dict[str, Any]] = queue.Queue()
        self.stderr: list[str] = []
        self.next_id = 1
        self.reader = threading.Thread(target=self._read_stdout, daemon=True)
        self.error_reader = threading.Thread(target=self._read_stderr, daemon=True)
        self.reader.start()
        self.error_reader.start()

    def _read_stdout(self) -> None:
        assert self.process.stdout is not None
        for line in self.process.stdout:
            if line.strip():
                self.messages.put(json.loads(line))

    def _read_stderr(self) -> None:
        assert self.process.stderr is not None
        for line in self.process.stderr:
            if line.strip():
                self.stderr.append(line.rstrip())

    def send(self, message: dict[str, Any]) -> None:
        assert self.process.stdin is not None
        self.process.stdin.write(
            json.dumps(message, ensure_ascii=False, separators=(",", ":")) + "\n"
        )
        self.process.stdin.flush()

    def request(
        self,
        method: str,
        params: dict[str, Any] | None = None,
        timeout: float = 60,
    ) -> dict[str, Any]:
        request_id = self.next_id
        self.next_id += 1
        self.send({"id": request_id, "method": method, "params": params or {}})
        deadline = time.monotonic() + timeout
        while time.monotonic() < deadline:
            try:
                message = self.messages.get(timeout=max(0.0, deadline - time.monotonic()))
            except queue.Empty as error:
                raise TimeoutError(f"Timed out waiting for {method}") from error
            if message.get("id") == request_id:
                if "error" in message:
                    raise RuntimeError(f"{method}: {message['error']}")
                result = message.get("result")
                return result if isinstance(result, dict) else {"value": result}
            request_from_server = message.get("id")
            if request_from_server is not None:
                self.send({"id": request_from_server, "result": {"decision": "decline"}})
        raise TimeoutError(f"Timed out waiting for {method}")

    def close(self) -> None:
        if self.process.poll() is None:
            self.process.terminate()
            try:
                self.process.wait(timeout=5)
            except subprocess.TimeoutExpired:
                self.process.kill()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--codex", default="codex")
    parser.add_argument("--cwd", default="/root")
    parser.add_argument("--timeout", type=float, default=60)
    args = parser.parse_args()

    client = AppServerClient(args.codex, Path(args.cwd))
    thread_id: str | None = None
    summary: dict[str, Any] = {}
    try:
        summary["initialize"] = client.request(
            "initialize",
            {
                "clientInfo": {
                    "name": "codex_remote_new_thread_blackbox",
                    "title": "Codex",
                    "version": "blackbox",
                },
                "capabilities": {
                    "experimentalApi": True,
                    "requestAttestation": False,
                },
            },
            timeout=args.timeout,
        )
        client.send({"method": "initialized", "params": {}})
        summary["account"] = client.request(
            "account/read",
            {"refreshToken": False},
            timeout=args.timeout,
        )
        models = client.request("model/list", {"limit": 100}, timeout=args.timeout).get("data")
        model = next(
            (entry for entry in models or [] if isinstance(entry, dict) and entry.get("isDefault")),
            (models or [None])[0],
        )
        model_id = model.get("model") or model.get("id")
        if not isinstance(model_id, str) or not model_id:
            raise RuntimeError("model/list returned no usable model")
        started = client.request(
            "thread/start",
            {
                "model": model_id,
                "approvalPolicy": "on-request",
                "approvalsReviewer": "user",
                "sandbox": "workspace-write",
            },
            timeout=args.timeout,
        )
        thread = started.get("thread")
        thread_id = thread.get("id") if isinstance(thread, dict) else None
        if not isinstance(thread_id, str) or not thread_id:
            raise RuntimeError("thread/start returned no thread.id")
        summary["thread_start_without_cwd"] = {
            "thread_id": thread_id,
            "request_cwd_omitted": True,
            "response_cwd": started.get("cwd"),
            "model": started.get("model"),
            "model_provider": started.get("modelProvider"),
        }
        listed = client.request(
            "thread/list",
            {
                "limit": 100,
                "sortKey": "updated_at",
                "sortDirection": "desc",
                "archived": False,
                "sourceKinds": [],
            },
            timeout=args.timeout,
        )
        listed_ids = [
            entry.get("id")
            for entry in listed.get("data") or []
            if isinstance(entry, dict) and isinstance(entry.get("id"), str)
        ]
        summary["thread_list_immediately_after_start"] = {
            "returned_count": len(listed_ids),
            "contains_started_thread": thread_id in listed_ids,
            "first_ids": listed_ids[:10],
        }
        summary["passed"] = True
    finally:
        if thread_id:
            try:
                client.request(
                    "thread/delete",
                    {"threadId": thread_id},
                    timeout=args.timeout,
                )
                summary["thread_deleted"] = True
            except Exception as error:
                summary["thread_deleted"] = False
                summary["thread_delete_error"] = f"{type(error).__name__}: {error}"
        summary["stderr_tail"] = client.stderr[-20:]
        client.close()
        print(json.dumps(summary, ensure_ascii=False, indent=2))
        if not summary.get("passed"):
            raise SystemExit(1)


if __name__ == "__main__":
    main()
