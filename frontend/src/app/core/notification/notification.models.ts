import { EventCategoryId } from '../../models/event-category';
import { DeliveryMode } from '../../models/delivery-mode';
import { ReadingLevel } from '../user/user.models';

export type Severity = 'LOW' | 'MODERATE' | 'HIGH' | 'UNKNOWN';

const SEVERITY_LABEL: Readonly<Record<Severity, string>> = {
  LOW: 'Low',
  MODERATE: 'Moderate',
  HIGH: 'High',
  UNKNOWN: 'Unknown',
};

export function severityLabel(severity: Severity): string {
  return SEVERITY_LABEL[severity] ?? severity;
}

export interface BriefingSnapshot {
  summary: string;
  impact: string;
  severity: Severity;
  precautions: string[];
}

export interface Notification {
  id: string;
  watchId: string;
  eventId: string;
  eventTitle: string;
  eventCategories: EventCategoryId[];
  eventUrl: string;
  eventDate: string;
  deliveryMode: DeliveryMode;
  readingLevel: ReadingLevel;
  deliveredAt: string;
  briefing: BriefingSnapshot;
}

export interface NotificationPage {
  items: Notification[];
  total: number;
  limit: number;
  offset: number;
}