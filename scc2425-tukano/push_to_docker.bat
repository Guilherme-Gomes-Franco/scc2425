@echo off

mvn install && copy target\*.war . && docker build -t jmlcoliveira/scc2425-webapp . && docker push jmlcoliveira/scc2425-webapp

pause