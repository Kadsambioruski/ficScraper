

import 'dart:ui';

import 'package:flutter/material.dart';

ThemeData lightMode = ThemeData(
  brightness: Brightness.light,
  cardColor: Colors.grey,
  scaffoldBackgroundColor: Colors.white,
  dividerColor:  const Color(0xFFE0E0E0),
  colorScheme: const ColorScheme.light(
    primary: Color(0xFF4F46E5),
    surface:Color(0xFFFFFFFF),
    onSurface: Color(0xFF212121),
    onSurfaceVariant: Color(0xFF666666),
    ),
);


ThemeData darkMode = ThemeData(
  brightness: Brightness.dark,

  scaffoldBackgroundColor: const Color(0xFF121212),

  cardColor: const Color(0xFF1E1E1E),

  dividerColor: const Color(0xFF2D2D2D),

  colorScheme: const ColorScheme.dark(
    primary: Color(0xFF6366F1),
    surface: Color(0xFF1E1E1E),
    onSurface: Color(0xFFE0E0E0),
    onSurfaceVariant: Color(0xFFB0B0B0),
  ),
);