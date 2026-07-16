import { Locator, Page } from '@playwright/test';
import { test, expect } from './fixtures';

test.describe.configure({ mode: 'serial' });

async function openFirstEventMarker(page: Page): Promise<Locator> {
  const eventsLoaded = page.waitForResponse(
    (res) => res.url().includes('/ingestion/events/search') && res.ok(),
  );
  await page.goto('/map');
  await eventsLoaded;

  const marker = page.locator('.leaflet-marker-icon').first();
  await expect(marker).toBeVisible();

  const eventLoaded = page.waitForResponse(
    (res) => /\/ingestion\/events\/[^/?]+$/.test(res.url()) && res.ok(),
  );
  await marker.dispatchEvent('click');
  await eventLoaded;
  return marker;
}

test.describe('Map event panel', () => {
  test('anonymous visitors see event details but a login gate for the AI briefing', async ({
    page,
  }) => {
    await openFirstEventMarker(page);

    await expect(page.getByText('Event ID')).toBeVisible();
    await expect(
      page.getByText('Field briefings are restricted to registered operators.'),
    ).toBeVisible();
    await expect(page.getByRole('link', { name: /log in to view ai brief/i })).toBeVisible();
  });

  test('a logged-in operator can request a briefing, and reopening the same event reuses it', async ({
    page,
    authedUser,
  }) => {
    test.setTimeout(400_000);
    const marker = await openFirstEventMarker(page);

    await expect(page.getByText('Event ID')).toBeVisible();
    await expect(page.getByRole('link', { name: /log in to view ai brief/i })).not.toBeVisible();

    const briefingRegion = page.getByRole('region', { name: 'AI briefing' });
    await expect(briefingRegion).toBeVisible();

    await expect(briefingRegion.getByText(/could not be reached/i)).not.toBeVisible();
    const detailedButton = briefingRegion.getByRole('button', { name: 'Show detailed briefing' });
    await expect(detailedButton).toBeVisible({ timeout: 380_000 });

    const eventReloaded = page.waitForResponse(
      (res) => /\/ingestion\/events\/[^/?]+$/.test(res.url()) && res.ok(),
    );
    await page.getByRole('button', { name: 'Close' }).click();
    await marker.dispatchEvent('click');
    await eventReloaded;
    await expect(detailedButton).toBeVisible();
  });
});