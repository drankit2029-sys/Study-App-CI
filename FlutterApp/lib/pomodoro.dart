import 'dart:async';
import 'dart:math' as math;

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:wakelock_plus/wakelock_plus.dart';

const sessionsPerCycle = 4;

enum Phase { focus, shortBreak, longBreak }

extension PhaseInfo on Phase {
  String get shortLabel => switch (this) {
        Phase.focus => 'Focus',
        Phase.shortBreak => 'Short break',
        Phase.longBreak => 'Long break',
      };

  String get heading => switch (this) {
        Phase.focus => 'Focus',
        Phase.shortBreak => 'Short break',
        Phase.longBreak => 'Long break',
      };

  String get actionLabel => switch (this) {
        Phase.focus => 'Begin focus',
        Phase.shortBreak => 'Begin short break',
        Phase.longBreak => 'Begin long break',
      };
}

class SessionSettings {
  final int focusMinutes;
  final int shortBreakMinutes;
  final int longBreakMinutes;

  const SessionSettings({
    this.focusMinutes = 25,
    this.shortBreakMinutes = 5,
    this.longBreakMinutes = 15,
  });

  int minutesFor(Phase phase) => switch (phase) {
        Phase.focus => focusMinutes,
        Phase.shortBreak => shortBreakMinutes,
        Phase.longBreak => longBreakMinutes,
      };

  SessionSettings copyWith({
    int? focusMinutes,
    int? shortBreakMinutes,
    int? longBreakMinutes,
  }) {
    return SessionSettings(
      focusMinutes: focusMinutes ?? this.focusMinutes,
      shortBreakMinutes: shortBreakMinutes ?? this.shortBreakMinutes,
      longBreakMinutes: longBreakMinutes ?? this.longBreakMinutes,
    );
  }
}

class SettingBounds {
  final int min;
  final int max;
  final int step;

  const SettingBounds({
    required this.min,
    required this.max,
    required this.step,
  });
}

const defaultSettings = SessionSettings();

const settingBounds = <Phase, SettingBounds>{
  Phase.focus: SettingBounds(min: 5, max: 60, step: 5),
  Phase.shortBreak: SettingBounds(min: 5, max: 30, step: 5),
  Phase.longBreak: SettingBounds(min: 10, max: 45, step: 5),
};

int normalizeSetting(int value, SettingBounds bounds) {
  final stepped = (value / bounds.step).round() * bounds.step;
  return math.min(bounds.max, math.max(bounds.min, stepped));
}

int secondsFor(Phase phase, SessionSettings settings) {
  return settings.minutesFor(phase) * 60;
}

String formatClock(int totalSeconds) {
  final safe = math.max(0, totalSeconds);
  final minutes = safe ~/ 60;
  final seconds = safe % 60;
  return '${minutes.toString().padLeft(2, '0')}:${seconds.toString().padLeft(2, '0')}';
}

class CompletionInfo {
  final Phase finished;
  final bool wasFocus;
  final int minutes;
  final Phase nextPhase;

  const CompletionInfo({
    required this.finished,
    required this.wasFocus,
    required this.minutes,
    required this.nextPhase,
  });

  String get eyebrow => wasFocus ? 'FOCUS COMPLETE' : 'BREAK COMPLETE';

  String get title => wasFocus ? 'A gentle pause.' : 'Ready when you are.';

  String get message {
    final unit = minutes == 1 ? 'minute' : 'minutes';
    if (wasFocus) {
      return 'Your focus session is tucked away. Step away for $minutes $unit, stretch, and let your eyes rest.';
    }
    return 'You gave yourself $minutes $unit of quiet. Return to the rhythm whenever you feel settled.';
  }

  String get primaryLabel => nextPhase.actionLabel;
}

class PomodoroController extends ChangeNotifier {
  SessionSettings settings = defaultSettings;
  Phase phase = Phase.focus;
  int remaining = secondsFor(Phase.focus, defaultSettings);
  bool isRunning = false;
  int completedFocus = 0;
  int focusedMinutes = 0;
  CompletionInfo? completion;

  DateTime? _deadline;
  Timer? _ticker;

  int get totalSeconds => secondsFor(phase, settings);

  double get progress {
    final total = totalSeconds;
    if (total <= 0) return 0;
    return ((total - remaining) / total).clamp(0.0, 1.0).toDouble();
  }

  int get sessionNumber => (completedFocus % sessionsPerCycle) + 1;

  int get filledDots {
    if (phase == Phase.longBreak &&
        completedFocus > 0 &&
        completedFocus % sessionsPerCycle == 0) {
      return sessionsPerCycle;
    }
    return completedFocus % sessionsPerCycle;
  }

  int get goalCount => filledDots;

  double get goalProgress => goalCount / sessionsPerCycle;

  String get goalCaption {
    if (goalCount >= sessionsPerCycle) {
      return 'A full rhythm. Carry the calm forward.';
    }
    return '${sessionsPerCycle - goalCount} more sessions to complete the rhythm.';
  }

  String get sessionLabel {
    if (phase == Phase.focus) {
      return 'SESSION $sessionNumber OF $sessionsPerCycle';
    }
    return '${phase.heading.toUpperCase()} · ${settings.minutesFor(phase)} MIN';
  }

  String get statusText {
    if (isRunning) {
      return '${phase.heading} · ${formatClock(remaining)} remaining';
    }
    return switch (phase) {
      Phase.focus => 'Ready when you are. One session at a time.',
      Phase.shortBreak => 'A short breath before the next session.',
      Phase.longBreak => 'A longer pause. You have earned it.',
    };
  }

  String get actionLabel {
    if (isRunning) return 'Pause';
    final partial = remaining < totalSeconds;
    if (phase == Phase.focus) {
      return partial ? 'Resume focus' : 'Start focus';
    }
    return partial ? 'Resume break' : 'Start break';
  }

  bool get canReset => isRunning || remaining < totalSeconds || completion != null;

  bool get isModalOpen => completion != null;

  void toggleTimer() {
    if (isRunning) {
      _pause();
    } else {
      _start();
    }
  }

  void reset() {
    _deadline = null;
    _ticker?.cancel();
    _ticker = null;
    isRunning = false;
    remaining = totalSeconds;
    completion = null;
    notifyListeners();
    unawaited(_setKeepAwake(false));
    unawaited(HapticFeedback.mediumImpact());
  }

  void selectPhase(Phase next) {
    if (next == phase && !isRunning && completion == null) return;
    _ticker?.cancel();
    _ticker = null;
    _deadline = null;
    isRunning = false;
    phase = next;
    remaining = secondsFor(next, settings);
    completion = null;
    notifyListeners();
    unawaited(HapticFeedback.selectionClick());
  }

  void adjustSetting(Phase target, int delta) {
    final bounds = settingBounds[target]!;
    final current = settings.minutesFor(target);
    final nextValue = normalizeSetting(current + delta, bounds);
    if (nextValue == current) {
      unawaited(HapticFeedback.selectionClick());
      return;
    }

    final oldTotal = current * 60;
    settings = switch (target) {
      Phase.focus => settings.copyWith(focusMinutes: nextValue),
      Phase.shortBreak => settings.copyWith(shortBreakMinutes: nextValue),
      Phase.longBreak => settings.copyWith(longBreakMinutes: nextValue),
    };

    if (target == phase && !isRunning && remaining == oldTotal) {
      remaining = nextValue * 60;
    }
    notifyListeners();
    unawaited(HapticFeedback.selectionClick());
  }

  void dismissCompletion() {
    if (completion == null) return;
    completion = null;
    notifyListeners();
  }

  void startNext() {
    if (completion == null) return;
    completion = null;
    _start();
  }

  void handleAppResumed() {
    if (isRunning && _deadline != null) {
      _tick();
    }
  }

  void _start() {
    if (remaining <= 0) return;
    _deadline = DateTime.now().add(Duration(seconds: remaining));
    isRunning = true;
    notifyListeners();
    unawaited(_setKeepAwake(true));
    unawaited(HapticFeedback.lightImpact());
    _startTicker();
  }

  void _pause() {
    final deadline = _deadline;
    _ticker?.cancel();
    _ticker = null;
    _deadline = null;

    if (deadline != null) {
      final next = _remainingSeconds(deadline);
      if (next <= 0) {
        _finishPhase();
        return;
      }
      remaining = next;
    }

    isRunning = false;
    notifyListeners();
    unawaited(_setKeepAwake(false));
    unawaited(HapticFeedback.selectionClick());
  }

  void _startTicker() {
    _ticker?.cancel();
    _ticker = Timer.periodic(const Duration(milliseconds: 250), (_) => _tick());
  }

  void _tick() {
    final deadline = _deadline;
    if (deadline == null) {
      _ticker?.cancel();
      _ticker = null;
      return;
    }

    final next = _remainingSeconds(deadline);
    if (next != remaining) {
      remaining = next;
      if (next > 0) {
        notifyListeners();
      }
    }
    if (next <= 0) {
      _finishPhase();
    }
  }

  void _finishPhase() {
    _deadline = null;
    _ticker?.cancel();
    _ticker = null;
    isRunning = false;

    final finished = phase;
    final wasFocus = finished == Phase.focus;
    final finishedMinutes = settings.minutesFor(finished);

    if (wasFocus) {
      completedFocus += 1;
      focusedMinutes += finishedMinutes;
    }

    final Phase nextPhase;
    if (wasFocus) {
      nextPhase = completedFocus % sessionsPerCycle == 0
          ? Phase.longBreak
          : Phase.shortBreak;
    } else {
      nextPhase = Phase.focus;
    }

    completion = CompletionInfo(
      finished: finished,
      wasFocus: wasFocus,
      minutes: finishedMinutes,
      nextPhase: nextPhase,
    );
    phase = nextPhase;
    remaining = secondsFor(nextPhase, settings);
    notifyListeners();
    unawaited(_setKeepAwake(false));
    unawaited(HapticFeedback.heavyImpact());
  }

  int _remainingSeconds(DateTime deadline) {
    final millis = deadline.difference(DateTime.now()).inMilliseconds;
    if (millis <= 0) return 0;
    return (millis / 1000).ceil();
  }

  Future<void> _setKeepAwake(bool enabled) async {
    try {
      if (enabled) {
        await WakelockPlus.enable();
      } else {
        await WakelockPlus.disable();
      }
    } catch (_) {
      // Wakelock support is best-effort and should never block the timer.
    }
  }

  @override
  void dispose() {
    _ticker?.cancel();
    _ticker = null;
    unawaited(_setKeepAwake(false));
    super.dispose();
  }
}
