# Command Recipes

## Preflight
```bash
git status -sb
git branch --show-current
java -version
node -v
npm -v
ps -o pid,ppid,command -ax | rg 'spring-boot:run|SampleApplication|tomcat'
```

## Build and targeted tests
```bash
mvn -DskipTests install
mvn test -Dtest=ResolvedStorybookConfigTest
```

## Sample app startup
```bash
cd sample
../mvnw spring-boot:run -DskipTests
```

## E2E
```bash
cd ..
npm run test:e2e
```

## Recovery
```bash
lsof -n -iTCP:6006 -sTCP:LISTEN
kill <PID>
```
