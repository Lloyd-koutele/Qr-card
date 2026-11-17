import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'app/application.dart';

void main() {
  // Pas de nouvel essai automatique : une erreur s'affiche et l'utilisateur relance lui-meme.
  runApp(
    ProviderScope(retry: (essai, erreur) => null, child: const Application()),
  );
}
