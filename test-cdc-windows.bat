@echo off
echo Testing CDC POC Service...
echo.

REM Test health endpoint
echo Testing health endpoint...
curl -s http://localhost:9123/health
if %errorlevel% neq 0 (
    echo ERROR: CDC service is not responding on port 9123
    echo Make sure the service is running first using start-cdc-windows.bat
    pause
    exit /b 1
)

echo.
echo SUCCESS: CDC service is running!
echo.

REM Test root endpoint
echo Testing root endpoint...
curl -s http://localhost:9123/
echo.
echo.

echo To test Debezium CDC:
echo 1. Install MongoDB: docker run -d --name mongo -p 27017:27017 mongo:latest
echo 2. Stop current CDC service
echo 3. Set DEBEZIUM_ENABLED=true in start-cdc-windows.bat
echo 4. Restart CDC service
echo 5. Insert test data: docker exec -it mongo mongosh
echo    use testdb
echo    db.testcol.insertOne({name: "test", value: 123})
echo 6. Watch console for CDC events

pause
