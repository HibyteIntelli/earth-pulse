export type DeliveryMode = 'IMMEDIATE' | 'DAILY';

export const DELIVERY_MODES: readonly DeliveryMode[] = ['IMMEDIATE', 'DAILY'];

const DELIVERY_MODE_LABEL: Readonly<Record<DeliveryMode, string>> = {
  IMMEDIATE: 'Immediate',
  DAILY: 'Daily digest',
};

export function deliveryModeLabel(mode: DeliveryMode): string {
  return DELIVERY_MODE_LABEL[mode] ?? mode;
}