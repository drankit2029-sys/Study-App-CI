import type { LucideIcon } from 'lucide-react-native';
import { Pressable, StyleSheet, Text } from 'react-native';
import type { StyleProp, ViewStyle } from 'react-native';

import { colors, radii, shadows, spacing } from '../theme';

type SoftButtonProps = {
  label: string;
  onPress: () => void;
  icon?: LucideIcon;
  variant?: 'primary' | 'secondary';
  disabled?: boolean;
  accessibilityLabel?: string;
  style?: StyleProp<ViewStyle>;
};

export function SoftButton({
  label,
  onPress,
  icon: Icon,
  variant = 'primary',
  disabled = false,
  accessibilityLabel,
  style,
}: SoftButtonProps) {
  const isPrimary = variant === 'primary';
  const contentColor = disabled
    ? colors.disabledText
    : isPrimary
      ? colors.textOnPrimary
      : colors.primary;

  return (
    <Pressable
      accessibilityLabel={accessibilityLabel ?? label}
      accessibilityRole="button"
      accessibilityState={{ disabled }}
      disabled={disabled}
      onPress={onPress}
      style={({ pressed }) => [
        styles.base,
        disabled
          ? styles.disabled
          : pressed
            ? isPrimary
              ? styles.primaryPressed
              : styles.secondaryPressed
            : isPrimary
              ? styles.primaryRaised
              : styles.secondaryRaised,
        style,
      ]}
    >
      {Icon ? <Icon color={contentColor} size={20} strokeWidth={2} /> : null}
      <Text numberOfLines={1} style={[styles.label, { color: contentColor }]}>
        {label}
      </Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  base: {
    alignItems: 'center',
    borderRadius: radii.pill,
    flexDirection: 'row',
    gap: spacing.tight,
    justifyContent: 'center',
    minHeight: 54,
    paddingHorizontal: 20,
  },
  label: {
    fontSize: 15,
    fontWeight: '700',
    includeFontPadding: false,
    letterSpacing: 0.2,
  },
  primaryRaised: {
    backgroundColor: colors.primary,
    boxShadow: shadows.raised,
  },
  primaryPressed: {
    backgroundColor: colors.primaryPressed,
    boxShadow: shadows.tealPressed,
  },
  secondaryRaised: {
    backgroundColor: colors.raised,
    boxShadow: shadows.raisedSoft,
  },
  secondaryPressed: {
    backgroundColor: colors.pressed,
    boxShadow: shadows.pressed,
  },
  disabled: {
    backgroundColor: colors.disabledFill,
    boxShadow: undefined,
  },
});
