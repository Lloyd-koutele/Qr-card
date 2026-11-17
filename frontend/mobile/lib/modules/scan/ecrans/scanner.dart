import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:mobile_scanner/mobile_scanner.dart';

import '../../../commun/widgets.dart';
import '../lien_madeqrcard.dart';

/// Scanne la carte de quelqu'un pour l'ouvrir dans l'application.
class EcranScanner extends StatefulWidget {
  const EcranScanner({super.key});

  @override
  State<EcranScanner> createState() => _EtatScanner();
}

class _EtatScanner extends State<EcranScanner> {
  final _camera = MobileScannerController(
    detectionSpeed: DetectionSpeed.noDuplicates,
  );
  bool _ouverture = false;

  @override
  void dispose() {
    _camera.dispose();
    super.dispose();
  }

  Future<void> _ouvrir(String? texte) async {
    if (_ouverture) return;
    final lien = LienMadeQrCard.lire(texte);
    if (lien == null) {
      annoncer(context, "Ce QR code n'est pas une carte MadeQrCard");
      return;
    }
    _ouverture = true;
    await _camera.stop();
    if (!mounted) return;
    await context.push(switch (lien) {
      LienCarte(:final jeton) => '/c/$jeton',
      LienInvitation(:final jeton) => '/invitation/$jeton',
    });
    _ouverture = false;
    if (mounted) await _camera.start();
  }

  Future<void> _saisir() async {
    final texte = await demanderTexte(
      context,
      titre: 'Ouvrir un lien',
      action: 'Ouvrir',
      indice: 'https://madeqrcard.sn/c/…',
      clavier: TextInputType.url,
    );
    if (texte != null) await _ouvrir(texte);
  }

  @override
  Widget build(BuildContext context) {
    // Fond noir : les textes blancs restent lisibles meme sans camera
    return ColoredBox(
      color: Colors.black,
      child: Stack(
        fit: StackFit.expand,
        children: [
          MobileScanner(
            controller: _camera,
            onDetect: (capture) =>
                _ouvrir(capture.barcodes.firstOrNull?.rawValue),
            errorBuilder: (context, erreur) => Center(
              child: Padding(
                padding: const EdgeInsets.all(24),
                child: Text(
                  erreur.errorCode == MobileScannerErrorCode.permissionDenied
                      ? "L'accès à la caméra est refusé. Autorisez-le dans les réglages du téléphone."
                      : 'Caméra indisponible sur cet appareil.',
                  textAlign: TextAlign.center,
                  style: const TextStyle(color: Colors.white),
                ),
              ),
            ),
          ),
          IgnorePointer(
            child: Center(
              child: Container(
                width: 240,
                height: 240,
                decoration: BoxDecoration(
                  border: Border.all(color: Colors.white, width: 3),
                  borderRadius: BorderRadius.circular(20),
                ),
              ),
            ),
          ),
          Positioned(
            left: 16,
            right: 16,
            bottom: 24,
            child: SafeArea(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Text(
                    'Visez le QR code d\'une carte MadeQrCard',
                    style: TextStyle(color: Colors.white, fontSize: 16),
                  ),
                  const SizedBox(height: 12),
                  FilledButton.tonalIcon(
                    onPressed: _saisir,
                    icon: const Icon(Icons.link),
                    label: const Text('Saisir un lien'),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
