import { StyleSheet, View } from 'react-native';
import type { ReactNode } from 'react';
import Svg, { Circle } from 'react-native-svg';

import { colors } from '../theme';

type ProgressRingProps = {
  size: number;
  strokeWidth?: number;
  progress: number;
  children: ReactNode;
};

export function ProgressRing({
  size,
  strokeWidth = 14,
  progress,
  children,
}: ProgressRingProps) {
  const center = size / 2;
  const radius = (size - strokeWidth) / 2;
  const circumference = 2 * Math.PI * radius;
  const clampedProgress = Math.min(1, Math.max(0, progress));
  const dashOffset = circumference * (1 - clampedProgress);

  return (
    <View style={[styles.container, { height: size, width: size }]}>
      <Svg height={size} style={StyleSheet.absoluteFill} width={size}>
        <Circle
          cx={center}
          cy={center}
          fill="none"
          r={radius}
          stroke={colors.border}
          strokeWidth={strokeWidth}
        />
        <Circle
          cx={center}
          cy={center}
          fill="none"
          r={radius}
          stroke={colors.primary}
          strokeDasharray={[circumference, circumference]}
          strokeDashoffset={dashOffset}
          strokeLinecap="round"
          strokeWidth={strokeWidth}
          transform={`rotate(-90 ${center} ${center})`}
        />
      </Svg>
      <View style={[styles.content, { width: size - strokeWidth * 4 }]}>{children}</View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    alignItems: 'center',
    justifyContent: 'center',
  },
  content: {
    alignItems: 'center',
    justifyContent: 'center',
  },
});
