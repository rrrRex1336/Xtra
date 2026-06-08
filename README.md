# Naven-Modern (Fabric Edition)

一个基于 Minecraft 1.20.1 Fabric 的现代化模组客户端，提供丰富的游戏增强功能。

> 本项目由原 Naven-Modern Forge 版本移植至 Fabric 生态。

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

* **Minecraft**：1.20.1
* **Mod Loader**：Fabric Loader 0.14+
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

### 构建步骤

```bash
# 克隆项目
git clone https://github.com/FireFlyDeveloper54/Naven-Modern.git
cd Naven-Modern

# 构建项目
./gradlew build
```

构建完成后，模组文件将位于：

```
build/libs/
```

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

## ⚖️ 许可证（GPL-3.0 强保护声明）

本项目采用 **GNU General Public License v3.0 (GPL-3.0)**。

根据 GPL-3.0 规定：

### ✅ 允许

* 使用
* 修改
* 分发
* 商业用途（前提是遵守 GPL）

---

### ❗ 强制要求（必须遵守）

1. **强制开源**

   * 任何分发的修改版本，必须公开完整源代码。
   * 必须继续使用 GPL-3.0 协议。

2. **必须署名原作者**

   * 不得删除或修改原作者版权声明。
   * 修改版本必须注明“基于 Naven-Modern Fabric 修改”。
   * 必须保留原项目来源说明。

3. **禁止闭源发布**

   * 不得以闭源形式发布或分发本项目或其衍生版本。

4. **禁止篡改协议**

   * 不得将本项目改为 MIT、Apache 等更宽松协议。
   * 不得移除 GPL 许可证文件。

---

### ⚠️ 关于商业销售

GPL-3.0 允许销售软件副本，但：

* 必须同时提供完整源代码
* 必须保留 GPL 协议
* 必须保留原作者署名

若违反以上条款，将构成侵权行为。

协议全文：
https://www.gnu.org/licenses/gpl-3.0.html

---

## ⚠️ 免责声明

本模组仅供学习与研究目的使用。
使用者需自行承担使用风险，开发者不对因使用本模组导致的问题负责。
请遵守服务器规则与相关条款。

---

*Naven-Modern (Fabric Edition) - 让你的 Minecraft 体验更加现代化*