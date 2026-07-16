import { DeliveryMode } from '../../models/delivery-mode';
import { EventCategoryId } from '../../models/event-category';
import { ReadingLevel } from '../user/user.models';

export interface BoundingBox {
  minLat: number;
  maxLat: number;
  minLon: number;
  maxLon: number;
}

export interface Watch extends BoundingBox {
  id: string;
  name: string | null;
  categories: EventCategoryId[];
  digestMode: DeliveryMode;
  readingLevel: ReadingLevel;
  active: boolean;
  createdAt: string;
}

export interface WatchRequest extends BoundingBox {
  name?: string;
  categories: EventCategoryId[];
  digestMode: DeliveryMode;
  readingLevel?: ReadingLevel;
}

export interface WatchUpdate extends Partial<BoundingBox> {
  name?: string;
  categories?: EventCategoryId[];
  digestMode?: DeliveryMode;
  readingLevel?: ReadingLevel;
  active?: boolean;
}