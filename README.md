# Offline Games Host (Android + lokaler Webserver)

Diese App macht ein Android-Gerät zum lokalen Multiplayer-Host im WLAN. Spieler treten per Browser bei (`http://<host-ip>:3000`).

## Features
- Lokaler Server auf Android (Ktor/CIO)
- Server Start/Stop in der App
- Lobby-Code erstellen
- QR-Code Join-Link
- Tic-Tac-Toe mit Live-Sync (WebSocket)
- Zuschauer möglich (mehr als 2 Clients können verbunden sein)
- Host-Reset des Spiels

## Projektstruktur
- `app/src/main/java/com/offlinegames/host/MainActivity.kt`: Android Host-UI
- `app/src/main/java/com/offlinegames/host/LocalGameServer.kt`: eingebetteter HTTP/WebSocket Server + Spielzustand
- `app/src/main/assets/web/index.html`: Webclient für Spieler

## APK bauen
1. Android Studio (Hedgehog/Koala oder neuer) installieren.
2. Projekt öffnen.
3. Gradle Sync laufen lassen.
4. Build > Build Bundle(s) / APK(s) > Build APK(s).
5. APK liegt anschließend unter `app/build/outputs/apk/debug/app-debug.apk`.

CLI:
```bash
./gradlew assembleDebug
```

## Nutzung
1. Host startet App.
2. Server startet automatisch.
3. Host erstellt Lobby.
4. Andere Geräte im selben WLAN öffnen IP:Port oder scannen QR-Code.
5. Code eingeben und spielen.

## Neue Spiele hinzufügen
1. Neue Spiel-Engine im Backend anlegen (z. B. `RockPaperScissorsState`).
2. In `LocalGameServer` neues Routing + Message-Typen ergänzen.
3. Im Webclient Spielauswahl und neue UI-Sektion ergänzen.
4. Admin-Panel API erweitern (`/api/admin/*`) für Kick/Close/Reset je Spiel.

Empfohlene Erweiterungsrichtung:
- `GameModule` Interface (join/move/reset/state)
- Registry `Map<String, GameModule>` für dynamische Spielauswahl
- Separate Web-Seiten pro Spiel unter `assets/web/games/*`
