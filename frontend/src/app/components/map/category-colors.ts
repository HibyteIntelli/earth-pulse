import { EventCategoryId } from '../../models/event-category';

export const CATEGORY_COLOR: Readonly<Record<EventCategoryId, string>> = {
  drought: 'var(--ev-drought)',
  dustHaze: 'var(--ev-dust)',
  earthquakes: 'var(--ev-quake)',
  floods: 'var(--ev-flood)',
  landslides: 'var(--ev-landslide)',
  manmade: 'var(--ink-dim)',
  seaLakeIce: 'var(--water)',
  severeStorms: 'var(--ev-storm)',
  snow: 'var(--water-deep)',
  tempExtremes: 'var(--ev-wildfire)',
  volcanoes: 'var(--ev-volcano)',
  waterColor: 'var(--forest)',
  wildfires: 'var(--ev-wildfire)',
};

export function colorForCategory(id: EventCategoryId): string {
  return CATEGORY_COLOR[id];
}