import 'package:flutter/material.dart';

const rougeMadeQrCard = Color(0xFFC8102E);

ThemeData theme(Brightness luminosite) {
  final couleurs = ColorScheme.fromSeed(
    seedColor: rougeMadeQrCard,
    brightness: luminosite,
    primary: luminosite == Brightness.light ? rougeMadeQrCard : null,
  );
  return ThemeData(
    colorScheme: couleurs,
    useMaterial3: true,
    inputDecorationTheme: const InputDecorationTheme(
      border: OutlineInputBorder(),
    ),
    filledButtonTheme: FilledButtonThemeData(
      style: FilledButton.styleFrom(minimumSize: const Size.fromHeight(48)),
    ),
    outlinedButtonTheme: OutlinedButtonThemeData(
      style: OutlinedButton.styleFrom(minimumSize: const Size.fromHeight(44)),
    ),
    cardTheme: CardThemeData(
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(14),
        side: BorderSide(color: couleurs.outlineVariant),
      ),
    ),
  );
}
