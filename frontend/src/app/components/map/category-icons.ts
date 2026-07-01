import * as L from 'leaflet';
import { EventCategoryId } from '../../models/event-category';

const make = (file: string): L.Icon =>
  L.icon({
    iconUrl: `assets/markers/${file}`,
    iconSize: [32, 32],
    iconAnchor: [16, 32],
    popupAnchor: [0, -32],
  });

const CATEGORY_ICONS: Record<EventCategoryId, L.Icon> = {
  drought: make('drought.png'),
  dustHaze: make('dust-haze.png'),
  earthquakes: make('earthquake.png'),
  floods: make('flood.png'),
  landslides: make('landslide.png'),
  manmade: make('manmade.png'),
  seaLakeIce: make('sea-lake-ice.png'),
  severeStorms: make('severe-storms.png'),
  snow: make('snow.png'),
  tempExtremes: make('temp-extremes.png'),
  volcanoes: make('volcano.png'),
  waterColor: make('water-color.png'),
  wildfires: make('wildfire.png'),
};

const DEFAULT_ICON = make('default.png');

export function iconFor(categories: EventCategoryId[]): L.Icon {
  return CATEGORY_ICONS[categories[0]] ?? DEFAULT_ICON;
}
