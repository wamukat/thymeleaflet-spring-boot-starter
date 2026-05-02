#!/usr/bin/env node

const fs = require('node:fs');

const CENTRAL_DEPLOYMENTS_URL = 'https://central.sonatype.com/publishing/deployments';

function parseCentralPublishStatus(logText) {
  const text = String(logText ?? '');
  const deploymentId = extractDeploymentId(text);
  const manualPublishRequired = /manual(?:ly)?\s+publish|publish(?:ed)?\s+manual(?:ly)?|manual\s+portal|required.*publish|publish.*required|can\s+be\s+published/i.test(text);
  const published = !manualPublishRequired && /(?:deployment|artifact|state).*(?:published|released)|(?:published|released).*(?:deployment|artifact|state)|\bPUBLISHED\b|\bRELEASED\b/i.test(text);
  const validated = /validat(?:ed|ion).*(?:success|passed|complete)|(?:success|passed|complete).*validat(?:ed|ion)|\bVALIDATED\b/i.test(text);
  const uploaded = /uploaded|created deployment|deployment id|deployment name/i.test(text) || deploymentId !== '';

  return {
    deploymentId,
    status: resolveStatus({ manualPublishRequired, published, validated, uploaded }),
    manualPublishUrl: manualPublishRequired ? CENTRAL_DEPLOYMENTS_URL : '',
  };
}

function extractDeploymentId(text) {
  const labelledMatch = text.match(/deployment(?:\s+(?:id|name))?\s*[:=]\s*([0-9a-f]{8}-[0-9a-f-]{27,})/i);
  if (labelledMatch) {
    return labelledMatch[1];
  }
  const uuidMatch = text.match(/\b[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\b/i);
  return uuidMatch ? uuidMatch[0] : '';
}

function resolveStatus({ manualPublishRequired, published, validated, uploaded }) {
  if (manualPublishRequired) {
    return 'manual-publish-required';
  }
  if (published) {
    return 'published';
  }
  if (validated) {
    return 'validated';
  }
  if (uploaded) {
    return 'uploaded';
  }
  return 'unknown';
}

function formatCentralPublishStatus(summary) {
  const lines = [
    `Maven Central status: ${summary.status}`,
    `Deployment ID: ${summary.deploymentId || '(not found)'}`,
  ];
  if (summary.manualPublishUrl) {
    lines.push(`Manual publish URL: ${summary.manualPublishUrl}`);
  }
  lines.push(`Release note: Maven Central deployment ${summary.deploymentId || '(unknown)'} is ${summary.status}.`);
  return lines.join('\n');
}

function readInput(path) {
  if (path && path !== '-') {
    return fs.readFileSync(path, 'utf8');
  }
  return fs.readFileSync(0, 'utf8');
}

function main(argv) {
  const logText = readInput(argv[2]);
  const summary = parseCentralPublishStatus(logText);
  process.stdout.write(`${formatCentralPublishStatus(summary)}\n`);
}

if (require.main === module) {
  main(process.argv);
}

module.exports = {
  CENTRAL_DEPLOYMENTS_URL,
  parseCentralPublishStatus,
  formatCentralPublishStatus,
};
