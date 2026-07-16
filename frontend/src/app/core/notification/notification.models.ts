import { DeliveryMode } from '../../models/delivery-mode';
import { EventCategoryId } from '../../models/event-category';
import { Severity } from '../llm/briefing.models';
import { ReadingLevel } from '../user/user.models';

export interface BriefingSnapshot {
  summary: string;
  impact: string;
  severity: Severity;
  precautions: string[];
}

export interface NotificationItem {
  id: string;
  watchId: string;
  eventId: string;
  eventTitle: string;
  eventCategories: EventCategoryId[];
  eventUrl: string | null;
  eventDate: string | null;
  deliveryMode: DeliveryMode;
  readingLevel: ReadingLevel;
  deliveredAt: string | null;
  briefing: BriefingSnapshot | null;
}

export interface NotificationPage {
  items: NotificationItem[];
  total: number;
  limit: number;
  offset: number;
}

export interface NotificationFilter {
  eventId?: string;
  category?: EventCategoryId;
  deliveryMode?: DeliveryMode;
  since?: string;
  limit?: number;
  offset?: number;
}