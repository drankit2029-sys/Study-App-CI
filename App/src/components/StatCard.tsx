import type { LucideIcon } from 'lucide-react-native';
import { StyleSheet, Text, View } from 'react-native';

import { colors, radii, shadows, spacing } from '../theme';

type StatCardProps = {
  icon: LucideIcon;
  label: string;
  value: string;
};

export function StatCard({ icon: Icon, label, value }: StatCardProps) {
  return (
    <View accessibilityLabel={`${label}: ${value}`} style={styles.card}>
      <View style={styles.iconBadge}>
        <Icon color={colors.primary} size={16} strokeWidth={2} />
      </View>
      <View style={styles.copy}>
        <Text numberOfLines={1} style={styles.value}>
          {value}
        </Text>
        <Text numberOfLines={1} style={styles.label}>
          {label}
        </Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    backgroundColor: colors.raised,
    borderRadius: radii.xLarge,
    boxShadow: shadows.raisedSoft,
    flex: 1,
    gap: spacing.tight,
    minWidth: 0,
    padding: 12,
  },
  iconBadge: {
    alignItems: 'center',
    backgroundColor: colors.tealTint,
    borderRadius: 10,
    height: 30,
    justifyContent: 'center',
    width: 30,
  },
  copy: {
    gap: 2,
  },
  value: {
    color: colors.text,
    fontSize: 20,
    fontVariant: ['tabular-nums'],
    fontWeight: '700',
    includeFontPadding: false,
    lineHeight: 24,
  },
  label: {
    color: colors.textSecondary,
    fontSize: 9,
    fontWeight: '700',
    includeFontPadding: false,
    letterSpacing: 0.6,
  },
});
