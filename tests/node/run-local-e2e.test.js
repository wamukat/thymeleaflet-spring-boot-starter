const assert = require('node:assert/strict');
const test = require('node:test');

const {
  shouldForwardSampleOutput,
  isSuccessfulSampleStop,
} = require('../../scripts/run-local-e2e');

test('shouldForwardSampleOutput suppresses sample output after expected shutdown starts', () => {
  assert.equal(shouldForwardSampleOutput(false), true);
  assert.equal(shouldForwardSampleOutput(true), false);
});

test('isSuccessfulSampleStop accepts expected termination during shutdown only', () => {
  assert.equal(isSuccessfulSampleStop({ stopping: true, shutdownMethod: 'sigterm', code: 143, signal: null }), true);
  assert.equal(isSuccessfulSampleStop({ stopping: true, shutdownMethod: 'sigterm', code: null, signal: 'SIGTERM' }), true);
  assert.equal(isSuccessfulSampleStop({ stopping: true, shutdownMethod: 'taskkill', code: 1, signal: null }), true);
  assert.equal(isSuccessfulSampleStop({ stopping: false, shutdownMethod: 'sigterm', code: 143, signal: null }), false);
  assert.equal(isSuccessfulSampleStop({ stopping: true, shutdownMethod: 'sigterm', code: 1, signal: null }), false);
  assert.equal(isSuccessfulSampleStop({ stopping: true, shutdownMethod: 'unknown', code: 0, signal: null }), false);
});
