import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'pomodoro_screen.dart';
import 'theme.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);
  SystemChrome.setSystemUIOverlayStyle(
    const SystemUiOverlayStyle(
      statusBarColor: Colors.transparent,
      statusBarIconBrightness: Brightness.dark,
      statusBarBrightness: Brightness.light,
      systemStatusBarContrastEnforced: false,
      systemNavigationBarColor: ivory,
      systemNavigationBarIconBrightness: Brightness.dark,
      systemNavigationBarDividerColor: ivory,
      systemNavigationBarContrastEnforced: false,
    ),
  );
  runApp(const PomodoroApp());
}

class PomodoroApp extends StatelessWidget {
  const PomodoroApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Pomodoro',
      debugShowCheckedModeBanner: false,
      theme: buildTheme(),
      home: const PomodoroScreen(),
    );
  }
}
