import { useCallback, useEffect, useRef, useState } from 'react';
import { AppState } from 'react-native';
import * as Haptics from 'expo-haptics';
import { activateKeepAwakeAsync, deactivateKeepAwake } from 'expo-keep-awake';

import type { CompletionInfo, Phase, SessionSettings } from '../pomodoro';
import {
  DEFAULT_SETTINGS,
  SESSIONS_PER_CYCLE,
  SETTING_BOUNDS,
  secondsFor,
} from '../pomodoro';

const KEEP_AWAKE_TAG = 'pomodoro-timer';

function selectionFeedback() {
  void Haptics.selectionAsync().catch(() => undefined);
}

export function usePomodoro() {
  const [settings, setSettings] = useState<SessionSettings>(DEFAULT_SETTINGS);
  const [phase, setPhase] = useState<Phase>('focus');
  const [remaining, setRemaining] = useState(secondsFor('focus', DEFAULT_SETTINGS));
  const [isRunning, setIsRunning] = useState(false);
  const [completedFocus, setCompletedFocus] = useState(0);
  const [focusedMinutes, setFocusedMinutes] = useState(0);
  const [completion, setCompletion] = useState<CompletionInfo | null>(null);
  const endAtRef = useRef<number | null>(null);

  const totalSeconds = secondsFor(phase, settings);
  const progress = totalSeconds === 0 ? 0 : 1 - remaining / totalSeconds;
  const canReset = isRunning || remaining < totalSeconds;

  const start = useCallback(() => {
    if (remaining <= 0) {
      return;
    }

    endAtRef.current = Date.now() + remaining * 1000;
    setIsRunning(true);
    void Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light).catch(() => undefined);
  }, [remaining]);

  const finishPhase = useCallback(() => {
    const finished = phase;
    const nextCompletedFocus =
      finished === 'focus' ? completedFocus + 1 : completedFocus;
    const nextFocusedMinutes =
      finished === 'focus' ? focusedMinutes + settings.focus : focusedMinutes;
    const nextPhase: Phase =
      finished === 'focus'
        ? nextCompletedFocus % SESSIONS_PER_CYCLE === 0
          ? 'long'
          : 'short'
        : 'focus';

    endAtRef.current = null;
    setIsRunning(false);
    setCompletedFocus(nextCompletedFocus);
    setFocusedMinutes(nextFocusedMinutes);
    setPhase(nextPhase);
    setRemaining(secondsFor(nextPhase, settings));
    setCompletion({
      finished,
      next: nextPhase,
      completedFocus: nextCompletedFocus,
    });
    void Haptics.notificationAsync(
      Haptics.NotificationFeedbackType.Success,
    ).catch(() => undefined);
  }, [phase, completedFocus, focusedMinutes, settings]);

  const pause = useCallback(() => {
    const endAt = endAtRef.current;

    if (endAt != null) {
      const nextRemaining = Math.max(
        0,
        Math.ceil((endAt - Date.now()) / 1000),
      );

      endAtRef.current = null;

      if (nextRemaining <= 0) {
        finishPhase();
        return;
      }

      setRemaining(nextRemaining);
    }

    setIsRunning(false);
    selectionFeedback();
  }, [finishPhase]);

  const toggle = useCallback(() => {
    if (isRunning) {
      pause();
    } else {
      start();
    }
  }, [isRunning, pause, start]);

  const reset = useCallback(() => {
    endAtRef.current = null;
    setIsRunning(false);
    setRemaining(secondsFor(phase, settings));
    void Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium).catch(() => undefined);
  }, [phase, settings]);

  const selectPhase = useCallback(
    (nextPhase: Phase) => {
      if (nextPhase === phase || isRunning) {
        return;
      }

      endAtRef.current = null;
      setCompletion(null);
      setPhase(nextPhase);
      setRemaining(secondsFor(nextPhase, settings));
      selectionFeedback();
    },
    [phase, isRunning, settings],
  );

  const updateSetting = useCallback(
    (key: keyof SessionSettings, value: number) => {
      const bounds = SETTING_BOUNDS[key];
      const nextValue = Math.min(bounds.max, Math.max(bounds.min, value));

      if (nextValue === settings[key]) {
        return;
      }

      const nextSettings = { ...settings, [key]: nextValue };
      setSettings(nextSettings);

      if (
        key === phase &&
        !isRunning &&
        remaining === secondsFor(phase, settings)
      ) {
        setRemaining(nextValue * 60);
      }

      selectionFeedback();
    },
    [settings, phase, isRunning, remaining],
  );

  const dismissCompletion = useCallback(() => {
    setCompletion(null);
  }, []);

  const startNext = useCallback(() => {
    setCompletion(null);
    start();
  }, [start]);

  useEffect(() => {
    if (!isRunning) {
      return;
    }

    void activateKeepAwakeAsync(KEEP_AWAKE_TAG).catch(() => undefined);

    return () => {
      void deactivateKeepAwake(KEEP_AWAKE_TAG).catch(() => undefined);
    };
  }, [isRunning]);

  useEffect(() => {
    if (!isRunning) {
      return;
    }

    const tick = () => {
      const endAt = endAtRef.current;

      if (endAt == null) {
        return;
      }

      const nextRemaining = Math.max(
        0,
        Math.ceil((endAt - Date.now()) / 1000),
      );

      setRemaining((current) => (current === nextRemaining ? current : nextRemaining));

      if (nextRemaining <= 0) {
        endAtRef.current = null;
        finishPhase();
      }
    };

    tick();
    const intervalId = setInterval(tick, 250);

    return () => clearInterval(intervalId);
  }, [isRunning, finishPhase]);

  useEffect(() => {
    const subscription = AppState.addEventListener('change', (nextState) => {
      if (nextState !== 'active' || !isRunning) {
        return;
      }

      const endAt = endAtRef.current;

      if (endAt == null) {
        return;
      }

      const nextRemaining = Math.max(
        0,
        Math.ceil((endAt - Date.now()) / 1000),
      );

      setRemaining((current) => (current === nextRemaining ? current : nextRemaining));

      if (nextRemaining <= 0) {
        endAtRef.current = null;
        finishPhase();
      }
    });

    return () => subscription.remove();
  }, [isRunning, finishPhase]);

  return {
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
  };
}
