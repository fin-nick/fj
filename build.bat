echo off
rem ant directory, path to "bin"
set PATH=CHANGE ME
rem ant directory
set ANT_HOME=CHANGE ME
rem java directory, path to "jdk"
set JAVA_HOME=CHANGE ME
rem source code directory
cd /D CHANGE ME
rem commands
ant clean && ant dist-all