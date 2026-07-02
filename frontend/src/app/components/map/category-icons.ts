import * as L from 'leaflet';
import {
  createElement,
  Activity,
  CloudLightning,
  CloudSnow,
  Droplets,
  Factory,
  Flame,
  Haze,
  MapPin,
  Mountain,
  Snowflake,
  SunDim,
  Thermometer,
  Waves,
} from 'lucide';
import { EventCategoryId } from '../../models/event-category';

type IconNode = Parameters<typeof createElement>[0];

// Lucide has no volcano glyph, so define one in its own stroke style (24×24,
// stroke=currentColor, round joins) to stay visually consistent with the set.
const Volcano: IconNode = [
  ['path', { d: 'M8 8 4 20h16L16 8' }],
  ['path', { d: 'M8 8h8' }],
  ['path', { d: 'M12 8V3' }],
  ['path', { d: 'M9.5 8 8.5 4.5' }],
  ['path', { d: 'M14.5 8 15.5 4.5' }],
];

/** Category → (glyph, color token). Colors live in colors.css so the map,
 *  legend and filter UI stay in sync. */
const CATEGORY_GLYPHS: Record<EventCategoryId, { icon: IconNode; color: string }> = {
  drought: { icon: SunDim, color: '--ev-drought' },
  dustHaze: { icon: Haze, color: '--ev-dust' },
  earthquakes: { icon: Activity, color: '--ev-quake' },
  floods: { icon: Waves, color: '--ev-flood' },
  landslides: { icon: Mountain, color: '--ev-landslide' },
  manmade: { icon: Factory, color: '--ev-manmade' },
  seaLakeIce: { icon: Snowflake, color: '--ev-ice' },
  severeStorms: { icon: CloudLightning, color: '--ev-storm' },
  snow: { icon: CloudSnow, color: '--ev-snow' },
  tempExtremes: { icon: Thermometer, color: '--ev-temp' },
  volcanoes: { icon: Volcano, color: '--ev-volcano' },
  waterColor: { icon: Droplets, color: '--ev-water' },
  wildfires: { icon: Flame, color: '--ev-wildfire' },
};

const pin = (node: IconNode, color: string): L.DivIcon => {
  const svg = createElement(node);
  svg.setAttribute('width', '16');
  svg.setAttribute('height', '16');
  return L.divIcon({
    html: `<span class="event-pin" style="--pin-color: var(${color})">${svg.outerHTML}</span>`,
    className: '',
    iconSize: [28, 28],
    iconAnchor: [14, 14],
    popupAnchor: [0, -16],
    tooltipAnchor: [0, -16],
  });
};

const CATEGORY_ICONS = Object.fromEntries(
  Object.entries(CATEGORY_GLYPHS).map(([id, { icon, color }]) => [id, pin(icon, color)]),
) as Record<EventCategoryId, L.DivIcon>;

const DEFAULT_ICON = pin(MapPin, '--ink-dim');

export function iconFor(categories: readonly EventCategoryId[]): L.DivIcon {
  const first = categories[0];
  return first ? (CATEGORY_ICONS[first] ?? DEFAULT_ICON) : DEFAULT_ICON;
}