export type ReadingLevel = 'DEFAULT' | 'SIMPLIFIED';

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