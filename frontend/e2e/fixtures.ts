import { test as base, APIRequestContext } from '@playwright/test';

const TOKEN_KEY = 'earthpulse.token';

export interface TestUser {
  email: string;
  name: string;
  password: string;
}

export function uniqueUser(prefix: string): TestUser {
  const id = `${Date.now()}-${Math.floor(Math.random() * 1_000_000)}`;
  return {
    email: `e2e-${prefix}-${id}@earth-pulse.test`,
    name: `E2E ${prefix}`,
    password: 'CorrectHorseBatteryStaple9',
  };
}

export async function signup(request: APIRequestContext, user: TestUser): Promise<void> {
  const res = await request.post('/api/auth/signup', { data: user });
  if (!res.ok()) {
    throw new Error(`Signup failed: ${res.status()} ${await res.text()}`);
  }
}

export async function login(request: APIRequestContext, user: TestUser): Promise<string> {
  const res = await request.post('/api/auth/login', {
    data: { email: user.email, password: user.password },
  });
  if (!res.ok()) {
    throw new Error(`Login failed: ${res.status()} ${await res.text()}`);
  }
  const body = (await res.json()) as { token: string };
  return body.token;
}

export const test = base.extend<{
  testUser: TestUser;
  authedUser: TestUser;
}>({
  testUser: async ({}, use) => {
    await use(uniqueUser('user'));
  },

  authedUser: async ({ page, request }, use) => {
    const user = uniqueUser('authed');
    await signup(request, user);
    const token = await login(request, user);

    await page.goto('/login');
    await page.evaluate(
      ([key, value]) => window.localStorage.setItem(key, value),
      [TOKEN_KEY, token],
    );

    await use(user);
  },
});

export { expect } from '@playwright/test';