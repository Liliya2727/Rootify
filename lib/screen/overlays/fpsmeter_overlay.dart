/*
 * Copyright (C) 2026 Rootify - Aby - FoxLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// ---- SYSTEM ---
import 'package:flutter/material.dart';

// ---- EXTERNAL ---
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_overlay_window/flutter_overlay_window.dart';

// ---- LOCAL ---
import '../../services/system_monitor.dart';

// ---- MAJOR ---
// Main Overlay Component
class FpsMeterOverlay extends ConsumerStatefulWidget {
  const FpsMeterOverlay({super.key});

  @override
  ConsumerState<FpsMeterOverlay> createState() => _FpsMeterOverlayState();
}

// ---- MAJOR ---
class _FpsMeterOverlayState extends ConsumerState<FpsMeterOverlay> {
  bool _isLocked = false;

  @override
  void initState() {
    super.initState();
    FlutterOverlayWindow.overlayListener.listen((data) {
      if (data is Map && data.containsKey('locked')) {
        setState(() {
          _isLocked = data['locked'] == true;
        });
        // Catatan: Jika ingin overlay tetap di posisi terakhir, 
        // sebaiknya hapus logika moveOverlay(0,0) di sini.
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return Directionality(
      textDirection: TextDirection.ltr,
      child: Stack( // Gunakan Stack agar kita bisa mengontrol posisi & ukuran
        children: [
          Align(
            alignment: Alignment.topLeft, // Memaksa widget mengkerut ke pojok
            child: GestureDetector(
              behavior: HitTestBehavior.opaque,
              onPanUpdate: (details) {
                if (!_isLocked) {
                  FlutterOverlayWindow.moveOverlay(
                    OverlayPosition(
                      details.globalPosition.dx,
                      details.globalPosition.dy,
                    ),
                  );
                }
              },
              child: _FpsPill(isLocked: _isLocked),
            ),
          ),
        ],
      ),
    );
  }
}

// ---- UI COMPONENT ---
class _FpsPill extends ConsumerWidget {
  final bool isLocked;
  const _FpsPill({this.isLocked = false});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final stats = ref.watch(systemMonitorProvider).asData?.value;
    final double fps = stats?.fps ?? 0.0;

    Color statusColor = const Color(0xFF00E676);
    if (fps < 30) {
      statusColor = const Color(0xFFFF1744);
    } else if (fps < 50) {
      statusColor = const Color(0xFFFF9100);
    }

    return Material(
      type: MaterialType.transparency,
      child: Container(
        // Padding horizontal lebih lebar dari vertical untuk efek "Pill"
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
        decoration: BoxDecoration(
          color: const Color(0xEE000000), 
          // Pakai radius besar (misal 50) untuk bentuk Pill sempurna
          borderRadius: BorderRadius.circular(50), 
          border: Border.all(
            color: statusColor.withOpacity(0.5),
            width: 1.5,
          ),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withOpacity(0.3),
              blurRadius: 8,
              offset: const Offset(0, 3),
            )
          ]
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min, // SANGAT PENTING agar tidak melar horizontal
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            // Indikator titik (dot) biar makin estetik
            Container(
              width: 6,
              height: 6,
              decoration: BoxDecoration(
                color: statusColor,
                shape: BoxShape.circle,
              ),
            ),
            const SizedBox(width: 8),
            Text(
              "FPS",
              style: TextStyle(
                fontSize: 10,
                fontWeight: FontWeight.bold,
                color: Colors.white.withOpacity(0.7),
              ),
            ),
            const SizedBox(width: 6),
            Text(
              fps.toStringAsFixed(0),
              style: TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.w900,
                fontFamily: 'Monospace',
                color: Colors.white,
                height: 1.2,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

