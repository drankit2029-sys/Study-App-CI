import type { LucideIcon } from 'lucide-react-native';
import { Modal, Pressable, StyleSheet, Text, View } from 'react-native';

import { colors, radii, shadows, spacing } from '../theme';
import { SoftButton } from './SoftButton';

type ConfirmModalProps = {
  visible: boolean;
  title: string;
  message: string;
  icon: LucideIcon;
  confirmLabel: string;
  cancelLabel?: string;
  tone?: 'teal' | 'amber';
  onConfirm: () => void;
  onCancel: () => void;
};

export function ConfirmModal({
  visible,
  title,
  message,
  icon: Icon,
  confirmLabel,
  cancelLabel = 'Cancel',
  tone = 'teal',
  onConfirm,
  onCancel,
}: ConfirmModalProps) {
  const accentColor = tone === 'amber' ? colors.warning : colors.primary;

  return (
    <Modal
      animationType="fade"
      onRequestClose={onCancel}
      statusBarTranslucent
      transparent
      visible={visible}
    >
      <View style={styles.backdrop}>
        <Pressable
          accessibilityLabel="Close dialog"
          accessibilityRole="button"
          onPress={onCancel}
          style={StyleSheet.absoluteFill}
        />
        <View accessibilityViewIsModal style={styles.card}>
          <View
            style={[
              styles.iconBadge,
              { backgroundColor: tone === 'amber' ? colors.warningTint : colors.tealTint },
            ]}
          >
            <Icon color={accentColor} size={30} strokeWidth={2} />
          </View>
          <Text style={styles.title}>{title}</Text>
          <Text style={styles.message}>{message}</Text>
          <View style={styles.actions}>
            <SoftButton
              label={cancelLabel}
              onPress={onCancel}
              style={styles.action}
              variant="secondary"
            />
            <SoftButton label={confirmLabel} onPress={onConfirm} style={styles.action} />
          </View>
        </View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  backdrop: {
    alignItems: 'center',
    backgroundColor: colors.overlay,
    flex: 1,
    justifyContent: 'center',
    padding: spacing.relaxed,
  },
  card: {
    alignItems: 'center',
    backgroundColor: colors.raised,
    borderColor: 'rgba(255, 255, 255, 0.60)',
    borderRadius: radii.modal,
    borderWidth: 1,
    boxShadow: shadows.modal,
    maxWidth: 340,
    padding: spacing.relaxed,
    width: '100%',
  },
  iconBadge: {
    alignItems: 'center',
    borderRadius: radii.pill,
    boxShadow: shadows.raisedSoft,
    height: 64,
    justifyContent: 'center',
    width: 64,
  },
  title: {
    color: colors.text,
    fontSize: 20,
    fontWeight: '700',
    includeFontPadding: false,
    marginTop: spacing.medium,
    textAlign: 'center',
  },
  message: {
    color: colors.textSecondary,
    fontSize: 13,
    includeFontPadding: false,
    lineHeight: 20,
    marginTop: spacing.tight,
    textAlign: 'center',
  },
  actions: {
    flexDirection: 'row',
    gap: spacing.tight,
    marginTop: spacing.relaxed,
    width: '100%',
  },
  action: {
    flex: 1,
    paddingHorizontal: 12,
  },
});
