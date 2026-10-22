import 'dart:ui' show FontFeature;

import 'package:flutter/material.dart';

const ivory = Color(0xFFFAF9F4);
const parchment = Color(0xFFF4F1E8);
const paper = Color(0xFFFFFEF9);
const forest = Color(0xFF0F5132);
const forestDeep = Color(0xFF0A3D26);
const mint = Color(0xFFB7D9C1);
const mintPale = Color(0xFFE4F0E7);
const stone = Color(0xFFD6D3C8);
const stoneLight = Color(0xFFEBE8DE);
const ink = Color(0xFF18241D);
const muted = Color(0xFF5F695F);
const mutedLight = Color(0xFF90998E);
const border = Color(0xFFE4E0D4);
const error = Color(0xFFA34B3F);
const overlay = Color(0x591E2B23);

const serifFamily = 'Cormorant';

TextStyle serifStyle({
  double fontSize = 16,
  FontWeight fontWeight = FontWeight.w400,
  Color color = ink,
  double? height,
  double? letterSpacing,
  FontStyle? fontStyle,
  List<FontFeature>? fontFeatures,
}) {
  return TextStyle(
    fontFamily: serifFamily,
    fontSize: fontSize,
    fontWeight: fontWeight,
    color: color,
    height: height,
    letterSpacing: letterSpacing,
    fontStyle: fontStyle,
    fontFeatures: fontFeatures,
  );
}

TextStyle sansStyle({
  double fontSize = 12,
  FontWeight fontWeight = FontWeight.w400,
  Color color = ink,
  double? height,
  double? letterSpacing,
}) {
  return TextStyle(
    fontSize: fontSize,
    fontWeight: fontWeight,
    color: color,
    height: height,
    letterSpacing: letterSpacing,
  );
}

BoxDecoration softCardDecoration({Color color = paper, double radius = 26}) {
  return BoxDecoration(
    color: color,
    borderRadius: BorderRadius.circular(radius),
    border: Border.all(color: const Color(0xBFFFFFFF)),
    boxShadow: const [
      BoxShadow(
        color: Color(0x1F706B5D),
        blurRadius: 18,
        offset: Offset(0, 9),
      ),
      BoxShadow(
        color: Color(0xCCFFFFFF),
        blurRadius: 12,
        offset: Offset(-5, -5),
      ),
    ],
  );
}

ThemeData buildTheme() {
  final scheme = ColorScheme.fromSeed(
    seedColor: forest,
    brightness: Brightness.light,
  ).copyWith(
    primary: forest,
    onPrimary: ivory,
    primaryContainer: mint,
    onPrimaryContainer: forestDeep,
    secondary: mint,
    onSecondary: ink,
    surface: paper,
    onSurface: ink,
    error: error,
  );

  return ThemeData(
    useMaterial3: true,
    colorScheme: scheme,
    scaffoldBackgroundColor: ivory,
    splashFactory: NoSplash.splashFactory,
    highlightColor: Colors.transparent,
    textSelectionTheme: const TextSelectionThemeData(
      cursorColor: forest,
      selectionColor: Color(0x55B7D9C1),
      selectionHandleColor: forest,
    ),
    textTheme: ThemeData.light().textTheme.apply(
      bodyColor: ink,
      displayColor: ink,
    ),
    dividerTheme: const DividerThemeData(
      color: border,
      thickness: 1,
      space: 1,
    ),
  );
}
