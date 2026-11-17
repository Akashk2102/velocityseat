@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    https://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM ----------------------------------------------------------------------------
@REM Maven Start Up Batch script
@REM ----------------------------------------------------------------------------

@IF "%DEBUG%" == "" @ECHO OFF
@REM set %ENABLE_DELAYED_EXPANSION% to on if you need delayed expansion
SETLOCAL ENABLEEXTENSIONS ENABLEDELAYEDEXPANSION

SET "BASE_DIR=%~dp0"
SET "MAVEN_PROJECTBASEDIR=%BASE_DIR%"
IF NOT "%MAVEN_PROJECTBASEDIR:~-1%"=="\" SET "MAVEN_PROJECTBASEDIR=%MAVEN_PROJECTBASEDIR%\"

SET "WRAPPER_JAR=%BASE_DIR%.mvn\wrapper\maven-wrapper.jar"
SET "WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain"

IF EXIST "%WRAPPER_JAR%" GOTO runMaven

@REM Download wrapper jar if not present
SET "WRAPPER_PROPERTIES=%BASE_DIR%.mvn\wrapper\maven-wrapper.properties"
FOR /F "tokens=1,2 delims==" %%A IN ('findstr /R "^wrapperUrl=" "%WRAPPER_PROPERTIES%"') DO SET "WRAPPER_URL=%%B"
IF "%WRAPPER_URL%"=="" SET "WRAPPER_URL=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"

ECHO Downloading Maven Wrapper jar from %WRAPPER_URL% ...
powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; (New-Object Net.WebClient).DownloadFile('%WRAPPER_URL%', '%WRAPPER_JAR%')"
IF NOT EXIST "%WRAPPER_JAR%" (
    ECHO ERROR: Failed to download Maven Wrapper JAR. Please check your internet connection.
    EXIT /B 1
)

:runMaven
IF DEFINED JAVA_HOME (
    SET "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
) ELSE (
    SET "JAVA_EXE=java.exe"
)

"%JAVA_EXE%" %JVM_CONFIG_MAVEN_PROPS% %MAVEN_OPTS% %MAVEN_DEBUG_OPTS% -classpath "%WRAPPER_JAR%" "-Dmaven.home=%BASE_DIR%.mvn\wrapper" "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" %WRAPPER_LAUNCHER% %*
IF ERRORLEVEL 1 EXIT /B %ERRORLEVEL%
