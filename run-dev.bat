@echo off
REM Script para ejecutar la aplicación con perfil dev
echo ================================================
echo   Ejecutando InnoSistemas con perfil DEV
echo ================================================
echo.
echo Perfil: dev (Base de datos PostgreSQL)
echo.
echo IMPORTANTE: Asegurate de tener PostgreSQL corriendo en localhost:5432
echo Usuario: postgres
echo Password: password
echo Base de datos: innosistemas
echo.
pause

mvn spring-boot:run -Dspring.profiles.active=dev