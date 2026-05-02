const assert = require('node:assert/strict');
const fs = require('node:fs');
const test = require('node:test');

const {
  CENTRAL_DEPLOYMENTS_URL,
  formatCentralPublishStatus,
  parseCentralPublishStatus,
} = require('../../scripts/summarize-central-publish');

test('parseCentralPublishStatus reports manual publish required with deployment id and URL', () => {
  const summary = parseCentralPublishStatus(`
    [INFO] Deployment ID: 4cc8f928-65c6-47d0-983b-24a451f11453
    [INFO] Deployment validated successfully.
    [INFO] This deployment requires manual publish in Central Portal.
  `);

  assert.deepEqual(summary, {
    deploymentId: '4cc8f928-65c6-47d0-983b-24a451f11453',
    status: 'manual-publish-required',
    manualPublishUrl: CENTRAL_DEPLOYMENTS_URL,
  });
});

test('parseCentralPublishStatus prefers Central deploymentId over unrelated UUIDs in noisy logs', () => {
  const log = fs.readFileSync('tests/fixtures/central-publishing/noisy-manual-publish.txt', 'utf8');

  const summary = parseCentralPublishStatus(log);

  assert.deepEqual(summary, {
    deploymentId: '3ab9d8d4-9a62-4835-8751-c7766d3ce6b8',
    status: 'manual-publish-required',
    manualPublishUrl: CENTRAL_DEPLOYMENTS_URL,
  });
});

test('parseCentralPublishStatus distinguishes uploaded, validated, and published states', () => {
  assert.equal(parseCentralPublishStatus('Created deployment id: 11111111-1111-1111-1111-111111111111').status, 'uploaded');
  assert.equal(parseCentralPublishStatus('Deployment validation passed for 22222222-2222-2222-2222-222222222222').status, 'validated');
  assert.equal(parseCentralPublishStatus('Deployment artifact published for 33333333-3333-3333-3333-333333333333').status, 'published');
});

test('parseCentralPublishStatus reads published release log fixture', () => {
  const log = fs.readFileSync('tests/fixtures/central-publishing/published-release.txt', 'utf8');

  const summary = parseCentralPublishStatus(log);

  assert.deepEqual(summary, {
    deploymentId: '22222222-2222-2222-2222-222222222222',
    status: 'published',
    manualPublishUrl: '',
  });
});

test('parseCentralPublishStatus handles Central state-token wording', () => {
  assert.equal(
    parseCentralPublishStatus('Deployment 44444444-4444-4444-4444-444444444444 has been VALIDATED').status,
    'validated',
  );
  assert.equal(
    parseCentralPublishStatus('Deployment 55555555-5555-5555-5555-555555555555 state: PUBLISHED').status,
    'published',
  );
});

test('parseCentralPublishStatus does not treat manual publish guidance as already published', () => {
  const summary = parseCentralPublishStatus(
    'Deployment 66666666-6666-6666-6666-666666666666 can be published manually in the Central Portal',
  );

  assert.equal(summary.status, 'manual-publish-required');
  assert.equal(summary.manualPublishUrl, CENTRAL_DEPLOYMENTS_URL);
});

test('formatCentralPublishStatus creates a release-note-ready summary', () => {
  const output = formatCentralPublishStatus({
    deploymentId: '4cc8f928-65c6-47d0-983b-24a451f11453',
    status: 'manual-publish-required',
    manualPublishUrl: CENTRAL_DEPLOYMENTS_URL,
  });

  assert.match(output, /Maven Central status: manual-publish-required/);
  assert.match(output, /Deployment ID: 4cc8f928-65c6-47d0-983b-24a451f11453/);
  assert.match(output, /Manual publish URL: https:\/\/central\.sonatype\.com\/publishing\/deployments/);
  assert.match(output, /Release note: Maven Central deployment 4cc8f928-65c6-47d0-983b-24a451f11453 is manual-publish-required\./);
});
