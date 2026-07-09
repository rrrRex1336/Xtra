# Naven-Modern (Fabric Edition)

一个基于 Minecraft 1.20.4 Fabric 的现代化模组客户端，提供丰富的游戏增强功能。

> 本项目最初基于 Naven-Modern Forge 1.20.1 官方映射版本，后移植至 Fabric 1.20.4 官方映射生态，目前已迁移为 Fabric 1.20.4 Yarn 映射版本。

---

## 🚀 特性

### 核心功能

* **模块化架构**：可扩展的模块系统，支持动态加载和管理
* **事件驱动系统**：高效的事件管理机制
* **命令系统**：完整的命令行界面支持
* **配置管理**：灵活的配置文件系统，支持热重载
* **通知系统**：实时消息通知

---

## 📦 运行环境

* **Minecraft**：1.20.4
* **Mod Loader**：Fabric Loader 0.19.2
* **Fabric API**：必须安装
* **Java**：17+
* **内存**：建议 4GB+

---

## 🛠️ 安装指南

### 前置要求

1. 安装 Java 17 或更高版本
2. 安装 Fabric Loader
3. 安装 Fabric API


---

### 安装模组

1. 将生成的 `.jar` 文件复制到 Minecraft 的 `mods` 文件夹
2. 确保已安装 Fabric Loader 与 Fabric API
3. 启动游戏即可使用

---

## ⚙️ 配置

### 配置文件

* `settings.json` - 主要配置文件
* `binds.json` - 按键绑定配置
* `friends.json` - 好友列表

---

## 🎮 使用说明

### ClickGUI 使用

1. 按下 `右Shift` 打开 ClickGUI
2. 点击不同分类查看对应模块
3. 点击模块名称启用 / 禁用功能
4. 点击设置图标配置模块参数

---

### 命令系统

命令前缀为：

```
.
```

常用命令：

* `.bind <模块> <按键>` - 绑定快捷键
* `.config <操作>` - 配置管理
* `.language <语言>` - 切换语言

---

## 🔧 开发说明

### 开发环境

* Java 17+
* Gradle
* Fabric Loom

---

### 项目结构

```
src/main/java/
├── commands/
├── events/
├── files/
├── modules/
│   ├── impl/
│   │   ├── combat/
│   │   ├── misc/
│   │   ├── move/
│   │   └── render/
├── ui/
├── utils/
└── values/
```

---

### 添加新模块

1. 在对应分类目录创建模块类
2. 继承 `Module` 类并添加注解
3. 注册至 `ModuleManager`
---
## ⚠️ 免责声明

本模组仅供学习与研究目的使用。
使用者需自行承担使用风险，开发者不对因使用本模组导致的问题负责。
请遵守服务器规则与相关条款。

---

*Naven-Modern (Fabric Edition) - 让你的 Minecraft 体验更加现代化*
