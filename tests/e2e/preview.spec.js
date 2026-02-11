const { test, expect } = require('@playwright/test');

async function openFragment(page, fragmentName) {
  await page.goto('/thymeleaflet/');
  const link = page.getByRole('link', { name: new RegExp(`^${fragmentName}\\b`, 'i') });
  await expect(link).toBeVisible();
  await link.click();
  await page.waitForSelector('#fragment-preview-host iframe');
  await expect(page.locator('#preview-container')).toBeVisible();
}

async function selectStory(page, storyName) {
  const storyLink = page.getByRole('link', { name: new RegExp(`^${storyName}\\b`, 'i') });
  await expect(storyLink).toBeVisible();
  await storyLink.click();
}

test('simpleCard preview matches snapshot', async ({ page }) => {
  await openFragment(page, 'simpleCard');
  const preview = page.locator('#preview-container');
  await expect(preview).toHaveScreenshot('simpleCard-preview.png');
});

test('mobile menu button opens fragment sidebar', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto('/thymeleaflet/');
  const sidebar = page.locator('.nav-sidebar');
  const openButton = page.locator(
    '#sidebar-open-button-placeholder, #sidebar-open-button-welcome, #sidebar-open-button'
  ).first();
  await expect(openButton).toBeVisible();
  await expect(sidebar).not.toHaveClass(/mobile-open/);

  await openButton.click();
  await expect(sidebar).toHaveClass(/mobile-open/);
  await expect(page.locator('#sidebar-close-button')).toBeVisible();

  await page.locator('#sidebar-close-button').click();
  await expect(sidebar).not.toHaveClass(/mobile-open/);
});

test('selectInput fullscreen matches snapshot', async ({ page }) => {
  await openFragment(page, 'selectInput');
  const fullscreenButton = page.getByRole('button', { name: /fullscreen/i });
  await expect(fullscreenButton).toBeVisible();
  await fullscreenButton.click();

  const overlay = page.locator('#preview-fullscreen-overlay');
  await expect(overlay).toHaveClass(/preview-fullscreen-active/);
  await expect(overlay).toHaveScreenshot('selectInput-fullscreen.png');

  await page.keyboard.press('Escape');
  await expect(overlay).not.toHaveClass(/preview-fullscreen-active/);
});

test('story selection updates URL', async ({ page }) => {
  await openFragment(page, 'selectInput');
  await selectStory(page, 'Custom');
  await expect(page).toHaveURL(/\/thymeleaflet\/components\.form-select\/selectInput\/custom$/);
});

test('custom parameters update preview', async ({ page }) => {
  await openFragment(page, 'selectInput');
  await selectStory(page, 'Custom');
  await expect(page).toHaveURL(/\/thymeleaflet\/components\.form-select\/selectInput\/custom$/);
  await page.waitForSelector('#fragment-preview-host iframe');
  await page.waitForFunction(() => {
    const iframe = document.querySelector('#fragment-preview-host iframe');
    return iframe && iframe.contentDocument && iframe.contentDocument.readyState === 'complete';
  });
  const frameLocator = page.frameLocator('#fragment-preview-host iframe');
  const firstSelect = frameLocator.locator('select').first();
  const options = await firstSelect.locator('option').evaluateAll(nodes =>
    nodes.map(node => node.value)
  );
  expect(options.length).toBeGreaterThan(1);
  const initialValue = await firstSelect.inputValue();
  const nextValue = options.find(value => value !== initialValue) || options[1];
  await firstSelect.selectOption(nextValue);
  await expect(firstSelect).toHaveValue(nextValue);
});

test('preview iframe does not show error page', async ({ page }) => {
  await openFragment(page, 'simpleCard');
  const iframe = page.locator('#fragment-preview-host iframe');
  const frame = await iframe.contentFrame();
  await expect(frame).not.toBeNull();
  const bodyText = await frame.locator('body').innerText();
  expect(bodyText).not.toContain('System Error');
  expect(bodyText).not.toContain('システムエラー');
});

test('viewport preset updates width badge', async ({ page }) => {
  await openFragment(page, 'simpleCard');
  const select = page.locator('#preview-viewport-select');
  await expect(select).toBeVisible();
  await select.selectOption({ label: 'Mobile Small' });

  const badge = page.locator('#preview-viewport-badge');
  await expect(badge).toBeVisible();
  const text = (await badge.textContent())?.trim();
  expect(text).toMatch(/\d+px/);
});

test('background toggle switches preview class', async ({ page }) => {
  await openFragment(page, 'simpleCard');
  const container = page.locator('#preview-container');
  const toggle = page.locator('#background-toggle');
  await expect(toggle).toBeVisible();

  await expect(container).toHaveClass(/preview-background-light|preview-background-gray/);
  const initiallyDark = await container.evaluate(el => el.classList.contains('preview-background-gray'));
  await toggle.click();
  if (initiallyDark) {
    await expect(container).toHaveClass(/preview-background-light/);
  } else {
    await expect(container).toHaveClass(/preview-background-gray/);
  }
});

test('copy usage button shows copied label', async ({ page }) => {
  await openFragment(page, 'simpleCard');
  await selectStory(page, 'Default');
  await expect(page.locator('#usage-example code')).toBeVisible();
  const result = await page.evaluate(() => {
    document.execCommand = () => true;
    try {
      copyUsageExample();
      return true;
    } catch (error) {
      return String(error);
    }
  });
  expect(result).toBe(true);
});
