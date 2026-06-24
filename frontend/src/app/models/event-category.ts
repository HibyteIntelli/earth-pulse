export type EventCategoryId =
  | 'drought'
  | 'dustHaze'
  | 'earthquakes'
  | 'floods'
  | 'landslides'
  | 'manmade'
  | 'seaLakeIce'
  | 'severeStorms'
  | 'snow'
  | 'tempExtremes'
  | 'volcanoes'
  | 'waterColor'
  | 'wildfires';

export interface EventCategory {
  id: EventCategoryId;
  title: string;
}

export const EVENT_CATEGORIES: readonly EventCategory[] = [
  { id: 'drought', title: 'Drought' },
  { id: 'dustHaze', title: 'Dust and Haze' },
  { id: 'earthquakes', title: 'Earthquakes' },
  { id: 'floods', title: 'Floods' },
  { id: 'landslides', title: 'Landslides' },
  { id: 'manmade', title: 'Manmade' },
  { id: 'seaLakeIce', title: 'Sea and Lake Ice' },
  { id: 'severeStorms', title: 'Severe Storms' },
  { id: 'snow', title: 'Snow' },
  { id: 'tempExtremes', title: 'Temperature Extremes' },
  { id: 'volcanoes', title: 'Volcanoes' },
  { id: 'waterColor', title: 'Water Color' },
  { id: 'wildfires', title: 'Wildfires' },
];

const CATEGORY_SHORT_CODE: Readonly<Record<EventCategoryId, string>> = {
  drought: 'DRGT',
  dustHaze: 'DUST',
  earthquakes: 'QUAK',
  floods: 'FLOD',
  landslides: 'SLID',
  manmade: 'MANM',
  seaLakeIce: 'ICE',
  severeStorms: 'STRM',
  snow: 'SNOW',
  tempExtremes: 'TEMP',
  volcanoes: 'VOLC',
  waterColor: 'WATR',
  wildfires: 'FIRE',
};

const BY_ID: ReadonlyMap<EventCategoryId, EventCategory> = new Map(
  EVENT_CATEGORIES.map((c) => [c.id, c]),
);

export function categoryTitle(id: EventCategoryId): string {
  return BY_ID.get(id)?.title ?? id;
}

export function categoryShortCode(id: EventCategoryId): string {
  return CATEGORY_SHORT_CODE[id] ?? id;
}