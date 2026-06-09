<div align="center">
  <img src="https://raw.githubusercontent.com/blabby-cn/UniHub/main/Unihub.png" alt="UniHub Logo" width="120" />

  # UniHub

  **双面板文件管理器 & Android 多功能浏览器**

  [![许可证](https://img.shields.io/badge/license-GPL--3.0-blue?style=flat-square)](https://github.com/blabby-cn/UniHub/blob/main/LICENSE)
  [![平台](https://img.shields.io/badge/platform-Android-brightgreen?style=flat-square&logo=android)](https://www.android.com)
  [![最低 SDK](https://img.shields.io/badge/minSDK-21-ff69b4?style=flat-square)](https://developer.android.com/about/versions/lollipop)
  [![目标 SDK](https://img.shields.io/badge/targetSDK-34-green?style=flat-square)](https://developer.android.com/about/versions/14)
  [![版本](https://img.shields.io/badge/version-1.0.2-orange?style=flat-square)](https://github.com/blabby-cn/UniHub/releases)
  [![Stars](https://img.shields.io/github/stars/blabby-cn/UniHub?style=flat-square)](https://github.com/blabby-cn/UniHub/stargazers)
  [![GitHub 发布](https://img.shields.io/github/v/release/blabby-cn/UniHub?style=flat-square)](https://github.com/blabby-cn/UniHub/releases)
  [![最后提交](https://img.shields.io/github/last-commit/blabby-cn/UniHub?style=flat-square)](https://github.com/blabby-cn/UniHub/commits/main)
  [![语言](https://img.shields.io/badge/language-Java-ED8B00?style=flat-square&logo=openjdk)](https://www.java.com)
  [![构建](https://img.shields.io/badge/build-Gradle-02303A?style=flat-square&logo=gradle)](https://gradle.org)
  [![欢迎 PR](https://img.shields.io/badge/PRs-welcome-brightgreen?style=flat-square)](https://github.com/blabby-cn/UniHub/blob/main/CONTRIBUTING.md)

  <hr />
</div>

## 概述

**UniHub** 是一款面向 Android 平台的多功能文件管理与浏览工具，将本地文件管理、远程连接和网页浏览集于一身。

它内置**双面板本地文件浏览器**、**FTP/SFTP 远程文件访问**、**多标签 WebView 浏览器**、**文本与 Markdown 编辑器**、**视频播放器**、**ZIP 归档浏览器**，并支持 **14 种界面语言**。

## 功能特性

<details open>
<summary><b>📂 文件管理</b></summary>

- 双面板文件浏览（主面板 / 副面板）
- 文件操作：复制、移动、重命名、删除
- 文件压缩（ZIP / XZ 格式）
- 存储空间状态显示
- 文件搜索、筛选、排序（功能预留）
- 显示/隐藏文件切换
- 书签管理（功能预留）
- 基于 OkHttp 的文件下载
- 文件分享
</details>

<details open>
<summary><b>🌐 远程连接</b></summary>

- **FTP 客户端** — 基于 Apache Commons Net
- **SFTP 客户端** — 基于 JSch（Java Secure Channel）
- 远程文件浏览与导航
- 账户管理，支持登录信息持久化存储
</details>

<details open>
<summary><b>🌍 网页浏览</b></summary>

- 基于 WebView 的浏览器
- 多标签页支持
- 前进 / 后退导航
- 地址栏手动输入 URL
</details>

<details open>
<summary><b>✏️ 文本工具</b></summary>

- **文本编辑器** — 纯文本文件编辑
- **Markdown 编辑器** — 格式化 Markdown 编辑
- 代码块编辑支持
</details>

<details open>
<summary><b>🎬 媒体与归档</b></summary>

- **视频播放器** — 内置视频播放
- **ZIP 浏览器** — 查看 ZIP 归档内容
- **文档查看器** — 查看用户协议、隐私政策、更新日志、第三方许可
</details>

<details open>
<summary><b>🎨 用户体验</b></summary>

- **侧边栏导航** — 全屏抽屉式菜单，含滑动动画
- **明暗主题** — 支持亮色 / 暗色主题切换
- **多语言支持** — 14 种界面语言
- **Blabby 账户系统** — 登录、退出、头像加载
- Material Design 设计风格
</details>

## 多语言支持

UniHub 内置国际化支持，目前提供以下语言：

| 语言 | 代码 | 名称 |
|:--------:|:----:|:-----------:|
| 简体中文 | `zh_cn` | 简体中文 |
| 繁体中文 | `zh_tw` | 繁體中文 |
| 英语 | `en` | English |
| 日语 | `ja` | 日本語 |
| 韩语 | `ko` | 한국어 |
| 马来语 | `ms` | Bahasa Melayu |
| 缅甸语 | `my` | မြန်မာ |
| 德语 | `de` | Deutsch |
| 法语 | `fr` | Français |
| 俄语 | `ru` | Русский |
| 乌克兰语 | `uk` | Українська |
| 丹麦语 | `da` | Dansk |
| 捷克语 | `cs` | Čeština |
| 希腊语 | `el` | Ελληνικά |

> 语言文件位于 `assets/languages/*.yaml`，欢迎提交 PR 添加新语言！

## 技术栈

| 类别 | 技术 |
|:---------|:-----------|
| 语言 | Java |
| 构建系统 | Gradle (AGP 8.11.0) |
| 最低 SDK | API 21 (Android 5.0) |
| 目标 SDK | API 34 (Android 14) |
| 界面 | AndroidX AppCompat, Material Design, ConstraintLayout |
| 网络 | OkHttp 4, Apache Commons Net, JSch |
| 图片加载 | Glide 4.16 |
| 压缩 | XZ (Tukaani) |
| 导航 | DrawerLayout, ViewPager2 |
| 测试 | JUnit, Espresso |

## 构建

### 前置条件

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17+
- Android SDK 34

### 克隆与编译

```bash
git clone https://github.com/blabby-cn/UniHub.git
cd UniHub
./gradlew assembleDebug
```

生成的 APK 位于 `app/build/outputs/apk/debug/`。

> **注意**：Release 构建需要有效的签名证书，请在 `app/build.gradle` 中配置 `signingConfigs.release`。


## 许可证

本项目基于 [GNU General Public License v3.0 (GPL-3.0)](https://github.com/blabby-cn/UniHub/blob/main/LICENSE) 开源。

---

<div align="center">
  <sub>由 <a href="https://github.com/blabby-cn">Blabby.Co Studio</a> 用心打造</sub>
  <br />
  <sub>Copyright © 2026-现在 Blabby.Co Studio</sub>
</div>
