@echo off

REM Loosely based on https://stackoverflow.com/questions/2872105/alternative-timestamping-services-for-authenticode

set timestampErrors=0

for /L %%a in (1,1,50) do (

    REM Try to sign the file.
    REM This operation is unreliable and may need to be repeated.

    echo Running %~dp0\signtool.exe timestamp /tr  http://timestamp.digicert.com /td SHA256 /f %1

    %~dp0\signtool.exe timestamp /tr http://timestamp.digicert.com /td SHA256 %1

    REM Check the return value of the signing operation
    REM if ERRORLEVEL matches anything above it
    if ERRORLEVEL 0 if not ERRORLEVEL 1 GOTO succeeded

    echo Signing failed - %ERRORLEVEL%.
    set /a timestampErrors+=1

    sleep 10
)

REM return an error code...
echo Timestamp failed. There were %timestampErrors% timestamping errors.
exit /b 1

:succeeded
REM return a successful code...
echo Timestamp successful. There were %timestampErrors% timestamping errors.
exit /b 0
