# Codex（简体中文 Android 客户端）

> **项目来源说明**
>
> 本仓库是 [`liuhao-labs/codex-remote-android`](https://github.com/liuhao-labs/codex-remote-android) 的独立分支（fork），
> 当前维护地址为 [`jiurag/codex-remote-android`](https://github.com/jiurag/codex-remote-android)。
> 本仓库不是上游作者的官方仓库，也不代表 OpenAI 官方产品。

这是一个通过 SSH 连接远端 Codex 主机的 Android 客户端。保存的一条连接对应一台主机；连接后，App 会读取远端 Codex 的项目和历史任务，并在远端启动 `codex app-server`，通过它的 JSONL 协议通信。Codex 本身不会在 Android 设备上运行。

## 项目来源与版本

- 上游仓库：<https://github.com/liuhao-labs/codex-remote-android>
- 当前分支仓库：<https://github.com/jiurag/codex-remote-android>
- 本地适配基线：提交 `372581a`
- 上游基线版本：`0.1.2`，`versionCode 3`
- 当前适配版本：`1.0.10`，`versionCode 19`
- 适配方式：基于上游创建独立分支（fork），改动只推送到当前分支仓库，不向上游仓库推送

当前版本是硬编码简体中文界面，没有英文回退和 App 内语言切换。自动审批提示、通用远端提示、远端登录命令诊断、速度档位、模型与推理说明、MCP 工具和资源统计均显示简体中文。远端模型名、技能名、插件名、文件路径、命令和协议字段保留原文，避免把业务标识误翻译后破坏请求；无法可靠翻译的英文运行提示显示中文摘要，并引导到服务器端 Codex 日志查看技术详情。

## MiSans 字体

界面已内置小米 MiSans 的 `Regular`、`Medium` 和 `Semibold` 三个字重，分别用于正文、辅助信息和标题按钮；代码、路径、模型标识和差异内容继续使用等宽字体。字体以官方原始文件内置，没有裁剪或二次开发。

官方来源、SHA-256 和许可协议见 [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md) 与 [`MiSans-Font-License.pdf`](licenses/MiSans-Font-License.pdf)。连接页也会显示“界面字体：MiSans（小米）”的软件内署名。

## Codex CLI 兼容性

本版本针对 `codex-cli 0.165.0` 做了以下协议适配：

1. `CollaborationModeMask` 的 `mode` 按官方 Schema 改成可空字段，只要求 `name` 必填。
2. 解析协作模式时保留 `mode = null` 的条目，不再直接丢弃。
3. 发送 `turn/start` 时，只有实际存在 `mode` 才附带 `collaborationMode`，避免发送非法空值。
4. 支持并显示 `0.165.0` 新增的 `functionCallOutput` 线程条目。
5. 补充 `mode = null` 和 `functionCallOutput` 的单元测试。
6. 新建会话允许省略 `cwd`；远端会返回实际工作目录，零历史服务器不再被误判为“没有可用项目”。

协议黑盒结果见 [`docs/BLACKBOX_CODEX_0165.md`](docs/BLACKBOX_CODEX_0165.md)，桌面功能对照和未实现项见 [`docs/FEATURE_PARITY.md`](docs/FEATURE_PARITY.md)。

## 远端主机要求

- Ubuntu、Linux 或 macOS 上的 OpenSSH 服务可连接，支持密码或 OpenSSH/PEM 私钥。
- 远端登录 shell 能直接执行 `codex --version`。
- 远端 Codex 已配置可用账号。验证环境使用 DeepSeek 模型配置，`account/read` 返回 `requiresOpenaiAuth = false`。
- 建议使用权限尽量小的专用 SSH 用户。高权限审批档位会扩大远端 Codex 能执行的操作范围，应按实际需要选择。

## DeepSeek 模型

App 不写死模型列表，而是通过远端 `model/list` 加载。验证环境返回：

| 模型 ID | 显示名 | 默认 | 输入能力 |
| --- | --- | --- | --- |
| `deepseek-v4-flash` | DeepSeek V4 Flash | 是 | 文本 |
| `deepseek-v4-pro` | DeepSeek V4 Pro | 否 | 文本 |

连接后可在输入框旁的模型菜单选择上述模型。若远端配置以后改变，App 会跟随 `model/list` 的返回结果，不需要重新修改 Android 源码。

## CC Switch 远端状态

当远端服务器安装了 CC Switch 并接管 Codex 供应商配置时，App 的“远端状态”优先读取 CC Switch 数据，不再查询 Codex 官方账号额度。

已在测试环境的 `cc-switch 5.11.3` 验证以下只读调用：

```text
cc-switch -a codex provider list
cc-switch -a codex provider quota <providerId> --json
```

App 从供应商列表识别当前 Codex 供应商，再查询该供应商的额度。界面会显示“数据源：CC Switch”、供应商名称、余额和单位。文档不记录验证账户的实际余额。

输入框右侧的上下文圆圈会同时显示余额：外环继续表示上下文使用率，圆心只显示金额整数部分，不显示货币符号、小数和单位。“远端状态”弹窗仍显示完整数值和单位。连接成功和每轮任务结束后会自动刷新余额；没有有效额度时圆心保持为空，不显示推测数值。

安全限制：

1. App 不调用会输出 API Key 的 `provider current`。
2. App 不读取 CC Switch 配置文件、API Key 或环境变量。
3. App 只调用固定的 `provider list` 和 `provider quota --json`，不执行切换供应商或额度重置。
4. 供应商 ID 必须符合只允许字母、数字、点、下划线和短横线的白名单，防止把远端表格内容拼成任意 shell 命令。

如果服务器没有 `cc-switch`，App 继续回退到原来的 Codex `account/rateLimits/read`。如果 CC Switch 已安装但额度脚本失败，状态弹窗会显示 CC Switch 查询错误，不再把包含 `auth` 的第三方供应商消息误报成 SSH 认证失败。实现说明见 [`docs/CC_SWITCH_STATUS.md`](docs/CC_SWITCH_STATUS.md)。

## 安装

正式版 APK：

```text
release/codex-remote-1.0.10-release.apk
```

安装命令：

```powershell
$env:ANDROID_HOME\platform-tools\adb.exe install -r .\release\codex-remote-1.0.10-release.apk
```

在 App 中添加 SSH 主机时填写服务器地址、端口、用户名和凭据。首次连接会显示主机指纹，确认与服务器管理员或 `ssh-keygen` 输出一致后才发送登录凭据。

如果手机上安装过本项目之前的 Debug 包，它和本正式包使用不同签名，Android 不允许直接覆盖升级。需要先卸载旧包：

```powershell
$env:ANDROID_HOME\platform-tools\adb.exe uninstall com.codex.remote
```

卸载会清除 App 本地保存的 SSH 连接和凭据，远端项目、任务和服务器文件不会被卸载；当前版本是否支持完整的连接数据导出和导入尚未验证，不能承诺迁移后一定保留本地连接数据。

## 构建

需要 JDK 17 和 Android SDK 35：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17.0.2'
$env:ANDROID_HOME='<Android SDK 路径>'
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
.\gradlew.bat test lintDebug connectedDebugAndroidTest assembleDebug assembleRelease bundleRelease
```

正式签名包需要设置以下环境变量后再执行 `assembleRelease`：

```text
CODEX_REMOTE_KEYSTORE_PATH
CODEX_REMOTE_KEYSTORE_PASSWORD
CODEX_REMOTE_KEY_ALIAS
CODEX_REMOTE_KEY_PASSWORD
```

签名材料不要提交到仓库。APK、密钥库、本机 SDK 配置、构建缓存和测试截图均不应上传。

## 自动化构建与发布

GitHub Actions 会按以下规则运行：

1. 对 `main` 的提交和 Pull Request 执行 Debug 单元测试、Lint 与 Debug APK 构建。
2. `main` 校验通过后，自动执行 Release 单元测试、Release Lint、签名 APK 和 AAB 构建。
3. 根据 `app/build.gradle.kts` 中的 `versionName` 创建或更新 `v<版本号>` GitHub Release。
4. Release 上传 APK、AAB、`SHA256SUMS.txt`，并保留 30 天 Actions 构建产物。
5. `workflow_dispatch` 可手动触发；手动发布也只允许从 `main` 执行。

签名材料通过 GitHub Actions Secrets 注入，包括：

- `CODEX_REMOTE_KEYSTORE_BASE64`
- `CODEX_REMOTE_KEYSTORE_PASSWORD`
- `CODEX_REMOTE_KEY_ALIAS`
- `CODEX_REMOTE_KEY_PASSWORD`

工作流结束时会删除临时签名文件。发布不是 Google Play 上架；它创建的是 GitHub Release，可在 Release 页面下载 APK 和 AAB。

## 测试现状

已通过：

1. `.\gradlew.bat test lintDebug assembleDebug assembleRelease bundleRelease` 构建通过，Release 的 R8 混淆和 `lintVitalRelease` 通过。
2. Debug 单元测试 86 项、Release 单元测试 86 项，均失败 0、错误 0、跳过 0。
3. `1.0.10` 在 `moying_glass_api35` 模拟器上完成 24 项 `connectedDebugAndroidTest`，失败 0、跳过 0；测试后已重新安装正式包。
4. `lintDebug`：0 个错误、13 个警告。
5. 模拟器黑盒已覆盖连接空状态、连接编辑、连接列表、主机指纹确认、工作区、侧栏、任务菜单、添加菜单、权限菜单、模型菜单和远端状态弹窗。
6. 模拟器黑盒已覆盖浅色、深色、`1.3` 倍字体、竖屏、横屏和键盘安全区。
7. 浅色侧栏打开时，状态栏和底部手势区已与侧栏一起铺成深色并切换为浅色系统图标；关闭侧栏后恢复浅色系统栏与深色图标。
8. 横屏 `1.3` 倍字体下的空工作区路径遮挡已修复并重新截图确认。
9. 正式 `1.0.10 (19)` APK 已在 API 35 模拟器安装并冷启动成功；包名、应用名、版本、签名和对齐均重新校验。
10. Codex CLI `0.165.0` 真实 `app-server` 协议黑盒已完成初始化、模型列表、两个 DeepSeek 模型、协作模式、三项权限档位、只读查询、新建临时任务、普通消息回合、运行中追加输入、审批请求挂起、中断、会话恢复/读取和临时资源清理。
11. MiSans 已在 API 35 模拟器实际渲染并完成黑盒截图检查，覆盖连接空状态、连接编辑及校验错误、连接列表、侧栏、任务列表、任务菜单、Markdown 正文、代码块、深色、`1.3` 倍字体和横屏；代码块继续使用等宽字体。
12. 连接表单校验不再在字段内部重复显示“必填”，错误原因统一显示在固定底栏；表单内容可完整滚动到底部，避免大字体下提示文字被底栏截断。
13. 输入栏的权限和模型入口改为纯图标，模型名、推理档位和权限文字不再常驻显示；菜单内容和无障碍朗读仍保留完整状态。普通手机宽度保持单行，只有任务运行且可用宽度小于 `300dp` 时才分两行；发送按钮保持 `48dp` 触控区且不越过输入框边界。已用 `deepseek-v4-flash`、`320dp`、`360dp` 和 `411dp` 宽度、`1.3` 倍字体完成黑盒截图。
14. 侧栏入口已从“新建任务”统一为“新建会话”；`/new` 命令也会创建独立远端会话。
15. 全量复查发现“从侧栏直接进入连接设置”会遗留白色状态栏图标；现已由连接页重新接管系统栏明暗，并完成浅色和深色黑盒截图检查。
16. 在零历史设备上点击“新建会话”不再提示“远端没有可用于新会话的 Codex 项目”，而是立即创建真实会话并显示输入框，并完成设备截图检查。
17. 在验证服务器的 Codex `0.165.0` 上使用与 App 默认一致的 `on-request`、`workspace-write` 参数发送不含 `cwd` 的 `thread/start`，服务端返回默认目录；临时线程随后删除成功。
18. 在验证服务器上继续执行“创建后立即查历史列表”，`thread/start` 已成功，但紧接着的 `thread/list` 返回 0 条；确认 App 的 `thread/started` 刷新会覆盖本地刚创建的新会话。
19. `1.0.7` 会保留服务端尚未列出的新会话，直到 `thread/list` 真正包含它；设备黑盒按“创建成功 → 刷新返回 0 条 → 输入框仍显示”完整复测通过。
20. `1.0.8` 已把自动审批结果、通用远端提示、速度档位、模型与推理说明、MCP 工具和资源统计统一转换为简体中文；设备黑盒确认英文自动审批原句与 `tools / resources` 不再显示。
21. `1.0.9` 又清理了“远端登录环境找不到 Codex 命令”时追加英文 shell 原文的边界提示；该位置现在只显示中文排查说明，涉及 Codex CLI 安装和 `PATH` 配置。
22. `1.0.10` 为 SSH 连接增加 30 秒心跳和连续 3 次无响应断线判定；App 每 15 秒检查连接状态。断线后显示“SSH 连接已断开，请重新连接。”，不会再误报成 CC Switch 安装或配置失败。
23. 输入栏圆圈接入 CC Switch 实时余额：外环保持上下文使用率，圆心只显示金额整数，不显示符号、小数和单位；无有效余额时不显示假数据。圆圈本体为 `34dp`，触控区保持 `48dp`，`320dp` 宽度和 `1.3` 倍字体下已通过边界与截图检查。

## 正式版交付

以下为本地构建产物。`.apk`、`.aab` 和 QA 截图默认不提交到 Git 仓库，源码构建方式见下文。

| 文件 | 大小 | SHA-256 |
| --- | ---: | --- |
| `release/codex-remote-1.0.10-release.apk` | `20,406,673` 字节（约 19.5 MiB） | `A8B837DDA7881D3A0BA5E3A21651C21393012C703A36D4FE774D69F56F72B907` |
| `release/codex-remote-1.0.10-release.aab` | `23,008,254` 字节（约 21.9 MiB） | `16CC2995E9DF28D49919E0BA1017991ACB4CB53BB0DD07BF119905FA43BFE9EB` |

APK 使用 APK Signature Scheme v2、RSA 4096 位独立 Release 密钥签名；签名证书 SHA-256 为 `53:27:2E:15:2C:49:47:6A:74:91:36:C6:4C:84:D5:CF:5F:F5:B2:50:E5:C8:B2:B2:25:D0:C7:ED:B7:9E:3F:96`。AAB 的 JAR 签名校验结果为有效，证书为项目自签名证书，因此 `jarsigner` 会提示无法建立公网 PKIX 信任链，这不等于签名损坏。

Lint 的 13 个警告包括依赖存在新版本、可由 `minSdkVersion 26` 合并的资源目录、未使用资源，以及 BouncyCastle 依赖中的空 `checkServerTrusted` 检测。最后一项目前只能说明 Lint 命中了依赖包代码，不能在没有单独安全审计的情况下断言 App 的 SSH 或网络链路存在可利用漏洞，也不能据此声称已安全验证。

尚未完成：

1. 当前完成的是 Android API 35 模拟器黑盒，尚未覆盖多款真实 Android 手机的厂商系统差异。
2. 尚未完成 TalkBack 全流程朗读回归。
3. `functionCallOutput` 已通过单元测试，但最终协议黑盒运行时没有自然产生该条目，尚不能声称真实模型事件已覆盖。
4. 正式包尚未提交到 Google Play；AAB 已完成本地签名和结构校验，商店上传后的 Play App Signing 状态需在发布时另行核对。

因此，本次可以确认“源码完成适配、CC Switch 实际额度命令和 JSON 结构已读取、正式 APK/AAB 已构建并签名、API 35 模拟器界面与状态弹窗黑盒通过”，但不能声称“多款 Android 真机全量验证通过”或“已经上架 Google Play”。

## 已知限制

- 当前只有简体中文，没有英文回退或语言切换。
- 远端配置中的弃用项会显示简体中文摘要，不再直接弹出英文原句；具体技术原文需到服务器端 Codex 日志查看。验证环境的 `disable_response_storage` 被 `0.165.0` 标记为忽略；为遵守“不改服务器原有配置”的要求，本次没有修改它。
- 上游尚未实现的功能仍保持缺失，完整列表见 [`docs/FEATURE_PARITY.md`](docs/FEATURE_PARITY.md)。

## 授权

本项目沿用上游的 [Apache License 2.0](LICENSE)。该项目是社区客户端，不代表 OpenAI 官方产品。
