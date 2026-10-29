# Codex CLI 0.165.0 黑盒验收

验证日期：2026-10-29（Asia/Hong_Kong）

## 环境

- 服务器：测试环境
- 系统：Ubuntu 24.04，x86_64
- Codex CLI：`codex-cli 0.165.0`
- 测试目录：服务器 `/tmp` 下一次性创建的 `codex-remote-blackbox-*`
- 测试方式：真实启动 `codex app-server --listen stdio://`，通过 JSONL 发送协议请求、接收事件和审批请求

测试脚本：

```text
tools/blackbox_codex_0165.py
tools/blackbox_thread_start_without_cwd.py
```

运行方式：

```text
python3 /tmp/blackbox_codex_0165.py --timeout 180
```

## 最终结果

脚本最终返回 `exit=0`，摘要字段为 `passed=true`、`failures=[]`。

| 检查项 | 结果 |
| --- | --- |
| `initialize` | 成功；服务端返回 `codex-cli 0.165.0` 用户代理信息 |
| 账号状态 | `requiresOpenaiAuth=false` |
| 模型列表 | 2 个模型 |
| 默认模型 | `deepseek-v4-flash` |
| 第二模型 | `deepseek-v4-pro` |
| 模型输入能力 | 两个模型均返回文本输入 |
| 协作模式 | 返回 `Plan / plan / medium` 与 `Default / default` |
| 权限档位 | `:read-only`、`:workspace`、`:danger-full-access` 均返回允许 |
| 只读查询 | 任务列表、协作模式、权限档位、技能、插件、MCP 状态和目录读取均成功 |
| 临时任务 | 创建成功 |
| 审批链路 | 收到 `item/commandExecution/requestApproval`，请求命令为 `/bin/bash -lc 'sleep 15'` |
| 运行中追加 | `turn/steer` 成功，未返回错误 |
| 运行中中断 | `turn/interrupt` 成功，没有返回错误 |
| 中断后状态 | `interrupted` |
| 普通消息回合 | 成功完成，最终状态 `completed` |
| 会话恢复 | `thread/resume` 返回任务 |
| 会话读取 | `thread/read` 返回任务，`thread/turns/list` 返回 2 个回合 |
| 临时线程清理 | 已删除 |
| 临时目录清理 | 已删除 |

## 安全边界

1. 测试使用临时工作目录，不接触用户项目和原有 Codex 会话。
2. 模型请求执行 `sleep 15` 时，审批先被挂起，用于验证运行中追加和中断；随后审批被拒绝，命令没有获得执行许可。
3. 没有修改服务器 Codex 配置、DeepSeek 模型配置、认证信息或密钥。
4. 测试结束后，临时线程和临时目录均已清理。

## 单元测试补充

以下两个 `0.165.0` 结构通过 Kotlin 单元测试：

1. `{"name":"Future preset","mode":null,...}` 能被保留，`mode` 为 `null`。
2. `functionCallOutput` 的文本和图片内容能转换成中文时间线条目。

本次真实协议运行没有自然产生 `functionCallOutput` 事件，因此只能说该解析逻辑由单元测试覆盖，不能说真实模型事件也已覆盖。

## 零历史项目专项黑盒

真实手机截图显示服务器已连接并显示 `Codex 0.165.0`，但远端历史为 `0 个项目、0 个会话`。专项黑盒使用 `tools/blackbox_thread_start_without_cwd.py`，通过真实 `codex app-server --listen stdio://` 验证以下协议路径：

1. 请求不发送 `cwd`，调用 `thread/start`。
2. 使用 App 默认的 `approvalPolicy=on-request`、`approvalsReviewer=user` 和 `sandbox=workspace-write`。
3. 服务端成功创建会话，返回目录 `/root`。
4. 返回模型为 `deepseek-v4-flash`，提供方为 `custom`。
5. 临时线程已删除，脚本退出码为 `0`。

结论：Codex `0.165.0` 在没有历史项目时可以由服务端选择默认目录，App 不应再以“没有可用项目”阻止创建会话。

Android 侧另有 `zeroHistoryCanStartNewSessionAndRevealComposer` 设备黑盒，覆盖“0 项目 → 新建会话 → 写入默认目录和新线程 → 输入框出现”。模拟器未保存测试环境的 SSH 凭据，因此界面黑盒与服务器协议黑盒分别完成，不能表述为同一模拟器内的完整端到端链路。

## 历史刷新竞争专项黑盒

真实手机在 `1.0.6` 上确认按钮、版本和连接都正常后，点击“新建会话”仍会丢失输入栏。针对该现象追加以下真实服务器步骤：

1. 使用 App 默认参数调用 `thread/start`，不发送 `cwd`。
2. 服务端成功返回线程 ID、目录 `/root`、模型 `deepseek-v4-flash` 和提供方 `custom`。
3. 紧接着按 App 相同参数调用 `thread/list`，只使用未归档会话、`updated_at` 降序和空来源过滤。
4. `thread/list` 返回 `returned_count=0`，`contains_started_thread=false`。
5. 临时线程删除成功，脚本退出码为 `0`。

结论：新会话在第一条消息前可能是临时线程，暂时不会出现在 `thread/list`。客户端的 `thread/started` 事件刷新必须合并“已启动但尚未列出”的本地线程，不能用空历史列表覆盖当前新会话。

`1.0.7` 已加入该合并保护。设备黑盒 `zeroHistoryCanStartNewSessionAndRevealComposer` 按“创建成功 → 历史刷新返回 0 条 → 输入框仍显示”的顺序复测通过。

## 未覆盖项

1. 本次模拟器没有保存可复用的测试 SSH 凭据，因此“模拟器界面点击”和“真实服务器会话创建”分别完成黑盒验证，未在同一次模拟器会话中串成端到端操作。
2. 所有 MCP 和插件分支：当前服务器黑盒返回技能组 1 个、插件市场 0 个、MCP 服务器 0 个，无法在没有外部配置的前提下实际覆盖非空界面。

## 服务器警告

`codex app-server` 的 stderr 返回：

```text
Codex is ignoring 1 unrecognized configuration setting.
user (/root/.codex/config.toml): `disable_response_storage` is ignored.
```

这是服务器现有配置中的弃用项。为遵守不改动原有服务器配置和 DeepSeek 配置的要求，本次没有修改。
