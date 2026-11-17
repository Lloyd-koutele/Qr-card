import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

/// Barre de navigation du bas : une pile de pages par onglet.
class Coquille extends StatelessWidget {
  const Coquille({super.key, required this.navigation});

  final StatefulNavigationShell navigation;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(bottom: false, child: navigation),
      bottomNavigationBar: NavigationBar(
        selectedIndex: navigation.currentIndex,
        onDestinationSelected: (i) => navigation.goBranch(
          i,
          initialLocation: i == navigation.currentIndex,
        ),
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.badge_outlined),
            selectedIcon: Icon(Icons.badge),
            label: 'Ma carte',
          ),
          NavigationDestination(
            icon: Icon(Icons.qr_code_scanner),
            label: 'Scanner',
          ),
          NavigationDestination(
            icon: Icon(Icons.bookmarks_outlined),
            selectedIcon: Icon(Icons.bookmarks),
            label: 'Carnet',
          ),
          NavigationDestination(
            icon: Icon(Icons.business_outlined),
            selectedIcon: Icon(Icons.business),
            label: 'Organisations',
          ),
          NavigationDestination(
            icon: Icon(Icons.settings_outlined),
            selectedIcon: Icon(Icons.settings),
            label: 'Paramètres',
          ),
        ],
      ),
    );
  }
}
