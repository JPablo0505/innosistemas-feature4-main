@echo off
REM Script para configurar Java 17 para este proyecto
REM Ejecuta este script antes de compilar o ejecutar la aplicación

echo ================================================
echo   Configurando Java 17 para InnoSistemas
echo ================================================

REM Buscar Java 17 en ubicaciones comunes
set JAVA_17_HOME=

if exist "C:\Program Files\Java\jdk-17" (
    set JAVA_17_HOME=C:\Program Files\Java\jdk-17
    goto :found
)

if exist "C:\Program Files\Java\jdk-17.0.14" (
    set JAVA_17_HOME=C:\Program Files\Java\jdk-17.0.14
    goto :found
)

if exist "C:\Program Files\Eclipse Adoptium\jdk-17.0.14" (
    set JAVA_17_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.14
    goto :found
)

if exist "%USERPROFILE%\.jdks\openjdk-17.0.14" (
    set JAVA_17_HOME=%USERPROFILE%\.jdks\openjdk-17.0.14
    goto :found
)

REM Si no se encuentra, mostrar error
echo [ERROR] No se encontró Java 17 en las ubicaciones comunes.
echo.
echo Por favor, descarga Java 17 desde:
echo   - https://adoptium.net/temurin/releases/?version=17
echo   - https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html
echo.
echo Después de instalar, ejecuta este script nuevamente.
echo.
pause
exit /b 1

:found
echo [OK] Java 17 encontrado en: %JAVA_17_HOME%
echo.

REM Configurar variables de entorno para la sesión actual
set JAVA_HOME=%JAVA_17_HOME%
set PATH=%JAVA_HOME%\bin;%PATH%

REM Verificar versión
echo Versión de Java configurada:
java -version

echo.
echo ================================================
echo   Configuración completada exitosamente
echo ================================================
echo.
echo Ahora puedes ejecutar:
echo   mvn clean install
echo   mvn spring-boot:run
echo.
pause