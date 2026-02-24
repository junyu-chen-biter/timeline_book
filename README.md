下面是ai的总结，这是一个vibe coding的作品，还有很多的东西可以添加上，属于是个半成品


# 📚 Timeline Book - 本地个人知识管理系统 (PKMS)

一个基于 **Spring Boot 3** + **Vue 3** 的全栈本地知识管理系统，专注于学科资料管理（PDF/Markdown/PPT）与智能复习推荐。

✨ **核心理念**：基于艾宾浩斯遗忘曲线，为你智能规划每日复习任务，告别“只存不看”。

## 🚀 核心特性

- **无限层级目录**：自由创建文件夹，支持无限嵌套，构建你的知识树。
- **多格式预览**：
  - 📄 **PDF**：内置 PDF.js 阅读器。
  - 📝 **Markdown**：支持实时渲染与代码高亮。
  - 📊 **PPT**：支持 Office PPT/PPTX 在线预览。
- **智能复习推荐**：
  - 独创 **PriorityScore** 算法：综合考量 `遗忘曲线时间因子` × `学科难度` × `学分权重` × `截止日期`。
  - 首页仪表盘自动生成“今日待复习”列表。
- **学习闭环**：
  - **打卡机制**：每次复习后打卡，系统自动记录并推算下次复习时间。
  - **笔记系统**：针对每个文件均可记录专属笔记（Markdown 格式）。
- **零配置部署**：使用 H2 本地文件数据库，无需安装 MySQL，数据随项目走。

## 🛠️ 技术栈

- **后端**：Spring Boot 3.2, Java 17+, Spring Data JPA, H2 Database
- **前端**：Vue 3, Vite, Pinia, Element Plus, TailwindCSS v4
- **工具**：Axios, PDF.js, Markdown-it

## 🏁 快速开始 (Quick Start)

### 1. 环境要求
- **JDK**: 17 或更高版本
- **Node.js**: 16 或更高版本

### 2. 启动后端 (Spring Boot)

```bash
# Windows
./mvnw spring-boot:run

# Mac/Linux
./mvnw spring-boot:run
```

*后端启动成功后，服务将运行在 `http://localhost:8080`*

### 3. 启动前端 (Vue)

打开一个新的终端窗口：

```bash
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

*前端启动成功后，访问控制台输出的地址（通常是 `http://localhost:5173`）即可使用。*

## 📂 项目结构说明

- `src/main/java`: 后端源码
- `frontend/`: 前端源码
- `uploads/`: **[自动生成]** 存放你上传的所有文件（PDF/MD等）。此目录不会被 Git 提交。
- `data/`: **[自动生成]** H2 数据库文件存放目录。

## 🤝 贡献与开源

本项目完全开源，欢迎 Fork 和 PR！如果你觉得好用，请给一个 Star ⭐️。

---
*Created by [Your Name]*
