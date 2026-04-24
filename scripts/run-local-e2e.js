#!/usr/bin/env node

const http = require('node:http');
const { spawn } = require('node:child_process');

const isWindows = process.platform === 'win32';
const npmCommand = isWindows ? 'npm.cmd' : 'npm';
const baseUrl = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:6006';
const healthUrl = new URL('/thymeleaflet', baseUrl);

let sampleProcess;
let stopping = false;

async function main() {
  await runCommand(npmCommand, ['run', 'verify:starter']);

  sampleProcess = spawn(npmCommand, ['run', 'sample:start'], {
    stdio: 'inherit',
    detached: !isWindows,
  });

  sampleProcess.on('exit', (code, signal) => {
    if (!stopping) {
      console.error(`sample:start exited before E2E completed (code=${code}, signal=${signal})`);
      process.exitCode = code || 1;
    }
  });

  await waitForSample(healthUrl, 90_000);
  await runCommand(npmCommand, ['run', 'test:e2e']);
}

function runCommand(command, args) {
  return new Promise((resolve, reject) => {
    const child = spawn(command, args, { stdio: 'inherit' });
    child.on('error', reject);
    child.on('exit', (code, signal) => {
      if (code === 0) {
        resolve();
      } else {
        reject(new Error(`${command} ${args.join(' ')} failed (code=${code}, signal=${signal})`));
      }
    });
  });
}

async function waitForSample(url, timeoutMs) {
  const startedAt = Date.now();
  while (Date.now() - startedAt < timeoutMs) {
    if (await canReach(url)) {
      return;
    }
    await delay(1_000);
  }
  throw new Error(`Timed out waiting for sample app at ${url}`);
}

function canReach(url) {
  return new Promise((resolve) => {
    const request = http.get(url, (response) => {
      response.resume();
      resolve(response.statusCode >= 200 && response.statusCode < 300);
    });
    request.on('error', () => resolve(false));
    request.setTimeout(1_000, () => {
      request.destroy();
      resolve(false);
    });
  });
}

function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function stopSample() {
  if (!sampleProcess || sampleProcess.exitCode !== null || stopping) {
    return;
  }
  stopping = true;
  if (isWindows) {
    spawn('taskkill', ['/pid', String(sampleProcess.pid), '/t', '/f'], { stdio: 'ignore' });
  } else {
    process.kill(-sampleProcess.pid, 'SIGTERM');
  }
}

process.on('SIGINT', () => {
  stopSample();
  process.exit(130);
});

process.on('SIGTERM', () => {
  stopSample();
  process.exit(143);
});

main()
  .catch((error) => {
    console.error(error.message);
    process.exitCode = 1;
  })
  .finally(() => {
    stopSample();
  });
