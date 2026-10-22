import {
  CircleCheck,
  Clock,
  Pause,
  Play,
  RotateCcw,
  SlidersHorizontal,
  Sparkles,
  Target,
  Timer,
} from 'lucide-react-native';
import { ScrollView, StyleSheet, Text, useWindowDimensions, View } from 'react-native';

import { ConfirmModal } from './components/ConfirmModal';
import { IconButton } from './components/IconButton';
import { ModeTabs } from './components/ModeTabs';
import { ProgressRing } from './components/ProgressRing';
import { SoftButton } from './components/SoftButton';
import { StatCard } from './components/StatCard';
import { Stepper } from './components/Stepper';
import { usePomodoro } from './hooks/usePomodoro';
import {
  formatClock,
  PHASE_HEADINGS,
  SESSIONS_PER_CYCLE,
  SETTING_BOUNDS,
} from './pomodoro';
import { colors, radii, shadows, spacing } from './theme';

const SETTING_ROWS = [
  { key: 'focus', label: 'Focus' },
  { key: 'short', label: 'Short break' },
  { key: 'long', label: 'Long break' },
] as const;

export function PomodoroScreen() {
  const {
    canReset,
    completedFocus,
    completion,
    dismissCompletion,
    focusedMinutes,
    isRunning,
    phase,
    progress,
    remaining,
    reset,
    selectPhase,
    settings,
    startNext,
    toggle,
    totalSeconds,
    updateSetting,
  } = usePomodoro();
  const { width } = useWindowDimensions();

  const timerCardInnerWidth = Math.max(0, width - spacing.relaxed * 2 - spacing.medium * 2);
  const ringWellSize = Math.min(280, timerCardInnerWidth);
  const ringSize = ringWellSize - 28;
  const timerFontSize = ringSize < 230 ? 44 : 52;

  const sessionNumber = (completedFocus % SESSIONS_PER_CYCLE) + 1;
  const sessionLabel =
    phase === 'focus'
      ? `Session ${sessionNumber} of ${SESSIONS_PER_CYCLE}`
      : phase === 'long'
        ? 'Long recovery'
        : 'Short recovery';
  const filledDots =
    phase === 'long' && completedFocus > 0 && completedFocus % SESSIONS_PER_CYCLE === 0
      ? SESSIONS_PER_CYCLE
      : completedFocus % SESSIONS_PER_CYCLE;

  const actionLabel = isRunning
    ? 'Pause'
    : remaining < totalSeconds
      ? 'Resume'
      : phase === 'focus'
        ? 'Start focus'
        : 'Start break';

  const statusText = isRunning
    ? 'Timer running - the screen will stay awake.'
    : remaining < totalSeconds
      ? 'Paused. Reset if you want a fresh start.'
      : 'Start a calm focus block, then take a real break.';

  const statusDotStyle = isRunning
    ? styles.statusDotActive
    : remaining < totalSeconds
      ? styles.statusDotPaused
      : styles.statusDotIdle;

  const completionTitle = completion
    ? completion.finished === 'focus'
      ? completion.next === 'long'
        ? 'Cycle complete'
        : 'Focus complete'
      : 'Break complete'
    : '';

  const completionMessage = completion
    ? completion.finished === 'focus'
      ? completion.next === 'long'
        ? `Four focus sessions complete. A ${settings.long}-minute long break is ready - step away and recharge.`
        : `Session ${completion.completedFocus} done. A ${settings.short}-minute short break is ready.`
      : `Break finished. Your next ${settings.focus}-minute focus session is ready when you are.`
    : '';

  const completionConfirmLabel =
    completion?.next === 'focus' ? 'Start focus' : 'Start break';

  return (
    <View style={styles.screen}>
      <ScrollView
        contentContainerStyle={styles.content}
        showsVerticalScrollIndicator={false}
      >
        <View style={styles.header}>
          <View style={styles.headerCopy}>
            <View style={styles.eyebrow}>
              <Sparkles color={colors.primary} size={14} strokeWidth={2} />
              <Text style={styles.eyebrowText}>STUDY RHYTHM</Text>
            </View>
            <Text style={styles.title}>Pomodoro</Text>
            <Text style={styles.subtitle}>A softer way to protect your focus.</Text>
          </View>
          <View style={styles.headerIcon}>
            <Timer color={colors.primary} size={24} strokeWidth={2} />
          </View>
        </View>

        <View style={styles.timerCard}>
          <ModeTabs activePhase={phase} disabled={isRunning} onSelect={selectPhase} />

          <View style={styles.ringArea}>
            <View
              style={[
                styles.ringWell,
                { borderRadius: ringWellSize / 2, height: ringWellSize, width: ringWellSize },
              ]}
            >
              <ProgressRing progress={progress} size={ringSize}>
                <Text style={styles.phaseLabel}>{PHASE_HEADINGS[phase].toUpperCase()}</Text>
                <Text
                  accessibilityLabel={`${formatClock(remaining)} remaining`}
                  style={[
                    styles.clock,
                    { fontSize: timerFontSize, lineHeight: timerFontSize + 6 },
                  ]}
                >
                  {formatClock(remaining)}
                </Text>
                <Text style={styles.sessionLabel}>{sessionLabel}</Text>
                <View
                  accessibilityLabel={`${filledDots} of ${SESSIONS_PER_CYCLE} sessions complete`}
                  style={styles.dots}
                >
                  {Array.from({ length: SESSIONS_PER_CYCLE }, (_, index) => {
                    const isFilled = index < filledDots;
                    const isCurrent = phase === 'focus' && index === filledDots;

                    return (
                      <View
                        key={index}
                        style={[
                          styles.dot,
                          isFilled ? styles.dotFilled : null,
                          isCurrent ? styles.dotCurrent : null,
                        ]}
                      />
                    );
                  })}
                </View>
              </ProgressRing>
            </View>
          </View>

          <View style={styles.controls}>
            <SoftButton
              accessibilityLabel={isRunning ? 'Pause timer' : `${actionLabel} timer`}
              icon={isRunning ? Pause : Play}
              label={actionLabel}
              onPress={toggle}
              style={styles.primaryAction}
            />
            <IconButton
              accessibilityLabel="Reset timer"
              disabled={!canReset}
              icon={RotateCcw}
              onPress={reset}
            />
          </View>

          <View style={styles.statusRow}>
            <View style={[styles.statusDot, statusDotStyle]} />
            <Text style={styles.statusText}>{statusText}</Text>
          </View>
        </View>

        <View style={styles.statsRow}>
          <StatCard
            icon={CircleCheck}
            label="SESSIONS"
            value={String(completedFocus)}
          />
          <StatCard icon={Clock} label="MINUTES" value={String(focusedMinutes)} />
          <StatCard
            icon={Target}
            label="GOAL"
            value={`${Math.min(completedFocus, SESSIONS_PER_CYCLE)}/${SESSIONS_PER_CYCLE}`}
          />
        </View>

        <View style={styles.settingsCard}>
          <View style={styles.settingsHeader}>
            <View style={styles.settingsTitleRow}>
              <View style={styles.settingsIcon}>
                <SlidersHorizontal color={colors.primary} size={18} strokeWidth={2} />
              </View>
              <Text style={styles.settingsTitle}>Session lengths</Text>
            </View>
            <View style={styles.badge}>
              <Text style={styles.badgeText}>MINUTES</Text>
            </View>
          </View>

          {SETTING_ROWS.map((row, index) => (
            <View key={row.key}>
              {index > 0 ? <View style={styles.divider} /> : null}
              <Stepper
                disabled={isRunning}
                label={row.label}
                max={SETTING_BOUNDS[row.key].max}
                min={SETTING_BOUNDS[row.key].min}
                onChange={(value) => updateSetting(row.key, value)}
                step={SETTING_BOUNDS[row.key].step}
                value={settings[row.key]}
              />
            </View>
          ))}

          {isRunning ? (
            <Text style={styles.settingsHint}>
              Pause the timer to adjust session lengths.
            </Text>
          ) : null}
        </View>
      </ScrollView>

      <ConfirmModal
        cancelLabel="Later"
        confirmLabel={completionConfirmLabel}
        icon={CircleCheck}
        message={completionMessage}
        onCancel={dismissCompletion}
        onConfirm={startNext}
        title={completionTitle}
        visible={completion !== null}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {
    backgroundColor: colors.base,
    flex: 1,
  },
  content: {
    paddingBottom: 40,
    paddingHorizontal: spacing.relaxed,
    paddingTop: spacing.medium,
  },
  header: {
    alignItems: 'center',
    flexDirection: 'row',
    marginBottom: spacing.relaxed,
  },
  headerCopy: {
    flex: 1,
  },
  eyebrow: {
    alignItems: 'center',
    alignSelf: 'flex-start',
    backgroundColor: colors.inset,
    borderRadius: radii.pill,
    boxShadow: shadows.inset,
    flexDirection: 'row',
    gap: 6,
    height: 28,
    paddingHorizontal: 10,
  },
  eyebrowText: {
    color: colors.primary,
    fontSize: 9,
    fontWeight: '700',
    includeFontPadding: false,
    letterSpacing: 1.4,
  },
  title: {
    color: colors.text,
    fontSize: 32,
    fontWeight: '700',
    includeFontPadding: false,
    letterSpacing: -0.6,
    lineHeight: 38,
    marginTop: 12,
  },
  subtitle: {
    color: colors.textSecondary,
    fontSize: 13,
    includeFontPadding: false,
    lineHeight: 19,
    marginTop: 2,
  },
  headerIcon: {
    alignItems: 'center',
    backgroundColor: colors.raised,
    borderRadius: radii.pill,
    boxShadow: shadows.raisedSoft,
    height: 56,
    justifyContent: 'center',
    marginLeft: spacing.medium,
    width: 56,
  },
  timerCard: {
    backgroundColor: colors.raised,
    borderRadius: radii.xxLarge,
    boxShadow: shadows.raised,
    padding: spacing.medium,
  },
  ringArea: {
    alignItems: 'center',
    marginBottom: spacing.medium,
    marginTop: 20,
  },
  ringWell: {
    alignItems: 'center',
    backgroundColor: colors.inset,
    boxShadow: shadows.inset,
    justifyContent: 'center',
  },
  phaseLabel: {
    color: colors.textSecondary,
    fontSize: 10,
    fontWeight: '700',
    includeFontPadding: false,
    letterSpacing: 1.5,
    textAlign: 'center',
  },
  clock: {
    color: colors.text,
    fontVariant: ['tabular-nums'],
    fontWeight: '700',
    includeFontPadding: false,
    letterSpacing: -1,
    marginTop: 2,
    textAlign: 'center',
  },
  sessionLabel: {
    color: colors.textSecondary,
    fontSize: 11,
    includeFontPadding: false,
    marginTop: 2,
    textAlign: 'center',
  },
  dots: {
    alignItems: 'center',
    flexDirection: 'row',
    gap: 6,
    marginTop: spacing.tight,
  },
  dot: {
    backgroundColor: colors.dotIdle,
    borderRadius: radii.pill,
    height: 8,
    width: 8,
  },
  dotFilled: {
    backgroundColor: colors.primary,
  },
  dotCurrent: {
    backgroundColor: 'transparent',
    borderColor: colors.focusGlow,
    borderWidth: 2,
  },
  controls: {
    flexDirection: 'row',
    gap: 12,
    marginTop: spacing.tight,
  },
  primaryAction: {
    flex: 1,
  },
  statusRow: {
    alignItems: 'center',
    flexDirection: 'row',
    gap: 6,
    justifyContent: 'center',
    marginTop: 12,
    paddingHorizontal: 4,
  },
  statusDot: {
    borderRadius: radii.pill,
    height: 6,
    width: 6,
  },
  statusDotActive: {
    backgroundColor: colors.primary,
  },
  statusDotPaused: {
    backgroundColor: colors.warning,
  },
  statusDotIdle: {
    backgroundColor: colors.disabledFill,
  },
  statusText: {
    color: colors.textSecondary,
    flexShrink: 1,
    fontSize: 11,
    includeFontPadding: false,
    lineHeight: 16,
    textAlign: 'center',
  },
  statsRow: {
    flexDirection: 'row',
    gap: spacing.tight,
    marginTop: spacing.medium,
  },
  settingsCard: {
    backgroundColor: colors.raised,
    borderRadius: radii.xxLarge,
    boxShadow: shadows.raisedSoft,
    marginTop: spacing.medium,
    padding: spacing.medium,
  },
  settingsHeader: {
    alignItems: 'center',
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 4,
  },
  settingsTitleRow: {
    alignItems: 'center',
    flexDirection: 'row',
    gap: spacing.tight,
  },
  settingsIcon: {
    alignItems: 'center',
    backgroundColor: colors.tealTint,
    borderRadius: 10,
    height: 32,
    justifyContent: 'center',
    width: 32,
  },
  settingsTitle: {
    color: colors.text,
    fontSize: 15,
    fontWeight: '700',
    includeFontPadding: false,
  },
  badge: {
    alignItems: 'center',
    backgroundColor: colors.primary,
    borderRadius: radii.pill,
    height: 24,
    justifyContent: 'center',
    paddingHorizontal: 10,
  },
  badgeText: {
    color: colors.textOnPrimary,
    fontSize: 8,
    fontWeight: '700',
    includeFontPadding: false,
    letterSpacing: 1,
  },
  divider: {
    backgroundColor: colors.border,
    height: 1,
  },
  settingsHint: {
    color: colors.textSecondary,
    fontSize: 11,
    includeFontPadding: false,
    marginTop: 4,
  },
});
