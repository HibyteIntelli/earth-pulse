import { EventCategoryId } from '../../models/event-category';

export type EventStatus = 'open' | 'closed';

export type EventStatusFilter = EventStatus | 'all';

export type EventSort = 'eventDate:desc' | 'eventDate:asc' | 'ingestedAt:desc' | 'ingestedAt:asc';

export interface EventGeometry {
  type: string;
  coordinates: [number, number];
}

export interface Event {
  id: string;
  title: string;
  description: string | null;
  sourceUrl: string | null;
  status: EventStatus;
  closedAt: string | null;
  category: EventCategoryId[];
  geometry: EventGeometry | null;
  eventDate: string | null;
  magnitudeValue: number | null;
  magnitudeUnit: string | null;
  ingestedAt: string;
  updatedAt: string;
}

export interface EventPage {
  items: Event[];
  total: number;
  page: number;
  size: number;
}

export interface EventFilter {
  bbox?: string;
  category?: EventCategoryId[];
  status?: EventStatusFilter;
  start?: string;
  end?: string;
  since?: string;
  sort?: EventSort;
  size?: number;
  page?: number;
}
