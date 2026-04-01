@echo off
setlocal enabledelayedexpansion

:: Название проекта и пакет
set PROJECT_NAME=UltimateItems
set PACKAGE_PATH=me\yourname\ultimateitems

echo Creating folder structure for %PROJECT_NAME%...

:: Создание папок
mkdir src\main\java\%PACKAGE_PATH%\commands
mkdir src\main\java\%PACKAGE_PATH%\teams
mkdir src\main\java\%PACKAGE_PATH%\items
mkdir src\main\java\%PACKAGE_PATH%\listeners
mkdir src\main\java\%PACKAGE_PATH%\utils
mkdir src\main\resources

:: Создание файлов (пустые заготовки)
type nul > src\main\java\%PACKAGE_PATH%\UltimateItems.java
type nul > src\main\java\%PACKAGE_PATH%\commands\TeamCommand.java
type nul > src\main\java\%PACKAGE_PATH%\commands\AdminCommand.java
type nul > src\main\java\%PACKAGE_PATH%\teams\TeamManager.java
type nul > src\main\java\%PACKAGE_PATH%\teams\Team.java
type nul > src\main\java\%PACKAGE_PATH%\items\ItemManager.java
type nul > src\main\java\%PACKAGE_PATH%\listeners\WeaponListener.java
type nul > src\main\java\%PACKAGE_PATH%\listeners\TeamListener.java
type nul > src\main\java\%PACKAGE_PATH%\utils\NBTUtils.java

:: Создание plugin.yml
(
echo name: %PROJECT_NAME%
echo version: 1.0
echo main: me.yourname.ultimateitems.UltimateItems
echo api-version: 1.21
echo commands:
echo   team:
echo     description: Основная команда для управления группами
echo   uitems:
echo     description: Админ-команда для выдачи предметов
) > src\main\resources\plugin.yml

:: Создание pom.xml (базовый для 1.21.4 Paper)
(
echo ^<?xml version="1.0" encoding="UTF-8"?^>
echo ^<project xmlns="http://maven.apache.org/POM/4.0.0"
echo          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
echo          xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd"^>
echo     ^<modelVersion>4.0.0^</modelVersion^>
echo     ^<groupId^>me.yourname^</groupId^>
echo     ^<artifactId^>%PROJECT_NAME%^</artifactId^>
echo     ^<version^>1.0^</version^>
echo     ^<properties^>
echo         ^<maven.compiler.source^>21^</maven.compiler.source^>
echo         ^<maven.compiler.target^>21^</maven.compiler.target^>
echo     ^</properties^>
echo     ^<repositories^>
echo         ^<repository^>
echo             ^<id^>papermc^</id^>
echo             ^<url^>https://repo.papermc.io/repository/maven-public/^</url^>
echo         ^</repository^>
echo     ^</repositories^>
echo     ^<dependencies^>
echo         ^<dependency^>
echo             ^<groupId^>io.papermc.paper^</groupId^>
echo             ^<artifactId^>paper-api^</artifactId^>
echo             ^<version^>1.21.4-R0.1-SNAPSHOT^</version^>
echo             ^<scope^>provided^</scope^>
echo         ^</dependency^>
echo     ^</dependencies^>
echo ^</project^>
) > pom.xml

echo Done! Structure created successfully.
pause