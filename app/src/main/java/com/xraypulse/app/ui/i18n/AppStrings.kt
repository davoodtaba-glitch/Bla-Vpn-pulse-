package com.xraypulse.app.ui.i18n

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import java.io.File

/**
 * Built-in EN/FA string tables.
 * Brand name "BLA VPN" and version are never translated in UI.
 * Technical terms (VLESS, TLS, DNS, Mux, SOCKS, HTTP, VPN, DPI, geoip, etc.) stay as-is.
 */
object AppStrings {

    private val gson = Gson()
    private val prettyGson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

    val en: Map<String, String> = linkedMapOf(
        "home" to "Home",
        "servers" to "Servers",
        "import" to "Import",
        "settings" to "Settings",
        "subscriptions" to "Subscriptions",
        "quick_setup" to "Quick setup",
        "choose_server" to "Choose server",
        "no_server" to "No server selected",
        "tap_connect" to "Tap to connect",
        "tap_disconnect" to "Tap to disconnect",
        "protected" to "Protected",
        "disconnected" to "Disconnected",
        "connecting" to "Connecting…",
        "session_time" to "Session time",
        "session_traffic" to "Session traffic",
        "unlimited" to "unlimited",
        "no_data" to "No data",
        "limit_almost_up" to "Limit almost up",
        "high_usage" to "High usage",
        "establishing_tunnel" to "Establishing tunnel…",
        "connected_no_traffic" to "Connected but no traffic — check server",
        "not_tested" to "Not tested",
        "quick_test" to "Quick test active config",
        "subscription" to "Subscription",
        "traffic" to "Traffic",
        "expire" to "Expire",
        "used" to "used",
        "apply_changes" to "Apply changes",
        "discard" to "Discard",
        "ok" to "OK",
        "cancel" to "Cancel",
        "back" to "Back",
        "continue_btn" to "Continue",
        "paste" to "Paste",
        "delete" to "Delete",
        "delete_all" to "Delete all",
        "delete_selected" to "Delete selected",
        "delete_invalid" to "Delete invalid",
        "select_all" to "Select all",
        "multi_select" to "Multi-select",
        "edit" to "Edit",
        "refresh" to "Refresh",
        "test_all" to "Test all",
        "sort_by_delay" to "Sort by delay",
        "search_servers" to "Search servers…",
        "no_servers_yet" to "No servers yet",
        "import_hint_empty" to "Import a share link or subscription",
        "select_servers" to "Select servers",
        "selected_count" to "selected",
        "configurations" to "configs",
        "testing_connections" to "Testing connections…",
        "active" to "ACTIVE",
        "help" to "Help",
        "got_it" to "Got it",
        "language" to "Language",
        "english" to "English",
        "persian" to "فارسی",
        "ui_language_hint" to "Choose the interface language. Persian uses right-to-left layout. App name and version stay English.",
        "appearance" to "Appearance",
        "theme_frame_color" to "Theme / frame color",
        "custom_color" to "Custom color",
        "custom_color_hint" to "Pick any color for frames, glow and accents",
        "session_limits" to "Session limits",
        "routing" to "Routing mode",
        "routing_mode" to "Routing mode",
        "core_options" to "Core options",
        "per_app_proxy" to "Per-app proxy",
        "tls_fragment" to "TLS Fragment (anti-DPI)",
        "local_ports" to "Local ports",
        "about" to "About",
        "unsaved_changes" to "Unsaved changes — tap Apply to save",
        "settings_subtitle" to "Routing, DNS, TLS Fragment and advanced options",
        "limit_action" to "When limit is reached",
        "limit_action_notify" to "Notify only (keep VPN on)",
        "limit_action_disconnect" to "Disconnect VPN",
        "limit_action_hint" to "Choose whether to only warn or also stop the VPN when time/data limits hit 100%. Progress bars on Home use these values. 0 = unlimited.",
        "domain_sniffing" to "Domain sniffing",
        "mux" to "Mux",
        "allow_insecure" to "Allow insecure TLS",
        "auto_connect" to "Auto connect on boot",
        "keep_alive" to "Keep-alive",
        "keep_alive_hint" to "While VPN is connected, send a light request through the local proxy so idle tunnels and NAT mappings stay warm. Default is on.",
        "keep_alive_interval" to "Interval (minutes)",
        "keep_alive_interval_hint" to "How often to ping (1–120 minutes). Default is 1 minute.",
        "allow_lan_proxy" to "Allow LAN devices to use this proxy",
        "allow_lan_proxy_hint" to "Bind SOCKS and HTTP to all network interfaces so other phones/PCs on the same Wi‑Fi can use this device as a proxy (use this phone’s Wi‑Fi IP + the ports below). Leave off if you only need VPN on this phone.",
        "bypass_domains" to "Direct domains (bypass VPN)",
        "bypass_domains_hint" to "One pattern per line. Matching hosts go direct (not through the proxy).\nExamples: example.com · *.example.com · *.example.com* · *cdn*\nWildcards (*) are supported. Lines starting with # are comments.",
        "ports_edit_hint" to "Local SOCKS and HTTP ports that Xray listens on inside the device. Change only if another app already uses the default ports.",
        "ports_restart_hint" to "Port or DNS changes need Apply. If VPN is on, it will reconnect.",
        "rename_subscription" to "Rename subscription",
        "time_limit" to "Time limit",
        "traffic_limit" to "Traffic limit",
        "global_proxy" to "Global proxy",
        "bypass_lan" to "Bypass LAN",
        "configure_apps" to "Configure apps",
        "per_app_on" to "On · {n} apps · mode={mode}",
        "per_app_off" to "Off — tap to choose which apps use VPN",
        "per_app_saved" to "(Saved immediately in per-app screen)",
        "scan_qr" to "Scan QR",
        "manual_vless" to "Manual VLESS",
        "manage_subscriptions" to "Manage subscriptions",
        "import_links" to "Links",
        "import_links_hint" to "vless:// · vmess:// · trojan:// · ss:// · https:// subscription · multi-line",
        "import_btn" to "Import",
        "name_subscription" to "Name subscription(s)",
        "name" to "Name",
        "import_all" to "Import all",
        "sources" to "sources",
        "no_subscriptions" to "No subscriptions. Add one from Import.",
        "servers_title" to "Servers",
        "never" to "never",
        "tab_all" to "All",
        "tab_manual" to "Manual",
        "delete_all_title" to "Delete all configs?",
        "delete_all_body" to "This removes every server permanently.",
        "delete_selected_title" to "Delete selected?",
        "delete_selected_body" to "Remove {n} server(s)? This cannot be undone.",
        "enable_fragment" to "Enable fragment",
        "fragment_desc" to "Splits TLS ClientHello / TCP packets to bypass DPI. Uses freedom dialerProxy chain (v2rayN style).",
        "packets_type" to "Packets type",
        "packets_hint" to "tlshello = fragment TLS handshake · 1-x = first N TCP packets",
        "length_range" to "Length (bytes range)",
        "custom_length" to "Custom length",
        "interval_range" to "Interval / delay (ms range)",
        "custom_interval" to "Custom interval",
        "max_split" to "Max split (optional)",
        "custom_max_split" to "Custom maxSplit",
        "custom_minutes" to "Custom minutes (0 = off)",
        "custom_mb" to "Custom MB (0 = off)",
        // Quick setup
        "qs_title" to "Quick setup",
        "qs_what" to "What do you want to do?",
        "qs_import" to "I want to import a config",
        "qs_color" to "I want to change the color",
        "qs_limits" to "I want to set time or data limits",
        "qs_language" to "I want to change language",
        "qs_settings" to "Open full settings",
        "qs_have_config" to "Do you have a config or subscription link?",
        "qs_yes" to "Yes",
        "qs_no" to "No",
        "qs_no_config_msg" to "No problem! Get a trusted config from a reliable provider, or buy a plan from your service. When you have a vless:// link or https:// subscription URL, come back and use Quick setup again.",
        "qs_copied_hint" to "Copy your link from the provider app or website first.",
        "qs_have_copied" to "Did you copy your config or subscription link?",
        "qs_paste_title" to "Paste your link here",
        "qs_paste" to "Paste",
        "qs_continue" to "Continue",
        "qs_back" to "Back",
        "qs_done_config" to "Config added and selected. Connecting…",
        "qs_done_sub" to "Subscription updated and a server was selected. Connecting…",
        "qs_invalid" to "That doesn’t look like a valid config or subscription URL. Check and try again.",
        "qs_empty" to "Please paste a link first.",
        "qs_help_color_title" to "Change color",
        "qs_help_color_body" to "In Settings → Appearance you can choose a ready accent color, or mix your own with Red / Green / Blue sliders.\n\nYour choice updates card frames, glows and buttons immediately. The VPN does not reconnect for color changes, so you can try several looks safely.\n\nTap Continue to open the Appearance section.",
        "qs_help_limits_title" to "Session limits",
        "qs_help_limits_body" to "Session limits control how long or how much data this device uses in one connection session. They are goals on this phone — not the traffic quota from your provider subscription.\n\n• Time limit — minutes until the goal is reached (0 = unlimited)\n• Traffic limit — megabytes of upload + download (0 = unlimited)\n\nAt about 80% and 95% you will see friendly warnings. At 100% you can choose:\n• Notify only — banner and notification; VPN stays connected\n• Disconnect VPN — notify and then stop the tunnel\n\nHome shows progress bars while you are connected.\n\nTap Continue to open Session limits.",
        "qs_help_language_title" to "Language",
        "qs_help_language_body" to "Choose English or Persian (فارسی) for the whole interface.\n\nPersian uses a right-to-left layout so lists, buttons and text fields align naturally for Persian readers.\n\nThe brand name BLA VPN and the version number are never translated, so support and store listings stay consistent.\n\nTap Continue to open Language settings.",
        "qs_help_settings_title" to "Full settings",
        "qs_help_settings_body" to "Settings cover everything advanced: routing mode, DNS, TLS Fragment (anti-DPI), session limits, keep-alive, appearance color, language, per-app proxy, and local SOCKS/HTTP ports.\n\nEach section has a Help (?) button with a full explanation. Edits stay in a draft until you tap Apply changes, so you can review before saving.\n\nIf the VPN is connected and you change something that affects the tunnel (for example ports or routing), the app will reconnect after Apply.\n\nTap Continue to open Settings.",
        "help_routing_title" to "Routing mode",
        "help_routing_body" to "Routing decides which traffic is sent through the VPN proxy and which goes out of the phone directly.\n\n• Global — apps use the tunnel by default. Private LAN IP ranges are still kept direct for safety.\n• Bypass LAN — private and local addresses stay direct. Recommended for home and office Wi‑Fi so printers and LAN hosts keep working.\n\nYou can also list specific domains under “Direct domains” so those hosts never use the proxy (wildcards supported).\n\nAfter you change routing, tap Apply. If the VPN is already on, it may reconnect so the new rules take effect.",
        "help_core_title" to "Core options",
        "help_core_body" to "These options control how the Xray core handles connections on this device.\n\n• Domain sniffing — looks at TLS/HTTP traffic to learn the real domain name, which improves routing accuracy when the destination is only an IP at first.\n• Mux — multiplexes several streams over fewer connections. Some servers work better with it; others are more stable with Mux off. Try both if you see odd latency.\n• Allow insecure TLS — skips certificate checks. Only for temporary testing; do not leave this on for daily use.\n• Auto connect on boot — after the phone restarts, if a server is already selected, the app can start the VPN automatically.\n• Keep-alive — while connected, the app sends a light request through the local proxy on the interval you set (default 1 minute) so idle provider links and NAT mappings are less likely to drop.\n• Allow LAN devices to use this proxy — listens on all interfaces so other phones/PCs on the same Wi‑Fi can point their SOCKS/HTTP client at this phone’s IP and ports.",
        "help_bypass_title" to "Direct domains",
        "help_bypass_body" to "List domain patterns that should connect directly, without the VPN proxy.\n\nWrite one pattern per line. Examples:\n• example.com — domain and its subdomains\n• *.example.com — wildcard form (converted to a regexp)\n• *.example.com* — broader wildcard match\n• *cdn* — any host containing “cdn”\n\nLines starting with # are ignored. After Apply + reconnect, matching traffic uses the direct outbound.",
        "help_limits_title" to "Session limits",
        "help_limits_body" to "Session limits are soft goals for this phone session. They are separate from the traffic and expiry bars that come from your provider’s subscription-userinfo header.\n\nSet a time limit in minutes and/or a traffic limit in megabytes (upload + download combined). Use 0 for unlimited.\n\nWhile you are connected, Home shows progress for these limits. Near 80% and 95% you get warnings. At 100%:\n• Notify only — a banner and system notification; the VPN stays up so nothing disconnects suddenly.\n• Disconnect VPN — you still get a notification, then the tunnel is stopped.\n\nPick the style that matches how strict you want the limit to feel.",
        "help_appearance_title" to "Appearance",
        "help_appearance_body" to "The app uses the Classic Pulse visual style. Here you only change the accent color that drives frames, glows and highlight buttons.\n\nUse a preset circle for a quick look, or open the custom color sliders (Red / Green / Blue) for a precise color.\n\nAppearance changes apply immediately and never force a VPN reconnect, so you can tune the look while you stay online.",
        "help_language_title" to "Language",
        "help_language_body" to "Switch the entire interface between English and Persian (فارسی).\n\nWhen Persian is selected, the layout becomes right-to-left: navigation, lists and most text fields follow that direction. Technical terms such as VPN, TLS, DNS, Mux, SOCKS, VLESS and similar stay in English so they remain searchable and familiar.\n\nThe product name BLA VPN and the version string are always shown as-is.",
        "help_perapp_title" to "Per-app proxy",
        "help_perapp_body" to "Per-app proxy lets you decide which applications go through the VPN and which bypass it.\n\nOpen Configure apps to pick packages. Depending on mode:\n• proxy — only the listed apps use the tunnel; others go direct\n• bypass — the listed apps go direct; everything else uses the VPN\n\nThis is useful when only a browser or messaging app needs the tunnel, or when a local app must avoid the VPN. Changes in the per-app screen are saved immediately; you may need to reconnect the VPN for system routing to fully refresh.",
        "help_fragment_title" to "TLS Fragment",
        "help_fragment_body" to "TLS Fragment splits early TLS/TCP data (often the ClientHello) into smaller pieces with short delays. Many restrictive networks use DPI that looks for a complete ClientHello in one packet; fragmenting can make that harder to detect.\n\nThis app uses a v2rayN-style freedom dialerProxy fragment chain.\n\n• Packets — tlshello fragments the TLS handshake; values like 1-2 / 1-3 affect the first few TCP packets\n• Length — size range of each fragment in bytes (default 12-23)\n• Interval — delay between fragments in milliseconds (default 1-2)\n• Max split — optional upper bound; leave empty unless you know you need it\n\nWrong values can slow you down or break sites. After editing, tap Apply and reconnect, then test on the network where you need the bypass.",
        "help_ports_title" to "Local ports",
        "help_ports_body" to "Xray opens local listeners on this device:\n\n• SOCKS port — default 10808; the TUN bridge (hev-socks5-tunnel) sends traffic here\n• HTTP port — default 10809; used for HTTP proxy access and keep-alive pings\n• DNS remote — DoH or DNS endpoint used in the generated config (for example https://1.1.1.1/dns-query)\n\nChange a port only if another app already occupies the default. Ports must be between 1 and 65535, and SOCKS and HTTP should not be the same number.\n\nAfter Apply, if the VPN is connected, the tunnel reconnects so the new ports are used.",
        "help_about_title" to "About",
        "help_about_body" to "This section shows the BLA VPN app version and the linked Xray core version string from the native library.\n\nWhen you report a problem, include both numbers, your Android version, and whether the issue happens with a single vless:// config or a full subscription URL. That information makes support much faster."
    )

    /**
     * Fluent Persian UI. Technical terms kept in English:
     * VPN, VLESS, TLS, DNS, Mux, SOCKS, HTTP, DPI, LAN, geoip, geosite, fragment, ClientHello, etc.
     */
    val fa: Map<String, String> = linkedMapOf(
        "home" to "خانه",
        "servers" to "سرورها",
        "import" to "ورود",
        "settings" to "تنظیمات",
        "subscriptions" to "اشتراک‌ها",
        "quick_setup" to "راه‌اندازی سریع",
        "choose_server" to "انتخاب سرور",
        "no_server" to "سروری انتخاب نشده است",
        "tap_connect" to "برای اتصال لمس کنید",
        "tap_disconnect" to "برای قطع اتصال لمس کنید",
        "protected" to "محافظت‌شده",
        "disconnected" to "قطع‌شده",
        "connecting" to "در حال اتصال…",
        "session_time" to "زمان نشست",
        "session_traffic" to "ترافیک نشست",
        "unlimited" to "نامحدود",
        "no_data" to "بدون داده",
        "limit_almost_up" to "نزدیک به پایان محدودیت",
        "high_usage" to "مصرف بالا",
        "establishing_tunnel" to "در حال برقراری تونل…",
        "connected_no_traffic" to "متصل است اما ترافیکی نیست — سرور را بررسی کنید",
        "not_tested" to "تست‌نشده",
        "quick_test" to "تست سریع کانفیگ فعال",
        "subscription" to "اشتراک",
        "traffic" to "ترافیک",
        "expire" to "انقضا",
        "used" to "مصرف‌شده",
        "apply_changes" to "اعمال تغییرات",
        "discard" to "لغو تغییرات",
        "ok" to "باشه",
        "cancel" to "انصراف",
        "back" to "بازگشت",
        "continue_btn" to "ادامه",
        "paste" to "چسباندن",
        "delete" to "حذف",
        "delete_all" to "حذف همه",
        "delete_selected" to "حذف انتخاب‌شده‌ها",
        "delete_invalid" to "حذف نامعتبرها",
        "select_all" to "انتخاب همه",
        "multi_select" to "انتخاب چندتایی",
        "edit" to "ویرایش",
        "refresh" to "به‌روزرسانی",
        "test_all" to "تست همه",
        "sort_by_delay" to "مرتب‌سازی بر اساس تأخیر",
        "search_servers" to "جستجوی سرور…",
        "no_servers_yet" to "هنوز سروری اضافه نشده",
        "import_hint_empty" to "یک لینک اشتراک یا کانفیگ وارد کنید",
        "select_servers" to "انتخاب سرورها",
        "selected_count" to "انتخاب‌شده",
        "configurations" to "کانفیگ",
        "testing_connections" to "در حال تست اتصال…",
        "active" to "فعال",
        "help" to "راهنما",
        "got_it" to "متوجه شدم",
        "language" to "زبان",
        "english" to "English",
        "persian" to "فارسی",
        "ui_language_hint" to "زبان رابط کاربری را انتخاب کنید. فارسی راست‌به‌چپ است. نام اپ و نسخه ترجمه نمی‌شوند.",
        "appearance" to "ظاهر",
        "theme_frame_color" to "رنگ تم و قاب",
        "custom_color" to "رنگ سفارشی",
        "custom_color_hint" to "رنگ دلخواه برای قاب‌ها، درخشش و رنگ‌های تأکیدی",
        "session_limits" to "محدودیت نشست",
        "routing" to "حالت مسیریابی",
        "routing_mode" to "حالت مسیریابی",
        "core_options" to "گزینه‌های هسته",
        "per_app_proxy" to "پروکسی هر اپ",
        "tls_fragment" to "TLS Fragment (ضد DPI)",
        "local_ports" to "پورت‌های محلی",
        "about" to "درباره",
        "unsaved_changes" to "تغییرات ذخیره‌نشده — برای ذخیره «اعمال تغییرات» را بزنید",
        "settings_subtitle" to "مسیریابی، DNS، TLS Fragment و گزینه‌های پیشرفته",
        "limit_action" to "وقتی محدودیت پر شد",
        "limit_action_notify" to "فقط اعلان (VPN بماند)",
        "limit_action_disconnect" to "قطع VPN",
        "limit_action_hint" to "انتخاب کنید فقط هشدار داده شود یا VPN هم قطع شود وقتی زمان یا حجم به ۱۰۰٪ رسید. نوار پیشرفت در خانه از این مقادیر استفاده می‌کند. ۰ = نامحدود.",
        "domain_sniffing" to "Domain sniffing",
        "mux" to "Mux",
        "allow_insecure" to "اجازه TLS ناامن",
        "auto_connect" to "اتصال خودکار پس از روشن شدن دستگاه",
        "keep_alive" to "Keep-alive",
        "keep_alive_hint" to "وقتی VPN وصل است، در بازهٔ زمانی که تنظیم می‌کنید یک درخواست سبک از مسیر پروکسی محلی فرستاده می‌شود تا تونل‌های بی‌کار و NAT قطع نشوند. پیش‌فرض روشن است.",
        "keep_alive_interval" to "بازه (دقیقه)",
        "keep_alive_interval_hint" to "هر چند دقیقه یک‌بار پینگ شود (۱ تا ۱۲۰). پیش‌فرض ۱ دقیقه است.",
        "allow_lan_proxy" to "اجازه به دستگاه‌های LAN برای استفاده از این پروکسی",
        "allow_lan_proxy_hint" to "پورت‌های SOCKS و HTTP روی همهٔ رابط‌های شبکه باز می‌شوند تا گوشی/لپ‌تاپ دیگر روی همان Wi‑Fi بتواند از این گوشی به‌عنوان پروکسی استفاده کند (IP وای‌فای این گوشی + پورت‌های زیر). اگر فقط خود این گوشی به VPN نیاز دارد خاموش بگذارید.",
        "bypass_domains" to "دامنه‌های مستقیم (bypass VPN)",
        "bypass_domains_hint" to "هر خط یک الگو. میزبان‌های مطابق مستقیم می‌روند (بدون پروکسی).\nمثال: example.com · *.example.com · *.example.com* · *cdn*\nوایلدکارد (*) پشتیبانی می‌شود. خط‌هایی که با # شروع شوند توضیح‌اند.",
        "ports_edit_hint" to "پورت‌های محلی SOCKS و HTTP که Xray روی همین دستگاه گوش می‌دهد. فقط اگر اپ دیگری پورت پیش‌فرض را گرفته تغییر دهید.",
        "ports_restart_hint" to "تغییر پورت یا DNS نیاز به «اعمال» دارد. اگر VPN روشن باشد دوباره وصل می‌شود.",
        "rename_subscription" to "تغییر نام اشتراک",
        "time_limit" to "محدودیت زمان",
        "traffic_limit" to "محدودیت ترافیک",
        "global_proxy" to "پروکسی سراسری",
        "bypass_lan" to "دور زدن LAN",
        "configure_apps" to "پیکربندی اپ‌ها",
        "per_app_on" to "روشن · {n} اپ · mode={mode}",
        "per_app_off" to "خاموش — لمس کنید تا مشخص شود کدام اپ‌ها از VPN استفاده کنند",
        "per_app_saved" to "(در صفحهٔ هر اپ فوراً ذخیره می‌شود)",
        "scan_qr" to "اسکن QR",
        "manual_vless" to "VLESS دستی",
        "manage_subscriptions" to "مدیریت اشتراک‌ها",
        "import_links" to "لینک‌ها",
        "import_links_hint" to "vless:// · vmess:// · trojan:// · ss:// · https:// اشتراک · چندخطی",
        "import_btn" to "ورود",
        "name_subscription" to "نام‌گذاری اشتراک",
        "name" to "نام",
        "import_all" to "ورود همه",
        "sources" to "منبع",
        "no_subscriptions" to "اشتراکی نیست. از بخش ورود یکی اضافه کنید.",
        "servers_title" to "سرورها",
        "never" to "هرگز",
        "tab_all" to "همه",
        "tab_manual" to "دستی",
        "delete_all_title" to "همه کانفیگ‌ها حذف شوند؟",
        "delete_all_body" to "همه سرورها برای همیشه حذف می‌شوند.",
        "delete_selected_title" to "موارد انتخاب‌شده حذف شوند؟",
        "delete_selected_body" to "{n} سرور حذف شود؟ این کار قابل بازگشت نیست.",
        "enable_fragment" to "فعال‌سازی fragment",
        "fragment_desc" to "بسته‌های TLS ClientHello / TCP را برای دور زدن DPI خرد می‌کند. از زنجیره freedom dialerProxy (سبک v2rayN) استفاده می‌شود.",
        "packets_type" to "نوع Packets",
        "packets_hint" to "tlshello = خرد کردن TLS handshake · 1-x = N بستهٔ اول TCP",
        "length_range" to "Length (بازه بایت)",
        "custom_length" to "Length سفارشی",
        "interval_range" to "Interval / تأخیر (بازه ms)",
        "custom_interval" to "Interval سفارشی",
        "max_split" to "Max split (اختیاری)",
        "custom_max_split" to "maxSplit سفارشی",
        "custom_minutes" to "دقیقهٔ سفارشی (۰ = خاموش)",
        "custom_mb" to "MB سفارشی (۰ = خاموش)",
        "qs_title" to "راه‌اندازی سریع",
        "qs_what" to "می‌خواهید چه کاری انجام دهید؟",
        "qs_import" to "می‌خواهم یک کانفیگ وارد کنم",
        "qs_color" to "می‌خواهم رنگ را عوض کنم",
        "qs_limits" to "می‌خواهم محدودیت زمان یا حجم بگذارم",
        "qs_language" to "می‌خواهم زبان را عوض کنم",
        "qs_settings" to "باز کردن همه تنظیمات",
        "qs_have_config" to "آیا کانفیگ یا لینک اشتراک دارید؟",
        "qs_yes" to "بله",
        "qs_no" to "خیر",
        "qs_no_config_msg" to "اشکالی ندارد! یک کانفیگ معتبر از ارائه‌دهندهٔ مطمئن بگیرید یا اشتراک بخرید. وقتی لینک vless:// یا آدرس https:// اشتراک داشتید، دوباره راه‌اندازی سریع را باز کنید.",
        "qs_copied_hint" to "ابتدا لینک را از سایت یا اپ ارائه‌دهنده کپی کنید.",
        "qs_have_copied" to "آیا کانفیگ یا لینک اشتراک را کپی کرده‌اید؟",
        "qs_paste_title" to "لینک را اینجا بچسبانید",
        "qs_paste" to "چسباندن",
        "qs_continue" to "ادامه",
        "qs_back" to "بازگشت",
        "qs_done_config" to "کانفیگ اضافه و انتخاب شد. در حال اتصال…",
        "qs_done_sub" to "اشتراک به‌روز شد و یک سرور انتخاب شد. در حال اتصال…",
        "qs_invalid" to "این متن شبیه کانفیگ یا لینک اشتراک معتبر نیست. دوباره بررسی کنید.",
        "qs_empty" to "لطفاً ابتدا یک لینک بچسبانید.",
        "qs_help_color_title" to "تغییر رنگ",
        "qs_help_color_body" to "در مسیر تنظیمات ← ظاهر می‌توانید یکی از رنگ‌های آماده را انتخاب کنید، یا با اسلایدرهای Red / Green / Blue رنگ کاملاً دلخواه بسازید.\n\nاین تغییر بلافاصله روی قاب کارت‌ها، درخشش‌ها و دکمه‌های تأکیدی اعمال می‌شود و برای عوض کردن رنگ نیازی به قطع VPN نیست؛ می‌توانید چند ظاهر را با خیال راحت امتحان کنید.\n\nروی «ادامه» بزنید تا بخش ظاهر باز شود.",
        "qs_help_limits_title" to "محدودیت نشست",
        "qs_help_limits_body" to "محدودیت نشست مشخص می‌کند این گوشی در یک اتصال چقدر زمان یا حجم مصرف کند. این‌ها اهداف محلی روی دستگاه شما هستند و با سهمیهٔ ترافیک یا تاریخ انقضای اشتراک ارائه‌دهنده فرق دارند.\n\n• محدودیت زمان — تعداد دقیقه تا رسیدن به هدف (۰ یعنی نامحدود)\n• محدودیت ترافیک — مگابایت آپلود به‌علاوه دانلود (۰ یعنی نامحدود)\n\nحدود ۸۰٪ و ۹۵٪ هشدار دوستانه می‌بینید. در ۱۰۰٪ می‌توانید انتخاب کنید:\n• فقط اعلان — بنر و اعلان؛ VPN وصل می‌ماند\n• قطع VPN — اعلان و سپس قطع تونل\n\nوقتی متصل هستید، صفحهٔ خانه نوار پیشرفت این محدودیت‌ها را نشان می‌دهد.\n\nروی «ادامه» بزنید تا بخش محدودیت نشست باز شود.",
        "qs_help_language_title" to "زبان",
        "qs_help_language_body" to "برای کل رابط کاربری می‌توانید English یا فارسی را انتخاب کنید.\n\nدر حالت فارسی چیدمان راست‌به‌چپ می‌شود تا فهرست‌ها، دکمه‌ها و بیشتر فیلدهای متنی برای خواندن فارسی طبیعی باشند.\n\nنام تجاری BLA VPN و شمارهٔ نسخه هرگز ترجمه نمی‌شوند تا در پشتیبانی و فروشگاه یکسان بمانند.\n\nروی «ادامه» بزنید تا تنظیمات زبان باز شود.",
        "qs_help_settings_title" to "همه تنظیمات",
        "qs_help_settings_body" to "صفحهٔ تنظیمات همهٔ بخش‌های پیشرفته را یک‌جا دارد: حالت مسیریابی، DNS، TLS Fragment (ضد DPI)، محدودیت نشست، Keep-alive، رنگ ظاهر، زبان، پروکسی هر اپ، و پورت‌های محلی SOCKS/HTTP.\n\nکنار عنوان هر بخش دکمهٔ راهنما (?) هست که توضیح کامل همان بخش را در یک صفحهٔ اسکرول‌شونده نشان می‌دهد. ویرایش‌ها تا وقتی «اعمال تغییرات» را نزنید فقط پیش‌نویس هستند.\n\nاگر VPN وصل باشد و چیزی که روی تونل اثر دارد (مثل پورت یا مسیریابی) را عوض کنید، بعد از اعمال ممکن است اتصال دوباره برقرار شود.\n\nروی «ادامه» بزنید تا تنظیمات باز شود.",
        "help_routing_title" to "حالت مسیریابی",
        "help_routing_body" to "مسیریابی مشخص می‌کند کدام ترافیک از پروکسی VPN رد شود و کدام مستقیم از گوشی خارج شود.\n\n• Global — اپ‌ها به‌طور پیش‌فرض از تونل استفاده می‌کنند؛ بازه‌های IP خصوصی LAN برای ایمنی مستقیم می‌مانند.\n• Bypass LAN — آدرس‌های خصوصی و محلی مستقیم می‌مانند. برای Wi‑Fi خانه و محل کار پیشنهاد می‌شود تا پرینتر و سرویس‌های LAN کار کنند.\n\nهمچنین در «دامنه‌های مستقیم» می‌توانید میزبان‌های خاص را بنویسید تا از پروکسی رد نشوند (وایلدکارد پشتیبانی می‌شود).\n\nبعد از تغییر، «اعمال» را بزنید. اگر VPN از قبل روشن باشد ممکن است برای اعمال قوانین جدید دوباره وصل شود.",
        "help_core_title" to "گزینه‌های هسته",
        "help_core_body" to "این گزینه‌ها رفتار هستهٔ Xray را روی همین دستگاه کنترل می‌کنند.\n\n• Domain sniffing — از ترافیک TLS/HTTP دامنهٔ واقعی را تشخیص می‌دهد تا مسیریابی وقتی اول فقط IP دیده می‌شود دقیق‌تر باشد.\n• Mux — چند جریان را روی اتصال‌های کمتر می‌فرستد. بعضی سرورها با Mux پایدارترند و بعضی بدون آن. اگر تأخیر عجیب دیدید هر دو حالت را امتحان کنید.\n• Allow insecure TLS — بررسی گواهی را رد می‌کند. فقط برای تست موقت؛ برای استفادهٔ روزمره روشن نگذارید.\n• Auto connect on boot — بعد از ریستارت گوشی، اگر سروری از قبل انتخاب شده باشد، اپ می‌تواند VPN را خودکار شروع کند.\n• Keep-alive — وقتی وصل هستید، در بازهٔ دقیقه‌ای که تنظیم کرده‌اید (پیش‌فرض ۱ دقیقه) یک درخواست سبک از مسیر پروکسی محلی فرستاده می‌شود تا لینک‌های بی‌کار و NAT کمتر قطع شوند.\n• اجازه به دستگاه‌های LAN — SOCKS/HTTP روی همهٔ رابط‌ها باز می‌شود تا دستگاه‌های همان Wi‑Fi از این گوشی به‌عنوان پروکسی استفاده کنند.",
        "help_bypass_title" to "دامنه‌های مستقیم",
        "help_bypass_body" to "الگوهای دامنه را خط‌به‌خط بنویسید تا بدون پروکسی VPN مستقیم وصل شوند.\n\nمثال‌ها:\n• example.com — دامنه و زیردامنه‌ها\n• *.example.com — با وایلدکارد\n• *.example.com* — تطبیق وسیع‌تر\n• *cdn* — هر میزبانی که cdn داشته باشد\n\nخط‌هایی که با # شروع شوند نادیده گرفته می‌شوند. بعد از اعمال و reconnect، ترافیک مطابق از outbound مستقیم می‌رود.",
        "help_limits_title" to "محدودیت نشست",
        "help_limits_body" to "محدودیت نشست اهداف نرم برای همین جلسه روی گوشی است و با نوار ترافیک/انقضای اشتراک ارائه‌دهنده (هدر subscription-userinfo) یکی نیست.\n\nمی‌توانید محدودیت زمان به دقیقه و/یا محدودیت ترافیک به مگابایت (جمع آپلود و دانلود) بگذارید. مقدار ۰ یعنی نامحدود.\n\nدر حالت اتصال، خانه پیشرفت این محدودیت‌ها را نشان می‌دهد. نزدیک ۸۰٪ و ۹۵٪ هشدار می‌گیرید. در ۱۰۰٪:\n• فقط اعلان — بنر و اعلان سیستمی؛ VPN وصل می‌ماند تا ناگهان قطع نشود.\n• قطع VPN — باز هم اعلان می‌آید، سپس تونل متوقف می‌شود.\n\nسبکی را انتخاب کنید که با سخت‌گیری مورد نظرتان جور باشد.",
        "help_appearance_title" to "ظاهر",
        "help_appearance_body" to "ظاهر برنامه روی تم Classic Pulse است. در این بخش فقط رنگ تأکیدی را عوض می‌کنید که قاب‌ها، درخشش‌ها و دکمه‌های برجسته را رنگ می‌کند.\n\nبا دایره‌های آماده سریع یک رنگ انتخاب کنید، یا با اسلایدرهای Red / Green / Blue رنگ دقیق بسازید.\n\nتغییر ظاهر فوری است و هرگز VPN را مجبور به قطع و وصل دوباره نمی‌کند؛ می‌توانید در حالت آنلاین ظاهر را تنظیم کنید.",
        "help_language_title" to "زبان",
        "help_language_body" to "کل رابط را بین English و فارسی جابه‌جا کنید.\n\nبا انتخاب فارسی، چیدمان راست‌به‌چپ می‌شود: ناوبری، فهرست‌ها و بیشتر فیلدهای متنی با جهت خواندن فارسی هم‌خوان می‌شوند. اصطلاحات فنی مثل VPN، TLS، DNS، Mux، SOCKS، VLESS و مشابه آن‌ها به انگلیسی می‌مانند تا جست‌وجوپذیر و آشنا بمانند.\n\nنام محصول BLA VPN و رشتهٔ نسخه همیشه بدون ترجمه نمایش داده می‌شوند.",
        "help_perapp_title" to "پروکسی هر اپ",
        "help_perapp_body" to "با پروکسی هر اپ مشخص می‌کنید کدام برنامه‌ها از VPN رد شوند و کدام مستقیم بروند.\n\n«پیکربندی اپ‌ها» را باز کنید و بسته‌ها را انتخاب کنید. بسته به حالت:\n• proxy — فقط اپ‌های فهرست‌شده از تونل استفاده می‌کنند؛ بقیه مستقیم‌اند\n• bypass — اپ‌های فهرست‌شده مستقیم‌اند؛ بقیه از VPN می‌روند\n\nوقتی فقط مرورگر یا پیام‌رسان به تونل نیاز دارد، یا یک اپ محلی نباید از VPN رد شود، این بخش کاربردی است. تغییرات در صفحهٔ هر اپ فوراً ذخیره می‌شود؛ برای تازه‌سازی کامل مسیریابی سیستم ممکن است لازم باشد VPN را یک‌بار قطع و وصل کنید.",
        "help_fragment_title" to "TLS Fragment",
        "help_fragment_body" to "TLS Fragment دادهٔ اولیهٔ TLS/TCP (اغلب ClientHello) را به تکه‌های کوچک‌تر با تأخیر کوتاه می‌شکند. بسیاری از شبکه‌های محدودکننده با DPI به‌دنبال ClientHello کامل در یک بسته می‌گردند؛ خرد کردن می‌تواند تشخیص را سخت‌تر کند.\n\nاین اپ از زنجیرهٔ fragment به سبک v2rayN با freedom dialerProxy استفاده می‌کند.\n\n• Packets — tlshello دست‌دهی TLS را خرد می‌کند؛ مقادیری مثل 1-2 یا 1-3 روی چند بستهٔ اول TCP اثر می‌گذارند\n• Length — بازهٔ اندازهٔ هر تکه به بایت (پیش‌فرض 12-23)\n• Interval — تأخیر بین تکه‌ها به میلی‌ثانیه (پیش‌فرض 1-2)\n• Max split — سقف اختیاری؛ اگر لازم ندارید خالی بگذارید\n\nمقادیر نادرست ممکن است سایت‌ها را کند یا قطع کند. بعد از ویرایش «اعمال» کنید، دوباره وصل شوید و روی همان شبکه‌ای که به عبور نیاز دارید تست بگیرید.",
        "help_ports_title" to "پورت‌های محلی",
        "help_ports_body" to "Xray روی همین دستگاه چند listener محلی باز می‌کند:\n\n• پورت SOCKS — پیش‌فرض 10808؛ پل TUN (hev-socks5-tunnel) ترافیک را به اینجا می‌فرستد\n• پورت HTTP — پیش‌فرض 10809؛ برای دسترسی HTTP proxy و پینگ‌های Keep-alive\n• DNS remote — نقطهٔ DoH یا DNS در کانفیگ تولیدشده (مثلاً https://1.1.1.1/dns-query)\n\nپورت را فقط وقتی عوض کنید که اپ دیگری پیش‌فرض را گرفته باشد. بازه باید بین 1 و 65535 باشد و SOCKS و HTTP نباید یک عدد باشند.\n\nبعد از «اعمال»، اگر VPN وصل باشد تونل دوباره برقرار می‌شود تا پورت‌های جدید استفاده شوند.",
        "help_about_title" to "درباره",
        "help_about_body" to "این بخش نسخهٔ اپ BLA VPN و رشتهٔ نسخهٔ هستهٔ Xray را از کتابخانهٔ native نشان می‌دهد.\n\nوقتی مشکلی گزارش می‌کنید، هر دو عدد، نسخهٔ Android، و اینکه مشکل با یک لینک تکی vless:// است یا با URL کامل اشتراک را بنویسید. این اطلاعات پشتیبانی را خیلی سریع‌تر می‌کند."
    )

    fun base(lang: String): Map<String, String> = when (lang.lowercase()) {
        "fa", "fa-ir", "persian", "farsi" -> fa
        else -> en
    }

    fun customFile(context: Context): File =
        File(context.filesDir, "i18n_custom.json")

    fun loadCustom(context: Context): Map<String, String> {
        val f = customFile(context)
        if (!f.exists()) return emptyMap()
        return parseJson(f.readText()) ?: emptyMap()
    }

    fun saveCustom(context: Context, map: Map<String, String>) {
        customFile(context).writeText(prettyGson.toJson(map))
    }

    fun resolve(context: Context, lang: String): Map<String, String> {
        val base = base(lang).toMutableMap()
        base.putAll(loadCustom(context))
        return base
    }

    fun exportTemplateJson(): String = prettyGson.toJson(en)

    fun parseJson(json: String): Map<String, String>? {
        if (json.isBlank()) return null
        var raw = json.trim()
            .removePrefix("\uFEFF")
            .replace('\u201C', '"')
            .replace('\u201D', '"')
            .replace('\u2018', '\'')
            .replace('\u2019', '\'')
        if (raw.startsWith("```")) {
            raw = raw.removePrefix("```json").removePrefix("```JSON").removePrefix("```").trim()
            if (raw.endsWith("```")) raw = raw.removeSuffix("```").trim()
        }
        return try {
            val element = JsonParser.parseString(raw)
            if (!element.isJsonObject) return null
            val out = linkedMapOf<String, String>()
            for ((k, v) in element.asJsonObject.entrySet()) {
                val value = when {
                    v.isJsonNull -> ""
                    v.isJsonPrimitive -> {
                        val p = v.asJsonPrimitive
                        when {
                            p.isString -> p.asString
                            p.isNumber -> p.asString
                            p.isBoolean -> p.asBoolean.toString()
                            else -> p.toString()
                        }
                    }
                    else -> v.toString()
                }
                out[k] = value
            }
            if (out.isEmpty()) null else out
        } catch (_: Exception) {
            try {
                val type = object : TypeToken<Map<String, Any?>>() {}.type
                val anyMap: Map<String, Any?> = gson.fromJson(raw, type) ?: return null
                anyMap.mapValues { (_, v) -> v?.toString().orEmpty() }
                    .filterKeys { it.isNotBlank() }
                    .ifEmpty { null }
            } catch (_: Exception) {
                null
            }
        }
    }
}

val LocalStrings = staticCompositionLocalOf { AppStrings.en }

@Composable
@ReadOnlyComposable
fun t(key: String): String = LocalStrings.current[key] ?: AppStrings.en[key] ?: key
