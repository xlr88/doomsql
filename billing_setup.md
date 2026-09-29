# Google Play Billing & Developer Tips Setup Guide

DoomSQL integrates official **Google Play In-App Billing (Billing Library 7.x)** for receiving tips and donations from users.

This is the **only legal way** to accept tips or digital support inside an Android app distributed via Google Play. Direct UPI links, QR codes, or third-party payment gateways (like Razorpay, Stripe, or Paytm direct) inside a Google Play app for digital donations violate Google Play's **Payments Policy** and can lead to app rejection or account suspension.

With Google Play Billing:
- Users in India can pay seamlessly using **UPI (Google Pay, PhonePe, Paytm, BHIM, CRED)**, Credit/Debit Cards, Net Banking, or Google Play Balance directly in the official Google Play bottom sheet.
- Google manages tax compliance, invoicing, and refunds.
- Google deposits the collected earnings directly into your bank account.

---

## 1. Setting Up Your Google Play Merchant Account (Payments Profile)

To receive real money into your bank account:

1. Open the [Google Play Console](https://play.google.com/console).
2. In the left-hand menu, navigate to **Settings > Developer account > Payment settings** (or **Monetize > Financial reports**).
3. Click **Set up a merchant account** (or link an existing Google Payments Merchant Profile).
4. Fill in your business / individual details:
   - **Country:** India (or your country).
   - **Business type:** Individual / Sole Proprietorship or Organization.
   - **PAN & Address:** Enter your tax details.
5. Add your **Bank Account Details**:
   - Account Holder Name (must match your ID/PAN).
   - Bank Account Number.
   - IFSC Code.
6. Google will send a small test deposit (a few paise) to verify your bank account. Once received, enter the deposit amount in the Console to verify.

---

## 2. Creating the In-App Products (Tips) in Play Console

DoomSQL has been pre-configured with 4 consumable in-app products. In Google Play Console:

1. In Play Console, select your **DoomSQL** application.
2. In the left navigation, scroll down to **Monetize** and click **In-app products** (under *Products*).
3. Click **Create product** for each of the following 4 products:

### Product 1: Small Tip
- **Product ID:** `tip_small` *(Must match exactly)*
- **Product name:** Chai & Samosa Tip
- **Description:** Fuel 1 hour of SQL question design
- **Status:** Active
- **Price:** `₹29.00` (or approx. `$0.99` in other countries — Google auto-converts currencies)
- Click **Save**, then click **Activate**.

### Product 2: Medium Tip
- **Product ID:** `tip_medium` *(Must match exactly)*
- **Product name:** Coffee & Cookie Tip
- **Description:** Help maintain sandbox engine & assets
- **Status:** Active
- **Price:** `₹79.00` (or approx. `$1.99`)
- Click **Save**, then click **Activate**.

### Product 3: Large Tip
- **Product ID:** `tip_large` *(Must match exactly)*
- **Product name:** Developer Pizza Tip
- **Description:** Support offline database tools & new question sets
- **Status:** Active
- **Price:** `₹199.00` (or approx. `$3.99` / `$4.99`)
- Click **Save**, then click **Activate**.

### Product 4: Hero Tip
- **Product ID:** `tip_hero` *(Must match exactly)*
- **Product name:** Hero Sponsor
- **Description:** VIP Patron badge + eternal gratitude
- **Status:** Active
- **Price:** `₹499.00` (or approx. `$9.99`)
- Click **Save**, then click **Activate**.

> **Note:** All 4 products are configured as **Consumable** in DoomSQL's code. This means once a user buys a tip, the purchase is consumed immediately and they can tip again anytime they want.

---

## 3. How Users Pay with UPI (PhonePe, Google Pay, Paytm)

When a user taps any tip button inside the DoomSQL **Settings** screen:
1. Google Play opens its official bottom sheet overlay.
2. The user sees their saved payment methods.
3. If they select **UPI**, Google Play lets them choose:
   - **Google Pay**
   - **PhonePe**
   - **Paytm**
   - Or enter any custom UPI ID (`@okhdfcbank`, `@ybl`, etc.).
4. The user approves the transaction in their UPI app.
5. Google Play confirms the transaction, returns control to DoomSQL, and the app gives the user a **⭐ Supporter Badge**!

---

## 4. Google's Service Fee & Payouts to Your Bank

- **Google Play Service Fee:** For the first $1M USD of earnings each year, Google charges only **15%** (under Google's 15% tier for enrolled developers). You keep **85%**.
- **Payout Schedule:** Google transfers earnings automatically once a month (around the 15th to 20th of the following month) directly into your linked bank account via NEFT/Wire Transfer once your balance reaches the minimum threshold ($1 or ₹100 in India).

---

## 5. Testing In-App Billing Before Publishing

To test the payment flow without paying real money:
1. In Google Play Console, go to **Settings > License testing**.
2. Add your Google Account email (e.g., `omkarmaduguri000@gmail.com`).
3. Set **License test response** to `RESPOND_NORMALLY`.
4. When testing on your device with this account, Google Play will provide a **"Test Card, Always Approves"** option so you can test the entire flow end-to-end for free!
