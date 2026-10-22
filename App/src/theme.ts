import type { ViewStyle } from 'react-native';

export const colors = {
  base: '#EFECE6',
  raised: '#F2EFE9',
  inset: '#EAE5DC',
  pressed: '#DFD9CE',
  primary: '#0F5257',
  primaryPressed: '#0A3B3F',
  text: '#1F2421',
  textSecondary: '#6E6A63',
  textOnPrimary: '#FFFFFF',
  disabledFill: '#BDB8B0',
  disabledText: '#ECE9E2',
  error: '#A63A3A',
  warning: '#C89D66',
  border: '#E2DDD5',
  dotIdle: '#D6D0C4',
  tealTint: 'rgba(15, 82, 87, 0.10)',
  warningTint: 'rgba(200, 157, 102, 0.18)',
  overlay: 'rgba(31, 36, 33, 0.35)',
  shadowDark: 'rgba(180, 170, 155, 0.60)',
  shadowLight: 'rgba(255, 255, 255, 0.85)',
  insetShadowDark: 'rgba(170, 160, 145, 0.60)',
  insetShadowLight: 'rgba(255, 255, 255, 0.80)',
  pressedShadowDark: 'rgba(155, 145, 130, 0.70)',
  pressedShadowLight: 'rgba(255, 255, 255, 0.60)',
  focusGlow: 'rgba(15, 82, 87, 0.35)',
} as const;

export const spacing = {
  tight: 8,
  medium: 16,
  relaxed: 24,
} as const;

export const radii = {
  small: 4,
  medium: 8,
  large: 12,
  xLarge: 16,
  modal: 20,
  xxLarge: 24,
  pill: 9999,
} as const;

type Shadow = ViewStyle['boxShadow'];

export const shadows: Record<
  'raised' | 'raisedSoft' | 'inset' | 'pressed' | 'modal' | 'focusGlow' | 'tealPressed',
  Shadow
> = {
  raised: [
    { offsetX: 6, offsetY: 6, blurRadius: 12, color: colors.shadowDark },
    { offsetX: -6, offsetY: -6, blurRadius: 12, color: colors.shadowLight },
  ],
  raisedSoft: [
    { offsetX: 3, offsetY: 3, blurRadius: 6, color: colors.shadowDark },
    { offsetX: -3, offsetY: -3, blurRadius: 6, color: colors.shadowLight },
  ],
  inset: [
    { inset: true, offsetX: 3, offsetY: 3, blurRadius: 6, color: colors.insetShadowDark },
    { inset: true, offsetX: -3, offsetY: -3, blurRadius: 6, color: colors.insetShadowLight },
  ],
  pressed: [
    { inset: true, offsetX: 4, offsetY: 4, blurRadius: 8, color: colors.pressedShadowDark },
    { inset: true, offsetX: -2, offsetY: -2, blurRadius: 5, color: colors.pressedShadowLight },
  ],
  modal: [
    { offsetX: 10, offsetY: 10, blurRadius: 20, color: colors.shadowDark },
    { offsetX: -10, offsetY: -10, blurRadius: 20, color: colors.shadowLight },
  ],
  focusGlow: [
    { offsetX: 0, offsetY: 0, blurRadius: 8, spreadDistance: 1, color: colors.focusGlow },
  ],
  tealPressed: [
    { inset: true, offsetX: 0, offsetY: 4, blurRadius: 8, color: 'rgba(0, 0, 0, 0.28)' },
  ],
};
