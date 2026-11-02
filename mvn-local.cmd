@echo off
rem ---------------------------------------------------------------------------
rem Local build helper for the STC platform.
rem Pins JAVA_HOME to the JDK that ships with this machine and uses a project
rem local Maven repository so the build never touches the global ~/.m2 state.
rem ---------------------------------------------------------------------------
set "JAVA_HOME=C:\Program Files\OpenLogic\jdk-8.0.452.09-hotspot"
set "MAVEN=C:\Users\Galaxy\Documents\deepseek-harness\default-workspace\sstc-project\tools\apache-maven-3.9.9\bin\mvn.cmd"
"%MAVEN%" -f "%~dp0pom.xml" -Dmaven.repo.local="%~dp0.m2repo" %*
