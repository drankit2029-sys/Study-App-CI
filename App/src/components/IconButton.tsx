import type { LucideIcon } from 'lucide-react-native';
import { Pressable, StyleSheet } from 'react-native';
import type { StyleProp, ViewStyle } from 'react-native';

import { colors, radii, shadows, spacing } from '../theme';

type IconButtonProps = {
  icon: LucideIcon;
  onPress: () => void;
  accessibilityLabel: string;
  disabled?: boolean;
  style?: StyleProp<ViewStyle>;
};

export function IconButton({
  icon: Icon,
  onPress,
  accessibilityLabel,
  disabled = false,
  style,
}: IconButtonProps) {
  const iconColor = disabled ? colors.disabledText : colors.primary;

  return (
    <Pressable
      accessibilityLabel={accessibilityLabel}
      accessibilityRole="button"
      accessibilityState={{ disabled }}
      disabled={disabled}
      hitSlop={4}
      onPress={onPress}
      style={({ pressed }) => [
        styles.base,
        disabled
          ? styles.disabled
          : pressed
            ? styles.pressed
            : styles.raised,
        style,
      ]}
    >
      <Icon color={iconColor} size={20} strokeWidth={2} />
    </Pressable>
  );
}

const styles = StyleSheet.create({
  base: {
    alignItems: 'center',
    borderRadius: radii.pill,
    height: 54,
    justifyContent: 'center',
    width: 54,
  },
  raised: {
    backgroundColor: colors.raised,
    boxShadow: shadows.raisedSoft,
  },
  pressed: {
    backgroundColor: colors.pressed,
    boxShadow: shadows.pressed,
  },
  disabled: {
    backgroundColor: colors.disabledFill,
    boxShadow: undefined,
  },
});
