#!/usr/bin/env python3
"""Black-box smoke test for the Codex app-server protocol used by the app."""

from __future__ import annotations

import argparse
import json
import queue
import shutil
import subprocess
import tempfile
import threading
import time
from pathlib import Path
from typing import Any


class RpcFailure(RuntimeError):
    pass


class AppServerClient:
    def __init__(self, command: list[str], cwd: Path) -> None:
        self.process = subprocess.Popen(
            command,
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
        self.notifications: list[dict[str, Any]] = []
        self.server_requests: list[dict[str, Any]] = []
        self.pending_server_requests: list[dict[str, Any]] = []
        self.approval_decisions: list[dict[str, Any]] = []
        self.defer_server_requests = False
        self.reader = threading.Thread(target=self._read_stdout, daemon=True)
        self.error_reader = threading.Thread(target=self._read_stderr, daemon=True)
        self.reader.start()
        self.error_reader.start()

    def _read_stdout(self) -> None:
        assert self.process.stdout is not None
        for line in self.process.stdout:
            if not line.strip():
                continue
            try:
                self.messages.put(json.loads(line))
            except json.JSONDecodeError as error:
                self.messages.put({"invalid_output": line.rstrip(), "error": str(error)})

    def _read_stderr(self) -> None:
        assert self.process.stderr is not None
        for line in self.process.stderr:
            if line.strip():
                self.stderr.append(line.rstrip())

    def send(self, message: dict[str, Any]) -> None:
        assert self.process.stdin is not None
        self.process.stdin.write(json.dumps(message, ensure_ascii=False, separators=(",", ":")) + "\n")
        self.process.stdin.flush()

    def request(
        self,
        method: str,
        params: dict[str, Any] | None = None,
        timeout: float = 120,
    ) -> dict[str, Any]:
        request_id = self.next_id
        self.next_id += 1
        self.send({"id": request_id, "method": method, "params": params or {}})
        deadline = time.monotonic() + timeout
        while time.monotonic() < deadline:
            message = self.next_message(deadline)
            if message.get("id") == request_id and ("result" in message or "error" in message):
                if "error" in message:
                    raise RpcFailure(f"{method}: {message['error']}")
                result = message.get("result")
                return result if isinstance(result, dict) else {"value": result}
            self.handle_incoming(message)
        raise TimeoutError(f"Timed out waiting for response to {method}")

    def next_message(self, deadline: float) -> dict[str, Any]:
        remaining = max(0.0, deadline - time.monotonic())
        try:
            return self.messages.get(timeout=remaining)
        except queue.Empty as error:
            raise TimeoutError("Timed out waiting for app-server output") from error

    def handle_incoming(self, message: dict[str, Any]) -> None:
        if "invalid_output" in message:
            raise RpcFailure(f"Invalid app-server output: {message['invalid_output']}")
        method = message.get("method")
        request_id = message.get("id")
        if method and request_id is not None:
            self.server_requests.append(message)
            if self.defer_server_requests:
                self.pending_server_requests.append(message)
            else:
                self.decline_server_request(message)
        elif method:
            self.notifications.append(message)

    def decline_server_request(self, message: dict[str, Any]) -> None:
        method = str(message.get("method", ""))
        if method == "item/permissions/requestApproval":
            result: dict[str, Any] = {"permissions": {}, "scope": "turn"}
        else:
            result = {"decision": "decline"}
        self.approval_decisions.append({"method": method, "result": result})
        self.send({"id": message["id"], "result": result})

    def wait_for_pending_request(
        self,
        thread_id: str,
        turn_id: str,
        timeout: float,
    ) -> dict[str, Any]:
        def matches(message: dict[str, Any]) -> bool:
            params = message.get("params") if isinstance(message.get("params"), dict) else {}
            return params.get("threadId") == thread_id and params.get("turnId") == turn_id

        for message in self.pending_server_requests:
            if matches(message):
                return message

        deadline = time.monotonic() + timeout
        while time.monotonic() < deadline:
            message = self.next_message(deadline)
            if message.get("method") and message.get("id") is not None and matches(message):
                self.handle_incoming(message)
                return message
            self.handle_incoming(message)
        raise TimeoutError(f"Timed out waiting for a server request in turn {turn_id}")

    def decline_pending_requests(self) -> None:
        pending = list(self.pending_server_requests)
        self.pending_server_requests.clear()
        for message in pending:
            self.decline_server_request(message)

    def wait_for_turn_complete(
        self,
        thread_id: str,
        turn_id: str,
        timeout: float,
    ) -> dict[str, Any]:
        deadline = time.monotonic() + timeout
        while time.monotonic() < deadline:
            message = self.next_message(deadline)
            method = message.get("method")
            params = message.get("params") if isinstance(message.get("params"), dict) else {}
            if method == "turn/completed":
                turn = params.get("turn") if isinstance(params.get("turn"), dict) else {}
                if params.get("threadId") == thread_id and turn.get("id") == turn_id:
                    return params
            self.handle_incoming(message)
        raise TimeoutError(f"Timed out waiting for turn {turn_id} to complete")

    def close(self) -> None:
        if self.process.poll() is None:
            try:
                assert self.process.stdin is not None
                self.process.stdin.close()
            except OSError:
                pass
            try:
                self.process.wait(timeout=3)
            except subprocess.TimeoutExpired:
                self.process.terminate()
                try:
                    self.process.wait(timeout=3)
                except subprocess.TimeoutExpired:
                    self.process.kill()


def choose_model(models: list[dict[str, Any]]) -> dict[str, Any]:
    for preferred in ("deepseek-v4-flash", "deepseek-v4-pro"):
        for model in models:
            if model.get("model") == preferred or model.get("id") == preferred:
                return model
    for model in models:
        if model.get("isDefault") is True:
            return model
    if not models:
        raise RpcFailure("model/list returned no models")
    return models[0]


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--codex", default="codex")
    parser.add_argument("--timeout", type=float, default=180)
    args = parser.parse_args()

    work_dir = Path(tempfile.mkdtemp(prefix="codex-remote-blackbox-"))
    client = AppServerClient([args.codex, "app-server", "--listen", "stdio://"], work_dir)
    summary: dict[str, Any] = {
        "work_dir": str(work_dir),
        "checks": {},
        "failures": [],
        "server_requests": [],
        "notifications": [],
        "cleanup": {},
    }
    thread_id: str | None = None
    try:
        summary["checks"]["initialize"] = client.request(
            "initialize",
            {
                "clientInfo": {
                    "name": "codex_remote_android_zh_blackbox",
                    "title": "Codex Remote Android 简体中文黑盒测试",
                    "version": "0.2.0-zh.2",
                },
                "capabilities": {"experimentalApi": True, "requestAttestation": False},
            },
        )
        client.send({"method": "initialized", "params": {}})

        account = client.request("account/read", {"refreshToken": False})
        models_result = client.request("model/list", {"limit": 100})
        models = models_result.get("data") if isinstance(models_result.get("data"), list) else []
        selected_model = choose_model(models)
        model_id = selected_model.get("model") or selected_model.get("id")
        if not isinstance(model_id, str) or not model_id:
            raise RpcFailure("Selected model has no id")

        threads = client.request(
            "thread/list",
            {
                "limit": 100,
                "sortKey": "updated_at",
                "sortDirection": "desc",
                "archived": False,
                "sourceKinds": [],
            },
        )
        collaboration_modes = client.request("collaborationMode/list")
        permissions = client.request(
            "permissionProfile/list",
            {"limit": 100, "cwd": str(work_dir)},
        )
        skills = client.request("skills/list", {"cwds": [str(work_dir)]})
        plugins = client.request("plugin/installed", {"cwds": [str(work_dir)]})
        mcp = client.request(
            "mcpServerStatus/list",
            {"limit": 100, "detail": "toolsAndAuthOnly"},
        )
        directory = client.request("fs/readDirectory", {"path": str(work_dir)})
        summary["checks"]["read_only"] = {
            "account_requires_openai_auth": account.get("requiresOpenaiAuth"),
            "model_count": len(models),
            "selected_model": model_id,
            "models": [
                {
                    "id": model.get("model") or model.get("id"),
                    "display_name": model.get("displayName"),
                    "is_default": model.get("isDefault"),
                    "input_modalities": model.get("inputModalities"),
                }
                for model in models
            ],
            "thread_count": len(threads.get("data") or []),
            "collaboration_modes": collaboration_modes.get("data") or [],
            "permission_profile_count": len(permissions.get("data") or []),
            "permission_profiles": [
                {
                    "id": profile.get("id"),
                    "description": profile.get("description"),
                    "allowed": profile.get("allowed"),
                }
                for profile in (permissions.get("data") or [])
                if isinstance(profile, dict)
            ],
            "skill_groups": len(skills.get("data") or []),
            "plugin_marketplaces": len(plugins.get("marketplaces") or []),
            "mcp_server_count": len(mcp.get("data") or []),
            "directory_entries": len(directory.get("entries") or []),
        }

        started = client.request(
            "thread/start",
            {
                "cwd": str(work_dir),
                "model": model_id,
                "approvalPolicy": "untrusted",
                "approvalsReviewer": "user",
                "permissions": ":workspace",
            },
        )
        thread = started.get("thread") if isinstance(started.get("thread"), dict) else {}
        thread_id = thread.get("id")
        if not isinstance(thread_id, str) or not thread_id:
            raise RpcFailure("thread/start returned no thread id")

        client.defer_server_requests = True
        interruption = client.request(
            "turn/start",
            {
                "threadId": thread_id,
                "cwd": str(work_dir),
                "model": model_id,
                "approvalPolicy": "untrusted",
                "approvalsReviewer": "user",
                "permissions": ":workspace",
                "input": [
                    {
                        "type": "text",
                        "text": "请立即调用 shell 命令工具执行 sleep 15，不要修改文件、不要联网，也不要提前结束回合。",
                        "text_elements": [],
                    }
                ],
            },
            timeout=args.timeout,
        )
        interrupt_turn = interruption.get("turn") if isinstance(interruption.get("turn"), dict) else {}
        interrupt_turn_id = interrupt_turn.get("id")
        if not isinstance(interrupt_turn_id, str) or not interrupt_turn_id:
            raise RpcFailure("turn/start returned no turn id for interrupt test")

        approval_request = client.wait_for_pending_request(
            thread_id,
            interrupt_turn_id,
            timeout=60,
        )
        steer_result: dict[str, Any] | None = None
        steer_error: str | None = None
        try:
            steer_result = client.request(
                "turn/steer",
                {
                    "threadId": thread_id,
                    "expectedTurnId": interrupt_turn_id,
                    "input": [
                        {
                            "type": "text",
                            "text": "请将本次回复标记为 STEERED。",
                            "text_elements": [],
                        }
                    ],
                },
            )
        except (RpcFailure, TimeoutError) as error:
            steer_error = str(error)

        try:
            interrupt_result = client.request(
                "turn/interrupt",
                {"threadId": thread_id, "turnId": interrupt_turn_id},
            )
        except RpcFailure as error:
            interrupt_result = {"error": str(error)}
        client.decline_pending_requests()
        client.defer_server_requests = False
        try:
            interrupt_completion = client.wait_for_turn_complete(
                thread_id,
                interrupt_turn_id,
                timeout=args.timeout,
            )
        except TimeoutError as error:
            interrupt_completion = {"timeout": str(error)}
        summary["checks"]["steer_and_interrupt"] = {
            "turn_id": interrupt_turn_id,
            "approval_request_method": approval_request.get("method"),
            "approval_request_command": (
                approval_request.get("params", {}).get("command")
                if isinstance(approval_request.get("params"), dict)
                else None
            ),
            "steer_result": steer_result,
            "steer_error": steer_error,
            "interrupt_result": interrupt_result,
            "completion": interrupt_completion,
        }
        if steer_error is not None:
            summary["failures"].append(f"turn/steer failed: {steer_error}")
        if "error" in interrupt_result:
            summary["failures"].append(f"turn/interrupt failed: {interrupt_result['error']}")
        if interrupt_completion.get("timeout"):
            summary["failures"].append(f"interrupted turn did not complete: {interrupt_completion['timeout']}")
        completion_status = (
            interrupt_completion.get("turn", {}).get("status")
            if isinstance(interrupt_completion.get("turn"), dict)
            else None
        )
        if completion_status != "interrupted":
            summary["failures"].append(
                f"interrupted turn completed with status {completion_status!r}, expected 'interrupted'"
            )

        simple = client.request(
            "turn/start",
            {
                "threadId": thread_id,
                "cwd": str(work_dir),
                "model": model_id,
                "approvalPolicy": "untrusted",
                "approvalsReviewer": "user",
                "permissions": ":workspace",
                "input": [
                    {
                        "type": "text",
                        "text": "请只回复 OK，不要调用命令或修改文件。",
                        "text_elements": [],
                    }
                ],
            },
            timeout=args.timeout,
        )
        simple_turn = simple.get("turn") if isinstance(simple.get("turn"), dict) else {}
        simple_turn_id = simple_turn.get("id")
        if not isinstance(simple_turn_id, str) or not simple_turn_id:
            raise RpcFailure("turn/start returned no turn id for completion test")
        simple_completion = client.wait_for_turn_complete(
            thread_id,
            simple_turn_id,
            timeout=args.timeout,
        )
        summary["checks"]["turn_round_trip"] = {
            "turn_id": simple_turn_id,
            "completion": simple_completion,
        }

        resumed = client.request(
            "thread/resume",
            {
                "threadId": thread_id,
                "cwd": str(work_dir),
                "excludeTurns": True,
                "initialTurnsPage": {
                    "limit": 5,
                    "sortDirection": "desc",
                    "itemsView": "full",
                },
            },
        )
        thread_read = client.request("thread/read", {"threadId": thread_id, "includeTurns": True})
        turns = client.request(
            "thread/turns/list",
            {
                "threadId": thread_id,
                "limit": 5,
                "sortDirection": "desc",
                "itemsView": "full",
            },
        )
        summary["checks"]["thread_lifecycle_read"] = {
            "thread_id": thread_id,
            "resume_has_thread": isinstance(resumed.get("thread"), dict),
            "read_has_thread": isinstance(thread_read.get("thread"), dict),
            "turn_count": len(turns.get("data") or []),
        }
    except Exception as error:
        summary["failures"].append(f"{type(error).__name__}: {error}")
        raise
    finally:
        if thread_id:
            try:
                client.request("thread/delete", {"threadId": thread_id})
                summary["cleanup"]["thread_deleted"] = True
            except Exception as error:
                summary["cleanup"]["thread_deleted"] = False
                summary["cleanup"]["thread_delete_error"] = str(error)
        summary["cleanup"]["temp_directory_removed"] = False
        try:
            shutil.rmtree(work_dir)
            summary["cleanup"]["temp_directory_removed"] = True
        except OSError as error:
            summary["cleanup"]["temp_directory_error"] = str(error)
        summary["server_requests"] = client.server_requests
        summary["notifications"] = [
            message.get("method")
            for message in client.notifications
            if message.get("method")
        ]
        summary["approval_decisions"] = client.approval_decisions
        summary["stderr_tail"] = client.stderr[-20:]
        summary["passed"] = not summary["failures"]
        client.close()
        print(json.dumps(summary, ensure_ascii=False, indent=2))
        if summary["failures"]:
            raise SystemExit(1)


if __name__ == "__main__":
    main()
