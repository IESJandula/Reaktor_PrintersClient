@echo off
set JAVA_HOME=C:\Program Files\Java\jdk-17.0.2
set JAR_PATH=C:\Users\usuario\git\Reaktor_PrintersClient\target\ReaktorPrintersClient-1.0.0.jar
set SERVICE_NAME=Reaktor_PrintersClient

%JAVA_HOME%\bin\javaw -jar %JAR_PATH%

REM Eliminar el servicio: sc delete ReaktorPrintersClients
REM Creación del servicio: sc create ReaktorPrintersClients binPath= "C:\Users\usuario\Desktop\reaktor_printersclients_service.bat"
REM Arranque automático: sc config ReaktorPrintersClients start= auto
REM Primer arranque: sc start ReaktorPrintersClients