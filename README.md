# OmniSIM

**简体中文** | [English](README_EN.md)

**SIM 与 eSIM 续费管理器**

再也不错过 SIM 续费。

OmniSIM 是一款轻量的 Android 原生工具，用于管理 SIM 和 eSIM 的续费、充值及
保号日期。它面向需要管理少量号码的个人用户，无需账号，也不依赖后端服务。

## 功能

- 首页优先显示下一张需要处理的 SIM
- 根据日期自动计算已逾期、今日到期、即将到期、使用中和已归档状态
- SIM/eSIM 信息本地持久化，支持搜索和实用筛选
- 支持 30、60、90、120、180、365 天及自定义续费周期
- 快速“标记为已续费”，并可修改自动计算的下次续费日期
- 支持单卡续费历史和可筛选的全局续费历史，可记录金额与备注
- 安全打开外部续费链接
- 支持归档、恢复及带二次确认的删除操作
- 通过可配置偏移量和去重机制提供近似每日本地提醒
- 支持跟随系统、浅色、深色主题及可选的 Material You 动态配色
- 默认隐藏手机号码的部分数字
- 使用欧洲中央银行每日参考汇率估算不同货币的合计成本，并提供离线缓存回退
- 设置中内置隐私与权限说明和简明使用指南
- 通过 Android 存储访问框架导出版本化 JSON，并进行校验和事务性恢复

## 截图

截图将在首个正式版本后补充。

| 首页 | SIM 详情 | 设置 |
| --- | --- | --- |
| _即将补充_ | _即将补充_ | _即将补充_ |

## 隐私

OmniSIM 以本地存储为核心。

你的手机号码、SIM 信息、续费日期和备注均保存在 Android 设备本地。

OmniSIM 不需要账号，也不会将你的 SIM 数据上传到远程服务器。

应用不包含分析、遥测、广告、远程日志或云服务。SIM 管理和提醒等核心功能可完全
离线使用。仅当“使用情况”页面需要进行货币换算时，OmniSIM 才会下载欧洲中央银行
公开的每日参考汇率 XML 并缓存在本地；请求不会携带 SIM 数据、手机号码、价格或
续费日期。只有用户主动点击续费网站时，应用才会使用用户选择的浏览器打开该链接。

## Android 要求

- Android 6.0（API 23）或更高版本
- Android 13 及更高版本仅在用户主动启用提醒后请求通知权限
- Android 12 及更高版本可使用基于壁纸的 Material You 动态配色

## 开发环境

1. 安装 Android Studio，或安装包含 API 36 和 Build Tools 35.0.0 及更高版本的
   Android SDK。
2. 安装 JDK 17 或更高版本；项目编译为 Java 17 字节码。
3. 克隆仓库并在 Android Studio 中打开项目根目录。
4. 等待 Gradle 同步完成，然后在模拟器或设备上运行 `app` 配置。

项目已提交 Gradle Wrapper。在 Windows PowerShell 中构建：

```powershell
.\gradlew.bat assembleDebug
```

在 macOS 或 Linux 中构建：

```bash
./gradlew assembleDebug
```

Debug APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。

## GitHub 发布

推送语义化版本标签会运行 `.github/workflows/release.yml`。工作流会执行测试和
Lint、构建经过压缩的 Release 版本、确认 APK 未签名，并创建包含 APK 与 SHA-256
校验文件的 GitHub Release。整个流程不使用签名密钥或签名 Secrets。

发布前需要修改 `app/build.gradle.kts` 中的 `versionCode` 和 `versionName`，提交
修改后推送与 `versionName` 一致的标签：

```bash
git tag v1.0.1
git push origin v1.0.1
```

发布文件命名为 `OmniSIM-<version>-release-unsigned.apk`。未签名 APK 不能作为
普通正式安装包直接安装，安装或分发前必须使用正式发布密钥进行签名。

## 测试与 Lint

```powershell
.\gradlew.bat test
.\gradlew.bat lint
```

业务逻辑测试覆盖续费日期计算、状态优先级、提醒匹配与去重，以及备份数据校验。

## 架构

OmniSIM 有意保持简单的架构：

```text
Jetpack Compose UI
        ↓
AppViewModel + 不可变 StateFlow 状态
        ↓
SimRepository / SettingsRepository
        ↓
Room / DataStore Preferences
```

一个轻量的应用容器负责提供数据库、Repository、备份管理器和提醒调度器。项目不使用
依赖注入框架或云服务。

Room 保存三张数据表：

- `sims`：SIM 身份信息和续费配置
- `renewal_history`：SIM 对应的实际续费记录，随 SIM 级联删除
- `reminder_state`：已经发送的唯一 SIM/日期/偏移量提醒状态

续费截止日期以 ISO `LocalDate` 保存，只有创建和更新时间等元数据使用 `Instant`，
避免时区变化导致日历日期偏移。

## 备份与恢复

通过“设置 → 数据”可以导出和导入 JSON。目标文件和来源文件由 Android 文档选择器
确定，因此 OmniSIM 不需要广泛的存储权限。

备份包含 `backupVersion`、SIM、续费历史和相关设置。显示恢复确认前，OmniSIM 会
完整解析并校验 ID、引用关系、日期、必填值、价格和安全的 HTTP(S) 链接。数据库替换
通过 Room 事务完成；无效输入不会修改现有数据。

## 通知行为

OmniSIM 会调度一个省电的周期性 WorkManager 任务。默认检查提前 30、14、7、3、1、
0 天以及已逾期状态，并在通知后记录唯一的 `SIM ID + 续费日期 + 提醒偏移量` 标记。
SIM 续费后会清除该 SIM 的旧提醒状态。

WorkManager 的执行时间是近似的。Doze、省电模式、应用待机或厂商电池策略可能延迟
后台任务，因此通知是日期提醒，不保证在某个精确时刻送达。OmniSIM 不请求精确闹钟
权限，也不运行持续后台服务。

## 参与贡献

欢迎提交 Issue 和范围明确的 Pull Request。请保持 OmniSIM 的目标：为个人管理少量
SIM 提供快速、私密、本地优先的续费提醒。

提交修改前请运行：

```powershell
.\gradlew.bat assembleDebug test lint
```

请勿加入分析、账号系统、云基础设施或不必要的权限和依赖。

## 许可证

OmniSIM 使用 [MIT License](LICENSE) 发布。
