const assert = require('node:assert/strict');
const fs = require('node:fs');
const test = require('node:test');

const workflow = fs.readFileSync('.github/workflows/verify.yml', 'utf8');

test('verify workflow runs on pull requests and main pushes', () => {
  assert.match(workflow, /^on:\n/m);
  assert.match(workflow, /pull_request:/);
  assert.match(workflow, /push:/);
  assert.match(workflow, /branches:\s*\n\s+- main/);
});

test('verify workflow uses repository build toolchain versions', () => {
  assert.match(workflow, /java-version:\s*'21'/);
  assert.match(workflow, /node-version:\s*20/);
});

test('verify workflow runs Maven tests and local E2E helper', () => {
  assert.match(workflow, /run:\s*\.\/mvnw test -q/);
  assert.match(workflow, /run:\s*npm run test:workflow/);
  assert.match(workflow, /run:\s*npm run test:e2e:local/);
});

test('verify workflow installs npm dependencies before workflow checks, Playwright, and E2E', () => {
  const installIndex = workflow.indexOf('run: npm ci');
  const workflowTestIndex = workflow.indexOf('run: npm run test:workflow');
  const playwrightInstallIndex = workflow.indexOf('run: npx playwright install --with-deps chromium');
  const e2eIndex = workflow.indexOf('run: npm run test:e2e:local');

  assert.notEqual(installIndex, -1);
  assert.ok(installIndex < workflowTestIndex);
  assert.ok(workflowTestIndex < playwrightInstallIndex);
  assert.ok(playwrightInstallIndex < e2eIndex);
});
