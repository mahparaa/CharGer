@echo off
REM Set the path to your JavaCC JAR
SET JAR="<SETUP_YOUR_JAVACC>"

REM Run JavaCC with all arguments passed to this script
java -classpath %JAR% javacc %*
