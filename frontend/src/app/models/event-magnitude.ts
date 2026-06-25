export interface EventMagnitude {
  value: number | null;
  unit: string | null;
}

export function formatMagnitude(magnitude: EventMagnitude): string {
  if (magnitude.value == null) {
    return 'Unknown';
  }

  const value = magnitude.value.toLocaleString();
  return magnitude.unit ? `${value} ${magnitude.unit}` : value;
}
