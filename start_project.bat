@echo off
setlocal

:: 设置项目根目录为当前脚本所在目录
set "PROJECT_ROOT=%~dp0"
cd /d "%PROJECT_ROOT%"

echo ==========================================
echo   正在启动 Timeline Book 项目...
echo ==========================================

:: 1. 启动后端 (Spring Boot)
echo [1/3] 正在启动后端服务...
:: 使用 start 命令打开新窗口运行 mvnw，避免阻塞当前脚本
start "Timeline Book Backend" cmd /k "mvnw spring-boot:run"

:: 2. 启动前端 (Vue)
echo [2/3] 正在启动前端服务...
cd frontend
start "Timeline Book Frontend" cmd /k "npm run dev"

:: 3. 自动打开浏览器
echo [3/3] 等待服务初始化 (约15秒)...
:: 等待 15 秒让服务启动，然后打开浏览器
timeout /t 15 >nul
start http://localhost:5173

echo ==========================================
echo   启动命令已发送！
echo   - 后端日志请查看 "Timeline Book Backend" 窗口
echo   - 前端日志请查看 "Timeline Book Frontend" 窗口
echo ==========================================
:: 脚本执行完毕后自动关闭当前窗口，保留后端和前端窗口
exit