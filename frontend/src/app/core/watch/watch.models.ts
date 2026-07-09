import { EventCategoryId } from '../../models/event-category';
import { ReadingLevel } from '../user/user.models';

export interface RegionBounds {
  minLat: number;
  maxLat: number;
  minLon: number;
  maxLon: number;
}

export type DigestMode = 'IMMEDIATE' | 'DAILY';

export const DIGEST_MODES: readonly DigestMode[] = ['IMMEDIATE', 'DAILY'];

const DIGEST_MODE_LABEL: Readonly<Record<DigestMode, string>> = {
  IMMEDIATE: 'Immediate',
  DAILY: 'Daily digest',
};

export function digestModeLabel(mode: DigestMode): string {
  return DIGEST_MODE_LABEL[mode] ?? mode;
}

export const READING_LEVELS: readonly ReadingLevel[] = ['DEFAULT', 'SIMPLIFIED'];

const READING_LEVEL_LABEL: Readonly<Record<ReadingLevel, string>> = {
  DEFAULT: 'Default',
  SIMPLIFIED: 'Simplified',
};

export function readingLevelLabel(level: ReadingLevel): string {
  return READING_LEVEL_LABEL[level] ?? level;
}

export interface Watch {
  id: string;
  name: string | null;
  minLat: number;
  maxLat: number;
  minLon: number;
  maxLon: number;
  categories: EventCategoryId[];
  digestMode: DigestMode;
  readingLevel: ReadingLevel;
  active: boolean;
  createdAt: string;
}

export interface WatchRequest {
  name?: string | null;
  minLat: number;
  maxLat: number;
  minLon: number;
  maxLon: number;
  categories: EventCategoryId[];
  digestMode: DigestMode;
  readingLevel?: ReadingLevel;
}

export interface WatchFormValue {
  name: string | null;
  region: RegionBounds;
  categories: EventCategoryId[];
  digestMode: DigestMode;
  readingLevel: ReadingLevel;
  active: boolean;
}

export type WatchUpdate = Partial<{
  name: string | null;
  minLat: number;
  maxLat: number;
  minLon: number;
  maxLon: number;
  categories: EventCategoryId[];
  digestMode: DigestMode;
  readingLevel: ReadingLevel;
  active: boolean;
}>;