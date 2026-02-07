@echo off
REM Set the path to your JavaCC JAR
SET JAR="C:\Users\Mubeen\Documents\IntelliJ\javacc-7.0.13.jar"

REM Run JavaCC with all arguments passed to this script
java -classpath %JAR% javacc %*
