# XrayPulse

A modern **Android Xray client** with a polished Material 3 UI. Built for the latest **Xray-core** features used by [v2rayN](https://github.com/2dust/v2rayN) / [v2rayNG](https://github.com/2dust/v2rayNG):

| Protocol | Transports | Security |
|----------|------------|----------|
| **VLESS** | TCP, WS, gRPC, HTTPUpgrade, **XHTTP**, KCP, QUIC, H2 | none, TLS, **REALITY** |
| **VMess** | same | TLS / REALITY |
| **Trojan** | same | TLS / REALITY |
| **Shadowsocks** | TCP | AEAD methods |
| Custom JSON | full Xray config | — |

### Xray features wired in the config builder

- **VLESS + REALITY + Vision** (`flow: xtls-rprx-vision`)
- **XHTTP** transport (modern successor to SplitHTTP)
- **uTLS fingerprints** (`chrome`, `firefox`, `safari`, `ios`, `random`, …)
- **Mux / XUDP**, domain sniffing, DoH DNS
- Routing: Global · Bypass LAN · Bypass CN (`geoip` / `geosite`)
- Optional TLS fragment settings (anti-DPI)
- Share-link import compatible with v2rayN (`vless://`, `vmess://`, `trojan://`, `ss://`, base64 subs)

---

## Screens

- **Home** — large connect power button, live stats, server card  
- **Servers** — search, latency badges, swipe-to-delete  
- **Import** — paste links, subscription URL, QR scan, manual VLESS form  
- **Settings** — routing, sniffing, mux, fragment, auto-connect  

UI: deep-space dark theme, glass cards, cyan/violet accents (Jetpack Compose + Material 3).

---

## Project structure

```
app/src/main/java/com/xraypulse/app/
  core/
    config/XrayConfigBuilder.kt   # Xray JSON builder
    parser/ShareLinkParser.kt     # v2rayN-style link parsing
    parser/ShareLinkExporter.kt
    xray/XrayController.kt        # libv2ray / libXray bridge
  service/XrayVpnService.kt       # Android VpnService
  data/                           # Room + DataStore
  ui/                             # Compose screens & theme
```

---

## Build

### Quick build (this machine)

Uses your installed **Android Studio JBR**, **Gradle 8.11.1** cache, and **Android SDK**:

```powershell
.\scripts\build-debug.ps1
```

APK output:

```
app\build\outputs\apk\debug\app-debug.apk
```

Toolchain pinned to match your other projects / local Gradle cache:

| Tool | Version / path |
|------|----------------|
| JDK | Android Studio JBR 21 (or `C:\Program Files\Java\jdk-17.0.2`) |
| Gradle | 8.11.1 (`%USERPROFILE%\.gradle\wrapper\dists\…`) |
| AGP | 8.9.2 |
| Kotlin | 1.9.22 |
| KSP | 1.9.22-1.0.17 |
| SDK | `%LOCALAPPDATA%\Android\Sdk` (`local.properties`) |

Maven mirrors: Aliyun Google/public (because `dl.google.com` is unreachable from Java on this network).

### Open in Android Studio

- Open the `G:\grok\app3` folder  
- Use the embedded JBR / project JDK 17+  
- Sync Gradle and run **app**  

The UI runs without the native Xray AAR (preview / stub mode).

### 2. Link real Xray-core (required for traffic)

The Go core is shipped as an AAR from [AndroidLibXrayLite](https://github.com/2dust/AndroidLibXrayLite) (same stack as v2rayNG):

**Linux / macOS / WSL**

```bash
chmod +x scripts/build-libxray.sh
./scripts/build-libxray.sh
```

This produces `app/libs/libv2ray.aar`. Rebuild the app.

**Manual**

```bash
git clone https://github.com/2dust/AndroidLibXrayLite.git
cd AndroidLibXrayLite
gomobile init
go mod tidy
gomobile bind -v -androidapi 24 -trimpath -ldflags='-s -w -buildid= -checklinkname=0' ./
cp libv2ray.aar /path/to/XrayPulse/app/libs/
```

Optional: download routing databases

```powershell
./scripts/download-geo.ps1
```

### 3. Install

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Usage

1. **Import** a `vless://…` link (or subscription).  
2. Select the server under **Servers**.  
3. On **Home**, tap the power button and accept the VPN permission.  
4. Adjust **Settings** (Bypass LAN / CN, mux, sniffing).

Example VLESS + REALITY share link shape:

```
vless://UUID@host:443?encryption=none&flow=xtls-rprx-vision&security=reality&sni=www.example.com&fp=chrome&pbk=PUBLIC_KEY&sid=SHORT_ID&type=tcp#MyServer
```

---

## Architecture notes

- **Config generation** follows current Xray JSON (`realitySettings`, `xhttpSettings`, Vision flow, hybrid domain matcher).  
- **XrayController** uses reflection so the project compiles without the AAR; once `libv2ray.aar` is present, the real core is used.  
- **VPN path** creates a TUN interface (`VpnService`) and starts Xray with local SOCKS/HTTP inbounds. For production-grade packet bridging, integrate [hev-socks5-tunnel](https://github.com/heiher/hev-socks5-tunnel) the same way v2rayNG does (JNI redirect from TUN → SOCKS).  
- Protocols and share formats are aligned with **v2rayN / v2rayNG**, not a fork of their UI.

---

## License

App code: MIT (this repository).  
Xray-core / AndroidLibXrayLite: their respective licenses (MPL-2.0 / project licenses).  
Use only with servers and networks you are authorized to access.

---

## Roadmap ideas

- [ ] Native hev-socks5-tunnel integration for full system proxy performance  
- [ ] Per-app proxy picker UI  
- [ ] Config fragment dialerProxy chain  
- [ ] WireGuard / Hysteria2 outbounds when using multi-core  
- [ ] Latency-based auto server pick  
