import { EventCategoryId } from '../../models/event-category';

export type ReadingLevel = 'DEFAULT' | 'SIMPLIFIED';

export type Severity = 'LOW' | 'MODERATE' | 'HIGH' | 'UNKNOWN';

export interface Briefing {
  eventId: string;
  readingLevel: ReadingLevel;
  generatedAt: string;
  summary: string;
  impact: string;
  severity: Severity;
  precautions: string[];
}

export interface BriefingQuery {
  readingLevel: ReadingLevel;
  magnitudeLevel: number;
  category: EventCategoryId;
}
