package com.example.obitrust.data.ebooks

import com.example.obitrust.data.model.EBookEntity

object EBookCurriculum {
    val books: List<EBookEntity> = listOf(
        EBookEntity(
            title = "1. Introduction to Modern Bookkeeping",
            category = "Foundations",
            level = "Beginner",
            author = "Obitrust Business Academy",
            readTimeMinutes = 12,
            summary = "A comprehensive introduction to bookkeeping fundamentals, business transactions, source documents, and internal controls for small businesses.",
            contentMarkdown = """
# Introduction to Modern Bookkeeping

## What is Bookkeeping?
Bookkeeping is the systematic process of recording, classifying, and organizing financial transactions of a business. Without accurate bookkeeping, a business operates blindly without knowing whether it is generating true profit or draining working capital.

### Why Every Business Needs Bookkeeping:
1. **Accurate Profit Tracking:** Distinguishes between cash inflow and real net earnings.
2. **Tax Compliance:** Meets statutory reporting obligations under the Ghana Revenue Authority (GRA).
3. **Credit & Investor Readiness:** Banks and suppliers require clean books before extending credit or loans.
4. **Fraud & Leakage Prevention:** Prevents employee theft, unaccounted stock shrinkage, and cash drawer discrepancies.

## Source Documents in Business
Every bookkeeping entry must originate from a verifiable source document:
- **Sales Invoices & Receipts:** Issued to customers upon purchase.
- **Purchase Orders & Bills:** Received from suppliers when replenishing stock.
- **Payment Vouchers & MoMo Statements:** Verifies outlays for rent, utilities, and daily transport.
- **Credit Notes:** Issued when items are returned or discounted after billing.

## The Bookkeeping Lifecycle
1. Occurrence of transaction
2. Generation/collection of source document
3. Recording in Special Journals (Sales, Purchases, Cashbook)
4. Posting to the General Ledger
5. Extraction of Trial Balance
6. Preparation of Financial Statements
            """.trimIndent()
        ),
        EBookEntity(
            title = "2. Double-Entry Accounting Demystified",
            category = "Core Accounting",
            level = "Beginner",
            author = "Obitrust Business Academy",
            readTimeMinutes = 16,
            summary = "Master the golden rules of accounting: Debits, Credits, the Accounting Equation, and how to maintain balanced ledgers.",
            contentMarkdown = """
# Double-Entry Accounting Demystified

## The Core Concept
The double-entry system, pioneered by Luca Pacioli, is founded on a fundamental truth: **Every financial transaction has two equal and opposite effects.** If money enters one account, it leaves another or represents earned revenue or equity.

## The Accounting Equation
$$\text{Assets} = \text{Liabilities} + \text{Owner's Equity}$$

If your business owns GH₵ 50,000 in assets (Cash + Stock + Bank), those assets were funded either by:
- Creditors and suppliers (**Liabilities**)
- The business owner (**Equity and Retained Profits**)

## The Golden Rules of Debit and Credit
### 1. Asset Accounts (Cash, Bank, MoMo, Stock, Debtors)
- **Debit** to INCREASE
- **Credit** to DECREASE

### 2. Liability Accounts (Creditors, Supplier Debt, Loans)
- **Debit** to DECREASE
- **Credit** to INCREASE

### 3. Equity Accounts (Capital, Retained Earnings)
- **Debit** to DECREASE (Drawings)
- **Credit** to INCREASE (Capital injections, Profit)

### 4. Revenue Accounts (Sales, Service Fees)
- **Credit** to INCREASE
- **Debit** to DECREASE (Sales returns)

### 5. Expense Accounts (Rent, Light, Salaries, Transport)
- **Debit** to INCREASE
- **Credit** to DECREASE

## Practical Example
**Transaction:** You sell GH₵ 1,200 of goods for cash with a product cost of GH₵ 800.
- **Entry 1:**
  - Debit: Cash on Hand (1010) `GH₵ 1,200`
  - Credit: Sales Revenue (4010) `GH₵ 1,200`
- **Entry 2:**
  - Debit: Cost of Goods Sold (5010) `GH₵ 800`
  - Credit: Merchandise Inventory (1200) `GH₵ 800`

Notice that in both entries, **Debits strictly equal Credits**!
            """.trimIndent()
        ),
        EBookEntity(
            title = "3. Inventory Accounting & Stock Valuation",
            category = "Inventory",
            level = "Intermediate",
            author = "Obitrust Business Academy",
            readTimeMinutes = 15,
            summary = "Techniques for managing stock movement, preventing shrinkage, FIFO vs Weighted Average valuation, and inventory audit controls.",
            contentMarkdown = """
# Inventory Accounting & Stock Valuation

## The Importance of Inventory Control
For wholesale and retail enterprises in Ghana, inventory represents the single largest allocation of working capital. Poor stock management leads to expired products, pilferage, and sudden cash flow shortages.

## Perpetual vs Periodic Inventory Systems
- **Perpetual System (Used by Obitrust Business Manager):**
  - Inventory accounts update immediately upon every single sale or purchase.
  - Cost of Goods Sold (COGS) is recorded simultaneously with the sale.
  - Allows instant reorder notifications and stock alerts.
- **Periodic System:**
  - Stock levels are only confirmed by periodic physical stock counts at month-end.

## Stock Valuation Methods
1. **FIFO (First-In, First-Out):**
   - Assumes older stock is sold first.
   - During times of inflation, FIFO yields a higher closing stock valuation and lower COGS.
2. **Weighted Average Cost:**
   - Computes average purchase price whenever new inventory batches arrive.
   - Recommended for retail shops with frequent supplier price adjustments.

## Managing Stock Adjustments & Shrinkage
Stock losses from breakage, expiration, or theft must be formally adjusted in the system:
- **Debit:** Inventory Loss / Damage Expense (6190)
- **Credit:** Merchandise Inventory (1200)

Never simply delete product counts without logging a formal stock adjustment with audit notes!
            """.trimIndent()
        ),
        EBookEntity(
            title = "4. Cashbook & Mobile Money Management in Ghana",
            category = "Treasury",
            level = "Beginner",
            author = "Obitrust Business Academy",
            readTimeMinutes = 14,
            summary = "Managing multi-channel payments: Cash drawers, MTN MoMo, Telecel Cash, AT Money, and bank account reconciliation.",
            contentMarkdown = """
# Cashbook & Mobile Money Management in Ghana

## The Multi-Account Reality of Ghanaian Commerce
Modern businesses in Accra, Kumasi, and across Ghana operate across diverse payment channels:
- Physical Cash in Drawer
- MTN Mobile Money (Merchant SIM or Personal Wallet)
- Telecel Cash (formerly Vodafone Cash)
- AT Money (AirtelTigo Money)
- Commercial Bank Current Accounts (GCB, Ecobank, Zenith, Stanbic)

## Daily Cash & MoMo Reconciliation Rules
1. **End-of-Day Float Check:** Count physical cash in drawer and match against Obitrust Cashbook closing balance.
2. **MoMo Wallet Auditing:** Verify incoming SMS transaction IDs against POS sale receipts.
3. **Tracking E-Levy and Transfer Fees:** Record merchant transfer fees as Operating Expenses (Account 6120) rather than losing track of small deduction leakages.

## Inter-Account Transfers
When moving GH₵ 5,000 cash from your shop drawer into your GCB Bank account:
- Debit: GCB Bank Account (1020) `GH₵ 5,000`
- Credit: Cash on Hand (1010) `GH₵ 5,000`
Net business wealth does not change; only the holding vessel shifts!
            """.trimIndent()
        ),
        EBookEntity(
            title = "5. Financial Statements: Profit & Loss and Balance Sheet",
            category = "Financial Reporting",
            level = "Intermediate",
            author = "Obitrust Business Academy",
            readTimeMinutes = 18,
            summary = "How to interpret the Income Statement, Balance Sheet, and Cash Flow Statement to guide strategic business growth.",
            contentMarkdown = """
# Financial Statements Masterclass

## The Three Pillar Statements
1. **Profit & Loss Statement (Income Statement):** Measures performance over a specific period (e.g. Month of November).
   $$\text{Gross Profit} = \text{Revenue} - \text{Cost of Goods Sold}$$
   $$\text{Net Profit} = \text{Gross Profit} - \text{Operating Expenses}$$

2. **Balance Sheet (Statement of Financial Position):** A snapshot of financial standing on a specific date.
   - Shows what the business owns (Assets)
   - Shows what the business owes (Liabilities)
   - Shows owner's net equity (Capital + Retained Profits)

3. **Cash Flow Statement:** Demonstrates the actual movement of cash into and out of operating, investing, and financing activities.

## Vital Financial Ratios
- **Gross Profit Margin:** $\frac{\text{Gross Profit}}{\text{Revenue}} \times 100$
  - Healthy retail benchmarks in Ghana range between 25% and 45%.
- **Net Profit Margin:** $\frac{\text{Net Profit}}{\text{Revenue}} \times 100$
  - Represents the actual take-home return on every GH₵ 100 of revenue.
- **Current Ratio:** $\frac{\text{Current Assets}}{\text{Current Liabilities}}$
  - A ratio above 1.5 indicates strong ability to service debts without stress.
            """.trimIndent()
        ),
        EBookEntity(
            title = "6. Ghanaian Business Taxation & GRA Compliance",
            category = "Taxation",
            level = "Advanced",
            author = "Obitrust Business Academy",
            readTimeMinutes = 20,
            summary = "Understanding Ghana VAT, NHIL, GETFund, COVID-19 Health Recovery Levy, Flat Rate Scheme, and Withholding Tax.",
            contentMarkdown = """
# Ghanaian Business Taxation & GRA Compliance

## Overview of Ghanaian Indirect Taxes
Businesses registered with the Ghana Revenue Authority (GRA) operate under either the Standard VAT Scheme or the VAT Flat Rate Scheme.

### 1. Standard VAT Scheme (Total ~21.9%)
- **Value Added Tax (VAT):** 15%
- **National Health Insurance Levy (NHIL):** 2.5%
- **Ghana Education Trust Fund (GETFund):** 2.5%
- **COVID-19 Health Recovery Levy:** 1%
*Note:* NHIL, GETFund, and COVID-19 levies are treated as non-deductible costs to the consumer.

### 2. VAT Flat Rate Scheme (VFRS)
- Intended for retail wholesalers and retailers of taxable goods.
- Fixed 3% VAT + 1% COVID-19 Levy = **4% flat rate** on the selling price.
- Input tax credit cannot be claimed under the flat rate scheme.

## Withholding Tax (WHT)
When supplying goods or services to large corporate clients or government agencies, tax may be withheld at source:
- Supply of goods: 3% WHT
- Supply of services: 7.5% WHT
Ensure you obtain official Withholding Tax Credit Certificates from clients to offset against your annual income tax liability.
            """.trimIndent()
        ),
        EBookEntity(
            title = "7. Small Business Financial Management & Budgeting",
            category = "Management",
            level = "Intermediate",
            author = "Obitrust Business Academy",
            readTimeMinutes = 15,
            summary = "Cash flow budgeting, managing working capital, setting emergency reserves, and avoiding common debt pitfalls.",
            contentMarkdown = """
# Small Business Financial Management & Budgeting

## Working Capital Management
The biggest killer of profitable businesses is a lack of liquid cash. A business may show high net profit on paper, yet fail because all funds are locked in unsold stock and uncollected customer credit.

### The Cash Conversion Cycle:
$$\text{Days Inventory} + \text{Days Receivables} - \text{Days Payables}$$

### Strategies to Accelerate Cash Flow:
1. **Enforce Credit Limits:** Restrict customer debt terms to a maximum of 14 or 30 days.
2. **Offer Early Payment Discounts:** Provide a 2% discount if paid within 48 hours via MoMo.
3. **Negotiate Supplier Credit:** Seek 30-day payment windows from trusted manufacturers.
4. **Liquidate Dead Stock:** Discount slow-moving items to recover working capital.
            """.trimIndent()
        ),
        EBookEntity(
            title = "8. Strategic Growth & Debt Management",
            category = "Growth",
            level = "Advanced",
            author = "Obitrust Business Academy",
            readTimeMinutes = 16,
            summary = "Prudent leverage, financing expansion, hiring high-return staff, and preparing financial statements for commercial bank financing.",
            contentMarkdown = """
# Strategic Growth & Debt Management

## Good Debt vs Bad Debt in Business
- **Good Debt:** Borrowing at sustainable interest rates to purchase high-turnover inventory with guaranteed 40%+ margins.
- **Bad Debt:** Financing recurrent overheads, luxury office fittings, or personal owner withdrawals using high-interest commercial debt.

## Managing Accounts Receivable Aging
Monitor customer debt aging weekly:
- **0 - 30 Days:** Current & healthy. Send friendly WhatsApp balance reminder.
- **31 - 60 Days:** Overdue. Freeze further credit sales until balance is cleared.
- **61+ Days:** Critical risk. Initiate formal recovery procedures and demand personal guarantor payment.

Obitrust Business Manager includes automatic debtor tracking and one-tap WhatsApp payment notifications to keep your cash cycles rapid and secure.
            """.trimIndent()
        )
    )
}
