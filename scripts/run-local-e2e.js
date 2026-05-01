#!/usr/bin/env node

const http = require('node:http');
const { spawn } = require('node:child_process');

const isWindows = process.platform === 'win32';
const npmCommand = isWindows ? 'npm.cmd' : 'npm';
const baseUrl = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:6006';
const healthUrl = new URL('/thymeleaflet', baseUrl);

let sampleProcess;
let stopping = false;
let shutdownMethod = 'none';

async function main() {
  await runCommand(npmCommand, ['run', 'verify:starter']);

  sampleProcess = spawn(npmCommand, ['run', 'sample:start'], {
    stdio: ['ignore', 'pipe', 'pipe'],
    detached: !isWindows,
  });
  forwardSampleOutput(sampleProcess);

  sampleProcess.on('exit', (code, signal) => {
    if (isSuccessfulSampleStop({ stopping, shutdownMethod, code, signal })) {
      return;
    }
    if (!stopping) {
      console.error(`sample:start exited before E2E completed (code=${code}, signal=${signal})`);
      process.exitCode = code || 1;
      return;
    }
    console.error(`sample:start exited unexpectedly during shutdown (code=${code}, signal=${signal})`);
    process.exitCode = code || 1;
  });

  await waitForSample(healthUrl, 90_000);
  await runCommand(npmCommand, ['run', 'test:e2e']);
  console.log('\nLocal E2E completed successfully. Stopping sample app...');
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

function forwardSampleOutput(child) {
  child.stdout?.on('data', (chunk) => {
    if (shouldForwardSampleOutput(stopping)) {
      process.stdout.write(chunk);
    }
  });
  child.stderr?.on('data', (chunk) => {
    if (shouldForwardSampleOutput(stopping)) {
      process.stderr.write(chunk);
    }
  });
}

function shouldForwardSampleOutput(isStopping) {
  return !isStopping;
}

function isSuccessfulSampleStop({ stopping: isStopping, shutdownMethod: method, code, signal }) {
  if (!isStopping) {
    return false;
  }
  if (method === 'sigterm') {
    return code === 143 || signal === 'SIGTERM';
  }
  if (method === 'taskkill') {
    return code === 1 || code === 128 || signal === null;
  }
  return false;
}

function stopSample() {
  if (!sampleProcess || sampleProcess.exitCode !== null || stopping) {
    return;
  }
  stopping = true;
  if (isWindows) {
    shutdownMethod = 'taskkill';
    spawn('taskkill', ['/pid', String(sampleProcess.pid), '/t', '/f'], { stdio: 'ignore' });
  } else {
    shutdownMethod = 'sigterm';
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

if (require.main === module) {
  main()
    .catch((error) => {
      console.error(error.message);
      process.exitCode = 1;
    })
    .finally(() => {
      stopSample();
    });
}

module.exports = {
  isSuccessfulSampleStop,
  shouldForwardSampleOutput,
};
