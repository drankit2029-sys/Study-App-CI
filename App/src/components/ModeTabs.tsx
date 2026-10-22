import { Pressable, StyleSheet, Text, View } from 'react-native';

import type { Phase } from '../pomodoro';
import { PHASE_LABELS } from '../pomodoro';
import { colors, radii, shadows, spacing } from '../theme';

type ModeTabsProps = {
  activePhase: Phase;
  onSelect: (phase: Phase) => void;
  disabled?: boolean;
};

const MODES: Phase[] = ['focus', 'short', 'long'];
const SHORT_LABELS: Record<Phase, string> = {
  focus: 'Focus',
  short: 'Short',
  long: 'Long',
};

export function ModeTabs({ activePhase, onSelect, disabled = false }: ModeTabsProps) {
  return (
    <View style={styles.container}>
      {MODES.map((phase) => {
        const active = phase === activePhase;

        return (
          <Pressable
            accessibilityLabel={`${PHASE_LABELS[phase]} timer`}
            accessibilityRole="tab"
            accessibilityState={{ disabled, selected: active }}
            disabled={disabled}
            key={phase}
            onPress={() => onSelect(phase)}
            style={({ pressed }) => [
              styles.tab,
              active ? styles.activeTab : pressed ? styles.pressedTab : null,
              disabled ? styles.disabledTab : null,
            ]}
          >
            <Text style={[styles.tabLabel, active ? styles.activeLabel : null]}>
              {SHORT_LABELS[phase]}
            </Text>
            {active ? <View style={styles.indicator} /> : null}
          </Pressable>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    backgroundColor: colors.raised,
    borderRadius: radii.pill,
    boxShadow: shadows.raisedSoft,
    flexDirection: 'row',
    gap: 2,
    padding: 4,
  },
  tab: {
    alignItems: 'center',
    borderRadius: radii.pill,
    flex: 1,
    justifyContent: 'center',
    minHeight: 40,
    paddingHorizontal: spacing.tight,
  },
  activeTab: {
    backgroundColor: colors.inset,
  },
  pressedTab: {
    backgroundColor: colors.pressed,
  },
  disabledTab: {
    opacity: 0.6,
  },
  tabLabel: {
    color: colors.textSecondary,
    fontSize: 13,
    fontWeight: '600',
    includeFontPadding: false,
    letterSpacing: 0.2,
  },
  activeLabel: {
    color: colors.primary,
    fontWeight: '700',
  },
  indicator: {
    backgroundColor: colors.primary,
    borderRadius: 1,
    bottom: 4,
    height: 2,
    position: 'absolute',
    width: 24,
  },
});
