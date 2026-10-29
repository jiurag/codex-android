# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

需要在 Android 手机上操作远端 Codex 的开发者和运维人员。主要场景是手机通过 SSH 连接一台已安装 Codex CLI 的 Linux/macOS/Windows 主机，查看历史任务、继续对话、处理审批并切换模型。

以上用户与场景来自现有 README、架构文档和界面实现；不是新增用户访谈结论。

## Product Purpose

这个 App 把远端 Codex 的项目、任务、模型、权限和审批流程带到 Android，同时让 Agent、仓库、凭据和工具执行始终留在远端主机。成功标准是用户能用最少的层级完成连接、找回任务、发送消息和理解当前 Agent 状态。

## Positioning

它不是把 Codex 塞进手机本地运行，而是通过 SSH 启动并控制远端 `codex app-server`，复用远端已有的项目、认证、模型和会话。

## Operating Context

- 用户可能在移动网络、小屏幕和单手操作下继续一项已经开始的开发任务。
- 连接信息包含 SSH 主机、端口、用户名和密码或私钥，需要明确的安全反馈。
- 工作区会显示流式回答、命令、工具调用、文件差异、目标、计划模式、模型档位、权限和审批。
- 远端模型、技能、插件、路径和协议字段是业务数据，不能为了中文界面而误翻译或改写。
- 已验证基线为 `codex-cli 0.165.0`，具体协议证据见 `docs/BLACKBOX_CODEX_0165.md`。
- 服务器可能没有任何历史项目或会话；“新建会话”必须允许远端使用默认工作目录，不能要求先存在历史项目。
- 新建的空会话可能暂时不出现在远端历史列表中；客户端必须保留刚创建的本地会话，直到远端列表真正包含它。

## Capabilities and Constraints

- 保存多条 SSH 连接并复用 Android Keystore 加密凭据。
- 首次连接固定主机指纹，指纹变化时阻止连接。
- 导入远端 Codex 项目和任务，支持恢复、新建、重命名、置顶、归档、恢复、删除、派生和压缩上下文。
- 支持文本、图片、远端路径、技能和插件引用；支持模型、推理强度、服务档位、协作模式和权限切换。
- 支持命令、文件变更、工具调用、审批、目标、MCP、诊断和反馈。
- 当前只提供简体中文界面，没有英文回退；远端业务字段保持原文。
- 技术栈为 Kotlin、Jetpack Compose、Material 3、SSHJ 和远端 JSONL JSON-RPC。

## Brand Commitments

- 界面必须使用本机 Codex 桌面客户端所体现的视觉语言：深靛色工作表面、浅蓝强调线、单色高对比图形、克制边框和终端式信息密度。
- 不声称这是 OpenAI 官方发布的设计规范；现有视觉依据来自用户指定方向和本机桌面客户端采样。
- 现有 44 个自绘动作图标用于界面操作；App 启动图标、侧栏品牌区和连接空状态使用从本机 Codex 桌面客户端取得的官方 PNG 图标。

## Evidence on Hand

- 上游源码与功能差异：`README.md`、`docs/ARCHITECTURE.md`、`docs/FEATURE_PARITY.md`。
- `codex-cli 0.165.0` 黑盒记录：`docs/BLACKBOX_CODEX_0165.md`。
- 当前动作图标系统：`app/src/main/java/com/codex/remote/ui/icons/CodexIcons.kt`，图标总览共 44 项。
- 当前品牌图标组件：`app/src/main/java/com/codex/remote/ui/components/CodexBrandMark.kt`，直接复用 `app/src/main/res/drawable-nodpi/ic_launcher_codex_official.png`。
- 当前界面截图：`review/codex-final-installed.png`、`review/codex-final-sidebar-1.3.png`、`review/codex-systembar-fixed.png`、`review/codex-systembar-fixed-dark.png`、`review/codex-current-drawer.png`。
- 没有品牌手册、官方组件规范或经用户确认的营销文案；后续工作不能虚构这些材料。

## Product Principles

1. 远端状态优先：界面必须清楚显示连接、模型、权限和审批的真实状态。
2. 一个动作一个主入口：高频动作直接展示，低频动作统一收进上下文菜单或底部面板。
3. 对话优先：任务内容占主要面积，导航和设置退到次级层级。
4. 终端式精确：路径、命令、差异和模型 ID 使用等宽信息层级，普通说明保持易读。
5. 移动端可靠：所有关键按钮至少 48dp，支持深色主题、字体放大、键盘和系统安全区。

## Accessibility & Inclusion

- 所有可点击控件需要至少 48dp 触控区域和可读的内容描述。
- 必须支持 Android 深色主题、`1.3` 倍字体、系统返回手势、IME 和导航栏安全区。
- 不能只靠颜色区分状态；连接、审批和错误需要文字或图形辅助。
