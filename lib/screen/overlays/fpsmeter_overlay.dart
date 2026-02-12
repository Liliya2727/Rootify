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
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return Directionality(
      textDirection: TextDirection.ltr,
      child: GestureDetector(
        behavior: HitTestBehavior.opaque,
        onPanUpdate: (details) {
          if (!_isLocked) {
            // Kita pindahkan Jendela-nya, bukan Widget-nya
            FlutterOverlayWindow.moveOverlay(
              OverlayPosition(
                details.globalPosition.dx,
                details.globalPosition.dy,
              ),
            );
          }
        },
        // Hapus Stack dan Align. Langsung panggil Pill.
        // Berikan Center agar Pill tetap rapi di dalam jendela kecilnya.
        child: Center(
          child: _FpsPill(isLocked: _isLocked),
        ),
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
    if (fps < 30) statusColor = const Color(0xFFFF1744);
    else if (fps < 50) statusColor = const Color(0xFFFF9100);

    return Material(
      type: MaterialType.transparency,
      child: Container(
        // Pakai padding agar konten tidak mepet ke pinggir jendela
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
        decoration: BoxDecoration(
          color: const Color(0xEE121212),
          borderRadius: BorderRadius.circular(50), 
          border: Border.all(
            color: statusColor.withOpacity(0.4),
            width: 1.5,
          ),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min, // Penting agar tidak melar
          children: [
            // Dot indicator
            Container(
              width: 6,
              height: 6,
              decoration: BoxDecoration(color: statusColor, shape: BoxShape.circle),
            ),
            const SizedBox(width: 6),
            const Text(
              "FPS",
              style: TextStyle(fontSize: 10, fontWeight: FontWeight.bold, color: Colors.white70),
            ),
            const SizedBox(width: 4),
            Text(
              fps.toStringAsFixed(0),
              style: const TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.black,
                fontFamily: 'Monospace',
                color: Colors.white,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
