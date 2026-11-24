@echo off
REM Arranca el backend en local con la configuracion de desarrollo.
REM Lee GOOGLE_CLIENT_ID del archivo .env de la raiz del repo (no contiene secretos de este script).
setlocal
cd /d "%~dp0"
set "APP_CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:5174,http://localhost:3000"
set "SESSION_COOKIE_SECURE=false"
if exist "%~dp0..\.env" (
  for /f "usebackq eol=# tokens=1,* delims==" %%a in ("%~dp0..\.env") do (
    if /i "%%a"=="GOOGLE_CLIENT_ID" set "GOOGLE_CLIENT_ID=%%b"
  )
)
if "%GOOGLE_CLIENT_ID%"=="" (
  echo [aviso] GOOGLE_CLIENT_ID esta vacio: el inicio de sesion con Google respondera 503.
) else (
  echo [ok] Inicio de sesion con Google habilitado.
)
echo [ok] CORS: %APP_CORS_ALLOWED_ORIGINS%
call mvnw.cmd spring-boot:run
endlocal
