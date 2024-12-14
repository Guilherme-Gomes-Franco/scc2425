@echo off

mvn install && copy target\*.war . && docker build -t jmlcoliveira/blob-http-trigger . && docker push jmlcoliveira/blob-http-trigger

pause