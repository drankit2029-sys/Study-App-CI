export type Phase = 'focus' | 'short' | 'long';

export type SessionSettings = {
  focus: number;
  short: number;
  long: number;
};

export type CompletionInfo = {
  finished: Phase;
  next: Phase;
  completedFocus: number;
};

export const SESSIONS_PER_CYCLE = 4;

export const DEFAULT_SETTINGS: SessionSettings = {
  focus: 25,
  short: 5,
  long: 15,
};

export const PHASE_LABELS: Record<Phase, string> = {
  focus: 'Focus',
  short: 'Short break',
  long: 'Long break',
};

export const PHASE_HEADINGS: Record<Phase, string> = {
  focus: 'Focus session',
  short: 'Short break',
  long: 'Long break',
};

export const SETTING_BOUNDS: Record<
  keyof SessionSettings,
  { min: number; max: number; step: number }
> = {
  focus: { min: 5, max: 60, step: 5 },
  short: { min: 5, max: 30, step: 5 },
  long: { min: 10, max: 60, step: 5 },
};

export function secondsFor(phase: Phase, settings: SessionSettings): number {
  return settings[phase] * 60;
}

export function formatClock(totalSeconds: number): string {
  const safeSeconds = Math.max(0, Math.floor(totalSeconds));
  const minutes = Math.floor(safeSeconds / 60);
  const seconds = safeSeconds % 60;

  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
}
