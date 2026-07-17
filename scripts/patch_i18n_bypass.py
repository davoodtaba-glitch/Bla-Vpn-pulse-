# -*- coding: utf-8 -*-
from pathlib import Path

p = Path(r"G:\grok\app3\app\src\main\java\com\xraypulse\app\ui\i18n\AppStrings.kt")
t = p.read_text(encoding="utf-8")

def ensure_before(marker: str, block: str, label: str):
    global t
    if "help_bypass_title" in t and label in t[t.find(marker)-200:t.find(marker)+20]:
        # crude: if help_bypass already near marker skip
        pass
    if f'"help_bypass_title" to "{label}"' in t:
        print(f"already has help_bypass for {label}")
        return
    i = t.find(marker)
    if i < 0:
        print(f"marker missing: {marker}")
        return
    t = t[:i] + block + t[i:]
    print(f"inserted before {marker}")

en_block = '''        "help_bypass_title" to "Direct domains",
        "help_bypass_body" to "List domain patterns that should connect directly, without the VPN proxy.\\n\\nWrite one pattern per line. Examples:\\n• example.com — domain and its subdomains\\n• *.example.com — wildcard form\\n• *.example.com* — broader wildcard match\\n• *cdn* — any host containing cdn\\n\\nLines starting with # are ignored. After Apply + reconnect, matching traffic uses the direct outbound.",
'''
fa_block = '''        "help_bypass_title" to "دامنه‌های مستقیم",
        "help_bypass_body" to "الگوهای دامنه را خط‌به‌خط بنویسید تا بدون پروکسی VPN مستقیم وصل شوند.\\n\\nمثال‌ها:\\n• example.com — دامنه و زیردامنه‌ها\\n• *.example.com — با وایلدکارد\\n• *.example.com* — تطبیق وسیع‌تر\\n• *cdn* — هر میزبانی که cdn داشته باشد\\n\\nخط‌هایی که با # شروع شوند نادیده گرفته می‌شوند. بعد از اعمال و reconnect، ترافیک مطابق از outbound مستقیم می‌رود.",
'''

# Insert EN help_bypass once before first help_limits_title (EN section)
en_marker = '"help_limits_title" to "Session limits"'
if '"help_bypass_title" to "Direct domains"' not in t:
    i = t.find(en_marker)
    if i > 0:
        t = t[:i] + en_block + t[i:]
        print("EN help_bypass inserted")

# Insert FA help_bypass before FA help_limits
fa_marker = '"help_limits_title" to "محدودیت نشست"'
if '"help_bypass_title" to "دامنه‌های مستقیم"' not in t:
    i = t.find(fa_marker)
    if i > 0:
        t = t[:i] + fa_block + t[i:]
        print("FA help_bypass inserted")

# Soften help_routing if still mentions Bypass China
t = t.replace(
    "• Bypass China — Chinese destinations can go direct via geoip/geosite rules while other traffic uses the proxy. Useful if your server is outside China.\n• Custom — advanced rule set for power users.\n\n",
    "You can also list specific domains under Direct domains so those hosts never use the proxy (wildcards supported).\n\n",
)
t = t.replace(
    "• Bypass China — مقصدهای چینی می‌توانند با قوانین geoip/geosite مستقیم بروند و بقیه از پروکسی. وقتی سرور شما بیرون چین است مفید است.\n• Custom — مجموعه قوانین پیشرفته برای کاربران حرفه‌ای.\n\n",
    "همچنین می‌توانید در «دامنه‌های مستقیم» میزبان‌های خاص را بنویسید تا از پروکسی رد نشوند (وایلدکارد پشتیبانی می‌شود).\n\n",
)

# Update keep-alive wording in FA help_core if still says 30 min only
t = t.replace(
    "• Keep-alive (هر ۳۰ دقیقه) — وقتی وصل هستید، هر نیم‌ساعت یک درخواست سبک از مسیر پروکسی محلی فرستاده می‌شود تا لینک‌های بی‌کار ارائه‌دهنده و نگاشت‌های NAT کمتر قطع شوند.",
    "• Keep-alive — وقتی وصل هستید، در بازهٔ دقیقه‌ای که تنظیم کرده‌اید (پیش‌فرض ۱ دقیقه) یک درخواست سبک از مسیر پروکسی محلی فرستاده می‌شود تا لینک‌های بی‌کار و NAT کمتر قطع شوند.\n• اجازه به دستگاه‌های LAN — SOCKS/HTTP روی همهٔ رابط‌ها باز می‌شود تا دستگاه‌های همان Wi‑Fi از این گوشی به‌عنوان پروکسی استفاده کنند.",
)

p.write_text(t, encoding="utf-8")
print("ok")
