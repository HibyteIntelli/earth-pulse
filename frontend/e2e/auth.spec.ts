import { test, expect, signup } from './fixtures';

test.describe('Authentication', () => {
  test('a new operator can register and then sign in', async ({ page, testUser }) => {
    await page.goto('/register');

    await page.getByLabel('Email').fill(testUser.email);
    await page.getByLabel('Operator name').fill(testUser.name);
    await page.getByLabel('Password').fill(testUser.password);
    await page.getByLabel('Confirm').fill(testUser.password);
    await page.locator('#consent').check();
    await page.getByRole('button', { name: 'Commission operator' }).click();

    await expect(page).toHaveURL(/\/login\?registered=1/);
    await expect(page.getByText('Operator commissioned. Sign in to continue.')).toBeVisible();

    await page.getByLabel('Email').fill(testUser.email);
    await page.getByLabel('Password').fill(testUser.password);
    await page.getByRole('button', { name: 'Authenticate' }).click();

    await expect(page).toHaveURL(/\/map$/);
    await expect(page.getByRole('button', { name: 'Logout' })).toBeVisible();
  });

  test('shows an error for invalid credentials', async ({ page, testUser, request }) => {
    await signup(request, testUser);

    await page.goto('/login');
    await page.getByLabel('Email').fill(testUser.email);
    await page.getByLabel('Password').fill('TotallyWrongPassword9');
    await page.getByRole('button', { name: 'Authenticate' }).click();

    await expect(page.getByText(/invalid email or password/i)).toBeVisible();
    await expect(page).toHaveURL(/\/login$/);
  });

  test('logout ends the session and returns to the public nav', async ({ page, authedUser }) => {
    await page.goto('/map');
    await expect(page.getByRole('button', { name: 'Logout' })).toBeVisible();

    await page.getByRole('button', { name: 'Logout' }).click();
    await page.getByRole('button', { name: 'Disconnect' }).click();

    await expect(page).toHaveURL(/\/login$/);
    await expect(page.getByRole('link', { name: 'Login' })).toBeVisible();

    await page.goto('/watches');
    await expect(page).toHaveURL(/\/login/);
  });
});