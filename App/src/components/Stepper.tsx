import { Minus, Plus } from 'lucide-react-native';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { colors, radii, shadows, spacing } from '../theme';

type StepperProps = {
  label: string;
  value: number;
  min: number;
  max: number;
  step: number;
  onChange: (value: number) => void;
  disabled?: boolean;
};

export function Stepper({
  label,
  value,
  min,
  max,
  step,
  onChange,
  disabled = false,
}: StepperProps) {
  const canDecrease = !disabled && value > min;
  const canIncrease = !disabled && value < max;

  return (
    <View style={styles.row}>
      <View style={styles.copy}>
        <Text style={[styles.label, disabled ? styles.labelDisabled : null]}>{label}</Text>
        <Text style={styles.helper}>
          {min}-{max} min
        </Text>
      </View>

      <View style={styles.controls}>
        <StepButton
          accessibilityLabel={`Decrease ${label} by ${step} minutes`}
          disabled={!canDecrease}
          icon={Minus}
          onPress={() => onChange(value - step)}
        />
        <View
          accessibilityLabel={`${label}: ${value} minutes`}
          accessibilityRole="text"
          style={styles.valueWell}
        >
          <Text style={[styles.value, disabled ? styles.valueDisabled : null]}>{value}</Text>
          <Text style={styles.unit}>min</Text>
        </View>
        <StepButton
          accessibilityLabel={`Increase ${label} by ${step} minutes`}
          disabled={!canIncrease}
          icon={Plus}
          onPress={() => onChange(value + step)}
        />
      </View>
    </View>
  );
}

type StepButtonProps = {
  accessibilityLabel: string;
  disabled: boolean;
  icon: typeof Minus;
  onPress: () => void;
};

function StepButton({
  accessibilityLabel,
  disabled,
  icon: Icon,
  onPress,
}: StepButtonProps) {
  return (
    <Pressable
      accessibilityLabel={accessibilityLabel}
      accessibilityRole="button"
      accessibilityState={{ disabled }}
      disabled={disabled}
      hitSlop={4}
      onPress={onPress}
      style={({ pressed }) => [
        styles.stepButton,
        disabled ? styles.stepButtonDisabled : pressed ? styles.stepButtonPressed : styles.stepButtonRaised,
      ]}
    >
      <Icon color={disabled ? colors.disabledText : colors.primary} size={16} strokeWidth={2} />
    </Pressable>
  );
}

const styles = StyleSheet.create({
  row: {
    alignItems: 'center',
    flexDirection: 'row',
    gap: spacing.medium,
    justifyContent: 'space-between',
    paddingVertical: 12,
  },
  copy: {
    flex: 1,
    gap: 2,
  },
  label: {
    color: colors.text,
    fontSize: 14,
    fontWeight: '700',
    includeFontPadding: false,
  },
  labelDisabled: {
    color: colors.textSecondary,
  },
  helper: {
    color: colors.textSecondary,
    fontSize: 11,
    includeFontPadding: false,
  },
  controls: {
    alignItems: 'center',
    flexDirection: 'row',
    gap: 6,
  },
  stepButton: {
    alignItems: 'center',
    borderRadius: radii.pill,
    height: 36,
    justifyContent: 'center',
    width: 36,
  },
  stepButtonRaised: {
    backgroundColor: colors.raised,
    boxShadow: shadows.raisedSoft,
  },
  stepButtonPressed: {
    backgroundColor: colors.pressed,
    boxShadow: shadows.pressed,
  },
  stepButtonDisabled: {
    backgroundColor: colors.disabledFill,
    boxShadow: undefined,
  },
  valueWell: {
    alignItems: 'center',
    backgroundColor: colors.inset,
    borderRadius: radii.medium,
    boxShadow: shadows.inset,
    flexDirection: 'row',
    gap: 3,
    height: 38,
    justifyContent: 'center',
    width: 60,
  },
  value: {
    color: colors.text,
    fontSize: 15,
    fontVariant: ['tabular-nums'],
    fontWeight: '700',
    includeFontPadding: false,
  },
  valueDisabled: {
    color: colors.textSecondary,
  },
  unit: {
    color: colors.textSecondary,
    fontSize: 9,
    fontWeight: '600',
    includeFontPadding: false,
    letterSpacing: 0.3,
  },
});
