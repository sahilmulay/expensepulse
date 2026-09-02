# ExpensePulse 💸
> Minimalist UPI Expense Manager with Apple Dynamic Island Liquid Popup, Statement Importer, and Gesture Triggers.

[![Deploy with Vercel](https://vercel.com/button)](https://vercel.com/new)

---

## 🌟 Key Features

1. **Camera Cutout Dynamic Island:**
   * Clean screen at rest — zero persistent floating elements.
   * On trigger (shake, back-tap, or button), the black OLED pill organically stretches down from the camera hole with 420ms liquid spring physics.
2. **Gesture Triggers:**
   * **Phone Shake:** Double-shake acceleration vector trigger ($>2.7g$).
   * **iPhone Back Tap:** Hardware $Z$-axis accelerometer impulse detector.
3. **Smart Statements & UPI Importer:**
   * On-device client-side PDF parsing using Mozilla PDF.js.
   * Auto-detects Debit Outflows (`Paid to`), Incoming Credits (`Received from`), and automatically excludes Self-Transfers.
4. **Multi-Bank Account Management:**
   * Configure multiple banks (SBI, IPPB, HDFC, Axis, Cash).
   * Dynamically synced to popup selector and dashboard filter tabs.
5. **Monthly Budget & Money Left Tracker:**
   * Live countdown card showing remaining funds, percentage consumed, and over-budget alerts.
6. **Progressive Web App (PWA):**
   * Offline caching via Service Worker.
   * 1-tap "Install to Home Screen" on both Android and iPhone.

---

## 🚀 Deploy to Vercel

1. Push this repository to GitHub.
2. Go to [vercel.com/new](https://vercel.com/new) and import this repository.
3. Deploy! (Root directory works out of the box).
