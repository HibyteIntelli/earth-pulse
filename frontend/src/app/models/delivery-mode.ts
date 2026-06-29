export type DeliveryMode = 'IMMEDIATE' | 'DAILY_DIGEST';

export const DELIVERY_MODES: readonly DeliveryMode[] = ['IMMEDIATE', 'DAILY_DIGEST'];

const DELIVERY_MODE_LABEL: Readonly<Record<DeliveryMode, string>> = {
  IMMEDIATE: 'Immediate',
  DAILY_DIGEST: 'Daily digest',
};

export function deliveryModeLabel(mode: DeliveryMode): string {
  return DELIVERY_MODE_LABEL[mode] ?? mode;
}