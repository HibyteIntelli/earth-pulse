import { Page } from '@playwright/test';
import { test, expect } from './fixtures';

async function drawWatchRegion(page: Page): Promise<void> {
  await page.getByRole('button', { name: /new watch/i }).click();
  await expect(page.getByText('Drag on the map to draw a watch region')).toBeVisible();

  const map = page.locator('.map-shell > .map');
  const box = await map.boundingBox();
  if (!box) throw new Error('Map container has no bounding box');

  const start = { x: box.x + box.width * 0.35, y: box.y + box.height * 0.35 };
  const end = { x: box.x + box.width * 0.65, y: box.y + box.height * 0.65 };

  await page.mouse.move(start.x, start.y);
  await page.mouse.down();
  await page.mouse.up();
  await page.mouse.move(end.x, end.y, { steps: 10 });
  await page.mouse.down();
  await page.mouse.up();

  await expect(page.getByRole('heading', { name: 'Chart this region' })).toBeVisible();
}

test.describe('Watches', () => {
  test('create, edit, pause and delete a watch', async ({ page, authedUser }) => {
    const originalName = `E2E watch ${Date.now()}`;
    const renamedTo = `${originalName} (renamed)`;

    await page.goto('/map');
    await drawWatchRegion(page);

    await page.getByLabel('Name (optional)').fill(originalName);
    await page.getByRole('checkbox', { name: 'Wildfires' }).click();
    await page.getByRole('button', { name: 'Create watch' }).click();
    await expect(page.getByRole('heading', { name: 'Chart this region' })).toBeHidden();

    await page.goto('/watches');
    const card = page.locator('.watch-card', { hasText: originalName });
    await expect(card).toBeVisible();
    await expect(card.getByText('Active', { exact: true })).toBeVisible();
    await expect(card.getByText('Wildfires')).toBeVisible();

    await card.getByRole('button', { name: 'Edit' }).click();
    await expect(page.getByRole('heading', { name: 'Adjust this region' })).toBeVisible();
    const nameField = page.getByLabel('Name (optional)');
    await nameField.fill('');
    await nameField.fill(renamedTo);
    await page.getByRole('button', { name: 'Save changes' }).click();
    await expect(page.getByRole('heading', { name: 'Adjust this region' })).toBeHidden();

    const renamedCard = page.locator('.watch-card', { hasText: renamedTo });
    await expect(renamedCard).toBeVisible();

    await renamedCard.getByRole('button', { name: 'Pause' }).click();
    await expect(renamedCard.getByText('Paused', { exact: true })).toBeVisible();
    await expect(renamedCard.getByRole('button', { name: 'Resume' })).toBeVisible();

    await renamedCard.getByRole('button', { name: 'Delete' }).click();
    await expect(page.getByText('Delete this watch permanently?')).toBeVisible();
    await page.getByRole('button', { name: 'Confirm delete' }).click();
    await expect(page.locator('.watch-card', { hasText: renamedTo })).toHaveCount(0);
  });
});
