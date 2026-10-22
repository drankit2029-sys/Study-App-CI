import 'dart:math' as math;
import 'dart:ui' show FontFeature;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'pomodoro.dart';
import 'theme.dart';

class PomodoroScreen extends StatefulWidget {
  const PomodoroScreen({super.key});

  @override
  State<PomodoroScreen> createState() => _PomodoroScreenState();
}

class _PomodoroScreenState extends State<PomodoroScreen>
    with WidgetsBindingObserver {
  late final PomodoroController _controller;

  @override
  void initState() {
    super.initState();
    _controller = PomodoroController();
    WidgetsBinding.instance.addObserver(this);
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      _controller.handleAppResumed();
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _controller,
      builder: (context, _) {
        final controller = _controller;
        return PopScope(
          canPop: !controller.isModalOpen,
          onPopInvokedWithResult: (didPop, result) {
            if (!didPop) {
              controller.dismissCompletion();
            }
          },
          child: Scaffold(
            backgroundColor: ivory,
            body: Stack(
              children: [
                Positioned(
                  top: -20,
                  right: -48,
                  child: SizedBox(
                    width: 132,
                    height: 132,
                    child: CustomPaint(painter: _LeafPainter()),
                  ),
                ),
                SafeArea(
                  child: Center(
                    child: ConstrainedBox(
                      constraints: const BoxConstraints(maxWidth: 520),
                      child: SingleChildScrollView(
                        padding: const EdgeInsets.fromLTRB(24, 20, 24, 34),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.stretch,
                          children: [
                            const _Header(),
                            const SizedBox(height: 26),
                            _ModeTabs(controller: controller),
                            const SizedBox(height: 14),
                            _TimerCard(controller: controller),
                            const SizedBox(height: 16),
                            _RhythmCard(controller: controller),
                            const SizedBox(height: 16),
                            _SettingsCard(controller: controller),
                            const SizedBox(height: 26),
                            const _Footer(),
                          ],
                        ),
                      ),
                    ),
                  ),
                ),
                if (controller.isModalOpen)
                  Positioned.fill(
                    child: _CompletionOverlay(controller: controller),
                  ),
              ],
            ),
          ),
        );
      },
    );
  }
}

class _Header extends StatelessWidget {
  const _Header();

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'STUDY / RHYTHM',
          style: serifStyle(
            fontSize: 17,
            fontWeight: FontWeight.w300,
            color: forest,
            letterSpacing: 4.6,
            height: 1.2,
          ),
        ),
        const SizedBox(height: 8),
        Text(
          'Pomodoro',
          style: serifStyle(
            fontSize: 46,
            fontWeight: FontWeight.w400,
            height: 1.06,
          ),
        ),
        const SizedBox(height: 6),
        Text(
          'Settle in. Do one thing at a time.',
          style: sansStyle(fontSize: 13, color: muted, height: 1.4),
        ),
        const SizedBox(height: 14),
        Container(width: 40, height: 1, color: stone),
      ],
    );
  }
}

class _ModeTabs extends StatelessWidget {
  final PomodoroController controller;

  const _ModeTabs({required this.controller});

  @override
  Widget build(BuildContext context) {
    return Opacity(
      opacity: controller.isRunning ? 0.55 : 1,
      child: Container(
        padding: const EdgeInsets.all(3),
        decoration: softCardDecoration(color: parchment, radius: 22),
        child: Row(
          children: [
            for (final phase in Phase.values)
              Expanded(
                child: _ModeTab(
                  phase: phase,
                  active: phase == controller.phase,
                  enabled: !controller.isRunning,
                  onTap: () => controller.selectPhase(phase),
                ),
              ),
          ],
        ),
      ),
    );
  }
}

class _ModeTab extends StatelessWidget {
  final Phase phase;
  final bool active;
  final bool enabled;
  final VoidCallback onTap;

  const _ModeTab({
    required this.phase,
    required this.active,
    required this.enabled,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Semantics(
      button: true,
      selected: active,
      enabled: enabled,
      label: '${phase.shortLabel} timer',
      child: ExcludeSemantics(
        child: GestureDetector(
          behavior: HitTestBehavior.opaque,
          onTap: enabled ? onTap : null,
          child: AnimatedContainer(
            duration: const Duration(milliseconds: 180),
            curve: Curves.easeOutCubic,
            height: 40,
            alignment: Alignment.center,
            decoration: BoxDecoration(
              color: active ? forest : Colors.transparent,
              borderRadius: BorderRadius.circular(19),
              boxShadow: active
                  ? const [
                      BoxShadow(
                        color: Color(0x2B0F5132),
                        blurRadius: 12,
                        offset: Offset(0, 4),
                      ),
                    ]
                  : null,
            ),
            child: Text(
              phase.shortLabel,
              style: serifStyle(
                fontSize: 16,
                fontWeight: FontWeight.w500,
                color: active ? ivory : muted,
                height: 1.1,
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _TimerCard extends StatelessWidget {
  final PomodoroController controller;

  const _TimerCard({required this.controller});

  @override
  Widget build(BuildContext context) {
    final canReset = controller.canReset;
    return Container(
      padding: const EdgeInsets.fromLTRB(16, 18, 16, 16),
      decoration: softCardDecoration(),
      child: Column(
        children: [
          _TimerFace(controller: controller),
          const SizedBox(height: 14),
          _StatusRow(controller: controller),
          const SizedBox(height: 16),
          Row(
            children: [
              Expanded(
                flex: 3,
                child: _SoftButton(
                  label: controller.actionLabel,
                  leadingIcon: controller.isRunning
                      ? Icons.pause_rounded
                      : Icons.play_arrow_rounded,
                  onTap: controller.toggleTimer,
                  variant: _ButtonVariant.primary,
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                flex: 2,
                child: _SoftButton(
                  label: 'Reset',
                  leadingIcon: Icons.refresh_rounded,
                  onTap: canReset ? controller.reset : null,
                  variant: _ButtonVariant.secondary,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _TimerFace extends StatelessWidget {
  final PomodoroController controller;

  const _TimerFace({required this.controller});

  @override
  Widget build(BuildContext context) {
    const clockFeatures = [
      FontFeature.liningFigures(),
      FontFeature.tabularFigures(),
    ];
    return LayoutBuilder(
      builder: (context, constraints) {
        final face = math.min(constraints.maxWidth, 258.0);
        final clockSize = face < 230 ? 44.0 : 56.0;
        return Center(
          child: SizedBox(
            width: face,
            height: face,
            child: Stack(
              alignment: Alignment.center,
              children: [
                CustomPaint(
                  size: Size.square(face),
                  painter: _RingPainter(progress: controller.progress),
                ),
                Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      controller.phase.shortLabel.toUpperCase(),
                      style: sansStyle(
                        fontSize: 8,
                        fontWeight: FontWeight.w600,
                        color: forest,
                        letterSpacing: 2.4,
                      ),
                    ),
                    const SizedBox(height: 7),
                    Semantics(
                      label:
                          '${formatClock(controller.remaining)} remaining',
                      child: ExcludeSemantics(
                        child: Text(
                          formatClock(controller.remaining),
                          style: serifStyle(
                            fontSize: clockSize,
                            fontWeight: FontWeight.w300,
                            height: 1.0,
                            letterSpacing: 0.5,
                            fontFeatures: clockFeatures,
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      'MIN',
                      style: sansStyle(
                        fontSize: 7,
                        fontWeight: FontWeight.w500,
                        color: mutedLight,
                        letterSpacing: 2.2,
                      ),
                    ),
                    const SizedBox(height: 12),
                    _SessionDots(controller: controller),
                    const SizedBox(height: 7),
                    Text(
                      controller.sessionLabel,
                      style: sansStyle(
                        fontSize: 9,
                        fontWeight: FontWeight.w500,
                        color: muted,
                        letterSpacing: 1.2,
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}

class _SessionDots extends StatelessWidget {
  final PomodoroController controller;

  const _SessionDots({required this.controller});

  @override
  Widget build(BuildContext context) {
    return Semantics(
      label:
          '${controller.filledDots} of $sessionsPerCycle focus sessions complete',
      child: ExcludeSemantics(
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            for (var index = 0; index < sessionsPerCycle; index++)
              Padding(
                padding: EdgeInsets.only(
                  right: index == sessionsPerCycle - 1 ? 0 : 6,
                ),
                child: Container(
                  width: 7,
                  height: 7,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    color: index < controller.filledDots
                        ? forest
                        : controller.phase == Phase.focus &&
                                index == controller.filledDots
                            ? mint
                            : stoneLight,
                    border: controller.phase == Phase.focus &&
                            index == controller.filledDots
                        ? Border.all(color: forest, width: 1)
                        : null,
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }
}

class _StatusRow extends StatelessWidget {
  final PomodoroController controller;

  const _StatusRow({required this.controller});

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Container(
          width: 5,
          height: 5,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            color: controller.isRunning ? forest : mint,
          ),
        ),
        const SizedBox(width: 7),
        Flexible(
          child: Text(
            controller.statusText,
            textAlign: TextAlign.center,
            maxLines: 2,
            style: sansStyle(
              fontSize: 11.5,
              color: muted,
              height: 1.35,
            ),
          ),
        ),
      ],
    );
  }
}

class _RhythmCard extends StatelessWidget {
  final PomodoroController controller;

  const _RhythmCard({required this.controller});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: softCardDecoration(radius: 24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const _SectionLabel(index: '02', title: "TODAY'S RHYTHM"),
          const SizedBox(height: 14),
          Row(
            children: [
              Expanded(
                child: _StatBlock(
                  value: '${controller.completedFocus}',
                  label: 'SESSIONS',
                ),
              ),
              Container(width: 1, height: 42, color: border),
              Expanded(
                child: _StatBlock(
                  value: '${controller.focusedMinutes}',
                  label: 'MINUTES',
                ),
              ),
              Container(width: 1, height: 42, color: border),
              Expanded(
                child: _StatBlock(
                  value:
                      '${controller.goalCount}/$sessionsPerCycle',
                  label: 'GOAL',
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          TweenAnimationBuilder<double>(
            tween: Tween(begin: 0, end: controller.goalProgress),
            duration: const Duration(milliseconds: 650),
            curve: Curves.easeOutCubic,
            builder: (context, value, child) {
              return SizedBox(
                height: 7,
                width: double.infinity,
                child: CustomPaint(
                  painter: _ProgressPainter(progress: value),
                ),
              );
            },
          ),
          const SizedBox(height: 8),
          Text(
            controller.goalCaption,
            style: sansStyle(
              fontSize: 11,
              color: muted,
              height: 1.4,
            ),
          ),
        ],
      ),
    );
  }
}

class _StatBlock extends StatelessWidget {
  final String value;
  final String label;

  const _StatBlock({required this.value, required this.label});

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Text(
          value,
          style: serifStyle(
            fontSize: 26,
            fontWeight: FontWeight.w500,
            height: 1.0,
          ),
        ),
        const SizedBox(height: 5),
        Text(
          label,
          style: sansStyle(
            fontSize: 8,
            fontWeight: FontWeight.w600,
            color: mutedLight,
            letterSpacing: 1.2,
          ),
        ),
      ],
    );
  }
}

class _SettingsCard extends StatelessWidget {
  final PomodoroController controller;

  const _SettingsCard({required this.controller});

  @override
  Widget build(BuildContext context) {
    final enabled = !controller.isRunning;
    return Container(
      padding: const EdgeInsets.fromLTRB(18, 18, 18, 4),
      decoration: softCardDecoration(radius: 24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const _SectionLabel(index: '03', title: 'SESSION LENGTHS'),
          const SizedBox(height: 5),
          _StepperRow(
            phase: Phase.focus,
            label: 'Focus session',
            helper: 'Deep work',
            enabled: enabled,
            controller: controller,
          ),
          const Divider(),
          _StepperRow(
            phase: Phase.shortBreak,
            label: 'Short break',
            helper: 'A small reset',
            enabled: enabled,
            controller: controller,
          ),
          const Divider(),
          _StepperRow(
            phase: Phase.longBreak,
            label: 'Long break',
            helper: 'A fuller rest',
            enabled: enabled,
            controller: controller,
          ),
          const SizedBox(height: 2),
        ],
      ),
    );
  }
}

class _StepperRow extends StatelessWidget {
  final Phase phase;
  final String label;
  final String helper;
  final bool enabled;
  final PomodoroController controller;

  const _StepperRow({
    required this.phase,
    required this.label,
    required this.helper,
    required this.enabled,
    required this.controller,
  });

  @override
  Widget build(BuildContext context) {
    final bounds = settingBounds[phase]!;
    final value = controller.settings.minutesFor(phase);
    final canDecrease = enabled && value > bounds.min;
    final canIncrease = enabled && value < bounds.max;

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 12),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  label,
                  style: serifStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.w500,
                    height: 1.1,
                  ),
                ),
                const SizedBox(height: 3),
                Text(
                  helper,
                  style: sansStyle(
                    fontSize: 10.5,
                    color: mutedLight,
                  ),
                ),
              ],
            ),
          ),
          Opacity(
            opacity: enabled ? 1 : 0.5,
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                _SoftIconButton(
                  icon: Icons.remove_rounded,
                  semanticLabel: 'Decrease $label',
                  enabled: canDecrease,
                  onTap: () =>
                      controller.adjustSetting(phase, -bounds.step),
                ),
                SizedBox(
                  width: 48,
                  child: Semantics(
                    label: '$label: $value minutes',
                    child: ExcludeSemantics(
                      child: Column(
                        children: [
                          Text(
                            '$value',
                            style: serifStyle(
                              fontSize: 23,
                              fontWeight: FontWeight.w500,
                              height: 1.0,
                              fontFeatures: const [
                                FontFeature.liningFigures(),
                                FontFeature.tabularFigures(),
                              ],
                            ),
                          ),
                          const SizedBox(height: 2),
                          Text(
                            'MIN',
                            style: sansStyle(
                              fontSize: 7,
                              fontWeight: FontWeight.w600,
                              color: mutedLight,
                              letterSpacing: 1.2,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
                _SoftIconButton(
                  icon: Icons.add_rounded,
                  semanticLabel: 'Increase $label',
                  enabled: canIncrease,
                  onTap: () =>
                      controller.adjustSetting(phase, bounds.step),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _SectionLabel extends StatelessWidget {
  final String index;
  final String title;

  const _SectionLabel({required this.index, required this.title});

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Text(
          index,
          style: sansStyle(
            fontSize: 8,
            fontWeight: FontWeight.w700,
            color: forest,
            letterSpacing: 1.2,
          ),
        ),
        const SizedBox(width: 10),
        Text(
          title,
          style: sansStyle(
            fontSize: 8,
            fontWeight: FontWeight.w700,
            color: mutedLight,
            letterSpacing: 1.6,
          ),
        ),
      ],
    );
  }
}

class _Footer extends StatelessWidget {
  const _Footer();

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              width: 4,
              height: 4,
              decoration: const BoxDecoration(
                shape: BoxShape.circle,
                color: forest,
              ),
            ),
            const SizedBox(width: 8),
            Text(
              'SLOW DOWN  ·  FOCUS  ·  BREATHE',
              style: sansStyle(
                fontSize: 8,
                fontWeight: FontWeight.w600,
                color: mutedLight,
                letterSpacing: 1.6,
              ),
            ),
          ],
        ),
        const SizedBox(height: 7),
        Text(
          'Built for unhurried attention.',
          style: serifStyle(
            fontSize: 15,
            color: muted,
            fontStyle: FontStyle.italic,
          ),
        ),
      ],
    );
  }
}

class _CompletionOverlay extends StatelessWidget {
  final PomodoroController controller;

  const _CompletionOverlay({required this.controller});

  @override
  Widget build(BuildContext context) {
    final info = controller.completion!;
    return GestureDetector(
      behavior: HitTestBehavior.opaque,
      onTap: controller.dismissCompletion,
      child: ColoredBox(
        color: overlay,
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: GestureDetector(
              behavior: HitTestBehavior.opaque,
              onTap: () {},
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 350),
                child: Container(
                  padding: const EdgeInsets.all(24),
                  decoration: softCardDecoration(radius: 28),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Container(
                            width: 50,
                            height: 50,
                            decoration: BoxDecoration(
                              shape: BoxShape.circle,
                              color: mintPale,
                              border: Border.all(
                                color: const Color(0x66B7D9C1),
                              ),
                            ),
                            child: const Icon(
                              Icons.check_rounded,
                              size: 24,
                              color: forest,
                            ),
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  info.eyebrow,
                                  style: sansStyle(
                                    fontSize: 8,
                                    fontWeight: FontWeight.w600,
                                    color: forest,
                                    letterSpacing: 1.5,
                                  ),
                                ),
                                const SizedBox(height: 4),
                                Text(
                                  info.title,
                                  style: serifStyle(
                                    fontSize: 29,
                                    fontWeight: FontWeight.w500,
                                    height: 1.0,
                                  ),
                                ),
                              ],
                            ),
                          ),
                          _PlainIconButton(
                            icon: Icons.close_rounded,
                            semanticLabel: 'Dismiss',
                            onTap: controller.dismissCompletion,
                          ),
                        ],
                      ),
                      const SizedBox(height: 14),
                      Text(
                        info.message,
                        style: sansStyle(
                          fontSize: 12.5,
                          color: muted,
                          height: 1.5,
                        ),
                      ),
                      const SizedBox(height: 18),
                      _SoftButton(
                        label: info.primaryLabel,
                        leadingIcon: Icons.play_arrow_rounded,
                        onTap: controller.startNext,
                        variant: _ButtonVariant.primary,
                      ),
                      const SizedBox(height: 8),
                      _SoftButton(
                        label: 'Later',
                        onTap: controller.dismissCompletion,
                        variant: _ButtonVariant.secondary,
                      ),
                      const SizedBox(height: 15),
                      Container(height: 1, color: border),
                      const SizedBox(height: 10),
                      Text(
                        'AUTOMATIC FLOW  ·  EASY DOES IT',
                        textAlign: TextAlign.center,
                        style: sansStyle(
                          fontSize: 7.5,
                          fontWeight: FontWeight.w600,
                          color: mutedLight,
                          letterSpacing: 1.4,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

enum _ButtonVariant { primary, secondary }

class _SoftButton extends StatefulWidget {
  final String label;
  final VoidCallback? onTap;
  final _ButtonVariant variant;
  final IconData? leadingIcon;
  final IconData? trailingIcon;

  const _SoftButton({
    required this.label,
    required this.onTap,
    required this.variant,
    this.leadingIcon,
    this.trailingIcon,
  });

  @override
  State<_SoftButton> createState() => _SoftButtonState();
}

class _SoftButtonState extends State<_SoftButton> {
  bool _pressed = false;

  @override
  Widget build(BuildContext context) {
    final enabled = widget.onTap != null;
    final primary = widget.variant == _ButtonVariant.primary;
    final contentColor = !enabled
        ? mutedLight
        : primary
            ? ivory
            : forest;

    final Color background;
    if (!enabled) {
      background = stoneLight;
    } else if (primary) {
      background = _pressed ? forestDeep : forest;
    } else {
      background = _pressed ? stoneLight : paper;
    }

    final List<BoxShadow> shadows;
    if (!enabled || _pressed) {
      shadows = const [];
    } else if (primary) {
      shadows = const [
        BoxShadow(
          color: Color(0x330F5132),
          blurRadius: 16,
          offset: Offset(0, 7),
        ),
      ];
    } else {
      shadows = const [
        BoxShadow(
          color: Color(0x1F706B5D),
          blurRadius: 14,
          offset: Offset(0, 7),
        ),
        BoxShadow(
          color: Color(0xBFFFFFFF),
          blurRadius: 10,
          offset: Offset(-4, -4),
        ),
      ];
    }

    return Semantics(
      button: true,
      enabled: enabled,
      label: widget.label,
      child: ExcludeSemantics(
        child: GestureDetector(
          behavior: HitTestBehavior.opaque,
          onTapDown:
              enabled ? (_) => setState(() => _pressed = true) : null,
          onTapUp: enabled ? (_) => setState(() => _pressed = false) : null,
          onTapCancel:
              enabled ? () => setState(() => _pressed = false) : null,
          onTap: widget.onTap,
          child: AnimatedContainer(
            duration: const Duration(milliseconds: 120),
            curve: Curves.easeOutCubic,
            height: 54,
            padding: const EdgeInsets.symmetric(horizontal: 12),
            decoration: BoxDecoration(
              color: background,
              borderRadius: BorderRadius.circular(27),
              border: enabled && !primary
                  ? Border.all(color: const Color(0xCCFFFFFF))
                  : null,
              boxShadow: shadows,
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                if (widget.leadingIcon != null) ...[
                  Icon(
                    widget.leadingIcon,
                    size: 18,
                    color: contentColor,
                  ),
                  const SizedBox(width: 8),
                ],
                Flexible(
                  child: FittedBox(
                    fit: BoxFit.scaleDown,
                    child: Text(
                      widget.label,
                      maxLines: 1,
                      style: serifStyle(
                        fontSize: 19,
                        fontWeight: FontWeight.w500,
                        color: contentColor,
                        height: 1.1,
                      ),
                    ),
                  ),
                ),
                if (widget.trailingIcon != null) ...[
                  const SizedBox(width: 10),
                  Icon(
                    widget.trailingIcon,
                    size: 18,
                    color: contentColor,
                  ),
                ],
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _SoftIconButton extends StatefulWidget {
  final IconData icon;
  final bool enabled;
  final VoidCallback onTap;
  final String semanticLabel;

  const _SoftIconButton({
    required this.icon,
    required this.enabled,
    required this.onTap,
    required this.semanticLabel,
  });

  @override
  State<_SoftIconButton> createState() => _SoftIconButtonState();
}

class _SoftIconButtonState extends State<_SoftIconButton> {
  bool _pressed = false;

  @override
  Widget build(BuildContext context) {
    final enabled = widget.enabled;
    return Semantics(
      button: true,
      enabled: enabled,
      label: widget.semanticLabel,
      child: ExcludeSemantics(
        child: GestureDetector(
          behavior: HitTestBehavior.opaque,
          onTapDown:
              enabled ? (_) => setState(() => _pressed = true) : null,
          onTapUp: enabled ? (_) => setState(() => _pressed = false) : null,
          onTapCancel:
              enabled ? () => setState(() => _pressed = false) : null,
          onTap: enabled ? widget.onTap : null,
          child: AnimatedContainer(
            duration: const Duration(milliseconds: 120),
            width: 36,
            height: 36,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: !enabled
                  ? stoneLight
                  : _pressed
                      ? stoneLight
                      : paper,
              border: enabled
                  ? Border.all(color: const Color(0xCCFFFFFF))
                  : null,
              boxShadow: enabled && !_pressed
                  ? const [
                      BoxShadow(
                        color: Color(0x14706B5D),
                        blurRadius: 9,
                        offset: Offset(0, 4),
                      ),
                      BoxShadow(
                        color: Color(0xBFFFFFFF),
                        blurRadius: 8,
                        offset: Offset(-3, -3),
                      ),
                    ]
                  : null,
            ),
            child: Icon(
              widget.icon,
              size: 18,
              color: enabled ? forest : mutedLight,
            ),
          ),
        ),
      ),
    );
  }
}

class _PlainIconButton extends StatelessWidget {
  final IconData icon;
  final VoidCallback onTap;
  final String semanticLabel;

  const _PlainIconButton({
    required this.icon,
    required this.onTap,
    required this.semanticLabel,
  });

  @override
  Widget build(BuildContext context) {
    return Semantics(
      button: true,
      label: semanticLabel,
      child: ExcludeSemantics(
        child: GestureDetector(
          behavior: HitTestBehavior.opaque,
          onTap: onTap,
          child: SizedBox(
            width: 32,
            height: 32,
            child: Icon(icon, size: 18, color: mutedLight),
          ),
        ),
      ),
    );
  }
}

class _RingPainter extends CustomPainter {
  final double progress;

  const _RingPainter({required this.progress});

  @override
  void paint(Canvas canvas, Size size) {
    const strokeWidth = 8.0;
    final center = size.center(Offset.zero);
    final radius = (size.shortestSide - strokeWidth) / 2;
    final rect = Rect.fromCircle(center: center, radius: radius);

    canvas.drawArc(
      rect,
      -math.pi / 2,
      math.pi * 2,
      false,
      Paint()
        ..color = stoneLight
        ..style = PaintingStyle.stroke
        ..strokeWidth = strokeWidth
        ..strokeCap = StrokeCap.round,
    );

    final clamped = progress.clamp(0.0, 1.0).toDouble();
    if (clamped <= 0.001) return;

    canvas.drawArc(
      rect,
      -math.pi / 2,
      math.pi * 2 * clamped,
      false,
      Paint()
        ..color = forest
        ..style = PaintingStyle.stroke
        ..strokeWidth = strokeWidth
        ..strokeCap = StrokeCap.round,
    );

    if (clamped < 0.996) {
      final angle = -math.pi / 2 + math.pi * 2 * clamped;
      final dot = Offset(
        center.dx + math.cos(angle) * radius,
        center.dy + math.sin(angle) * radius,
      );
      canvas.drawCircle(
        dot,
        strokeWidth * 0.62,
        Paint()..color = mint,
      );
      canvas.drawCircle(
        dot,
        strokeWidth * 0.62,
        Paint()
          ..color = forest
          ..style = PaintingStyle.stroke
          ..strokeWidth = 1.4,
      );
    }
  }

  @override
  bool shouldRepaint(_RingPainter oldDelegate) {
    return oldDelegate.progress != progress;
  }
}

class _ProgressPainter extends CustomPainter {
  final double progress;

  const _ProgressPainter({required this.progress});

  @override
  void paint(Canvas canvas, Size size) {
    final radius = Radius.circular(size.height / 2);
    final track = RRect.fromRectAndRadius(
      Offset.zero & size,
      radius,
    );
    canvas.drawRRect(track, Paint()..color = stoneLight);

    final clamped = progress.clamp(0.0, 1.0).toDouble();
    if (clamped <= 0.001) return;

    final width = math.max(size.height, size.width * clamped);
    final fill = RRect.fromRectAndRadius(
      Rect.fromLTWH(0, 0, width, size.height),
      radius,
    );
    canvas.drawRRect(fill, Paint()..color = forest);
  }

  @override
  bool shouldRepaint(_ProgressPainter oldDelegate) {
    return oldDelegate.progress != progress;
  }
}

class _LeafPainter extends CustomPainter {
  const _LeafPainter();

  @override
  void paint(Canvas canvas, Size size) {
    canvas.save();
    canvas.scale(size.width / 132, size.height / 132);

    final stem = Paint()
      ..color = const Color(0x3D0F5132)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 1.4
      ..strokeCap = StrokeCap.round;

    canvas.drawPath(
      Path()
        ..moveTo(32, 120)
        ..cubicTo(50, 95, 74, 56, 92, 21),
      stem,
    );

    final leafFill = Paint()..color = const Color(0xB8B7D9C1);
    final leafStroke = Paint()
      ..color = const Color(0x3D0F5132)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 1.0;
    final vein = Paint()
      ..color = const Color(0x2E0F5132)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 0.8;

    void drawLeaf(Path path, Offset veinStart, Offset veinEnd) {
      canvas.drawPath(path, leafFill);
      canvas.drawPath(path, leafStroke);
      canvas.drawLine(veinStart, veinEnd, vein);
    }

    drawLeaf(
      Path()
        ..moveTo(46, 92)
        ..cubicTo(26, 88, 18, 72, 26, 60)
        ..cubicTo(42, 62, 54, 75, 46, 92),
      const Offset(31, 75),
      const Offset(45, 87),
    );
    drawLeaf(
      Path()
        ..moveTo(62, 67)
        ..cubicTo(43, 60, 38, 43, 48, 32)
        ..cubicTo(62, 38, 71, 52, 62, 67),
      const Offset(49, 48),
      const Offset(60, 62),
    );
    drawLeaf(
      Path()
        ..moveTo(77, 43)
        ..cubicTo(64, 31, 69, 15, 82, 8)
        ..cubicTo(90, 20, 91, 33, 77, 43),
      const Offset(78, 22),
      const Offset(80, 38),
    );
    drawLeaf(
      Path()
        ..moveTo(88, 23)
        ..cubicTo(96, 14, 109, 15, 114, 22)
        ..cubicTo(108, 33, 95, 34, 88, 23),
      const Offset(101, 22),
      const Offset(108, 26),
    );
    drawLeaf(
      Path()
        ..moveTo(54, 86)
        ..cubicTo(53, 102, 64, 114, 76, 116)
        ..cubicTo(78, 102, 67, 89, 54, 86),
      const Offset(61, 98),
      const Offset(70, 109),
    );

    canvas.drawPath(
      Path()
        ..moveTo(96, 39)
        ..lineTo(116, 26)
        ..lineTo(116, 31)
        ..lineTo(101, 39)
        ..lineTo(116, 47)
        ..lineTo(116, 52)
        ..lineTo(96, 39)
        ..close(),
      leafFill,
    );
    canvas.drawPath(
      Path()
        ..moveTo(96, 39)
        ..lineTo(116, 26)
        ..lineTo(116, 31)
        ..lineTo(101, 39)
        ..lineTo(116, 47)
        ..lineTo(116, 52)
        ..lineTo(96, 39)
        ..close(),
      leafStroke,
    );

    canvas.restore();
  }

  @override
  bool shouldRepaint(_LeafPainter oldDelegate) => false;
}
