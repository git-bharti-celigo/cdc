# Minimal MongoDB Setup for Windows Client Testing

## Option 1: MongoDB Community Server (No Docker)

### Download & Install:
1. Go to: https://www.mongodb.com/try/download/community
2. Download "Windows x64" MSI installer
3. Install with default settings
4. MongoDB will run as Windows service automatically

### Test Connection:
```cmd
# MongoDB should be running on localhost:27017
# Test with MongoDB Compass (GUI) or mongo shell
```

## Option 2: Portable MongoDB (No Installation)

### Download Portable:
1. Download MongoDB ZIP (not MSI)
2. Extract to `C:\mongodb\`
3. Create data directory: `C:\mongodb\data\`

### Start Manually:
```cmd
cd C:\mongodb\bin
mongod.exe --dbpath C:\mongodb\data
```

## Option 3: Use Agent's Built-in Test Data

### Mock MongoDB Changes:
Instead of real MongoDB, we can simulate CDC events by:

1. **Agent triggers fake events** when certain conditions occur
2. **Test with file changes** (watch a directory for new files)
3. **Use Agent's existing database connections** to simulate changes

## Testing CDC Without MongoDB:

The CDC service can be tested by:
1. **Health endpoint**: `http://localhost:9123/health`
2. **Debezium disabled**: Safe to run without any database
3. **Log verification**: Check Agent logs for CDC service startup

## Recommended for Client Testing:
- **Option 1** (MongoDB Community) for real CDC testing
- **Option 3** (Mock events) for basic functionality testing
