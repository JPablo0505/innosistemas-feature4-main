@echo off
REM Script para ejecutar la aplicación con perfil test
echo ================================================
echo   Ejecutando InnoSistemas con perfil TEST
echo ================================================
echo.
echo Perfil: test (Base de datos H2 en memoria)
echo.

mvn spring-boot:run -Dspring.profiles.active=test