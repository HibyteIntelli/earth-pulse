export type ReadingLevel = 'DEFAULT' | 'SIMPLIFIED';

export interface SignupRequest {
  email: string;
  password: string;
  name: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
}

export interface UserProfile {
  id: string;
  email: string;
  name: string;
  readingLevel: ReadingLevel;
  profilePictureUrl: string | null;
  createdAt: string;
}

export interface UpdateAccountRequest {
  email?: string;
  name?: string;
  currentPassword?: string;
  newPassword?: string;
  profilePictureUrl?: string;
  readingLevel?: ReadingLevel;
}

export interface ApiError {
  message: string;
  fields?: Record<string, string>;
}