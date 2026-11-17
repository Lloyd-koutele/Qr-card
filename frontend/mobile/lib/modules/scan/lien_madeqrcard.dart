/// Ce qu'un QR code ou un lien colle designe dans MadeQrCard.
sealed class LienMadeQrCard {
  const LienMadeQrCard();

  /// Reconnait /c/{jeton} (carte) et /invitation/{jeton}, quel que soit le domaine.
  static LienMadeQrCard? lire(String? texte) {
    final uri = Uri.tryParse((texte ?? '').trim());
    if (uri == null) return null;
    final morceaux = uri.pathSegments.where((s) => s.isNotEmpty).toList();
    if (morceaux.length != 2 ||
        !RegExp(r'^[A-Za-z0-9_-]{8,64}$').hasMatch(morceaux[1]))
      return null;
    return switch (morceaux[0]) {
      'c' => LienCarte(morceaux[1]),
      'invitation' => LienInvitation(morceaux[1]),
      _ => null,
    };
  }
}

class LienCarte extends LienMadeQrCard {
  const LienCarte(this.jeton);

  final String jeton;
}

class LienInvitation extends LienMadeQrCard {
  const LienInvitation(this.jeton);

  final String jeton;
}
