package com.example.obitrust.data.accounting

object ChartOfAccounts {
    // Assets (1000 - 1999)
    const val CODE_CASH = "1010"
    const val NAME_CASH = "Cash on Hand"

    const val CODE_BANK = "1020"
    const val NAME_BANK = "Bank Account"

    const val CODE_MOMO = "1030"
    const val NAME_MOMO = "Mobile Money (MoMo)"

    const val CODE_ACCOUNTS_RECEIVABLE = "1100"
    const val NAME_ACCOUNTS_RECEIVABLE = "Accounts Receivable (Customer Debt)"

    const val CODE_INVENTORY = "1200"
    const val NAME_INVENTORY = "Merchandise Inventory Asset"

    // Liabilities (2000 - 2999)
    const val CODE_ACCOUNTS_PAYABLE = "2010"
    const val NAME_ACCOUNTS_PAYABLE = "Accounts Payable (Supplier Debt)"

    const val CODE_TAX_PAYABLE = "2050"
    const val NAME_TAX_PAYABLE = "Sales Tax Payable (GRA VAT/Levies)"

    // Equity (3000 - 3999)
    const val CODE_CAPITAL = "3010"
    const val NAME_CAPITAL = "Owner's Capital"

    const val CODE_DRAWINGS = "3020"
    const val NAME_DRAWINGS = "Owner's Drawings"

    const val CODE_RETAINED_EARNINGS = "3030"
    const val NAME_RETAINED_EARNINGS = "Retained Earnings"

    // Revenue (4000 - 4999)
    const val CODE_SALES_REVENUE = "4010"
    const val NAME_SALES_REVENUE = "Sales Revenue"

    // Cost of Goods Sold (5000 - 5999)
    const val CODE_COGS = "5010"
    const val NAME_COGS = "Cost of Goods Sold (COGS)"

    // Expenses (6000 - 6999)
    const val CODE_RENT = "6010"
    const val NAME_RENT = "Rent Expense"

    const val CODE_ELECTRICITY = "6020"
    const val NAME_ELECTRICITY = "Electricity Expense"

    const val CODE_WATER = "6030"
    const val NAME_WATER = "Water Expense"

    const val CODE_TRANSPORTATION = "6040"
    const val NAME_TRANSPORTATION = "Transportation Expense"

    const val CODE_SALARIES = "6050"
    const val NAME_SALARIES = "Salaries & Wages"

    const val CODE_FUEL = "6060"
    const val NAME_FUEL = "Fuel Expense"

    const val CODE_INTERNET = "6070"
    const val NAME_INTERNET = "Internet & Communication"

    const val CODE_MAINTENANCE = "6080"
    const val NAME_MAINTENANCE = "Maintenance & Repairs"

    const val CODE_FOOD = "6090"
    const val NAME_FOOD = "Food & Refreshments"

    const val CODE_STATIONERY = "6100"
    const val NAME_STATIONERY = "Stationery & Supplies"

    const val CODE_MARKETING = "6110"
    const val NAME_MARKETING = "Marketing & Promotion"

    const val CODE_TRANSFER_FEES = "6120"
    const val NAME_TRANSFER_FEES = "Bank & MoMo Transfer Charges"

    const val CODE_STOCK_LOSS = "6190"
    const val NAME_STOCK_LOSS = "Inventory Damage & Loss"

    const val CODE_MISC_EXPENSE = "6200"
    const val NAME_MISC_EXPENSE = "Miscellaneous Expense"

    fun getExpenseCodeAndName(category: String): Pair<String, String> {
        return when (category.lowercase().trim()) {
            "rent" -> CODE_RENT to NAME_RENT
            "electricity" -> CODE_ELECTRICITY to NAME_ELECTRICITY
            "water" -> CODE_WATER to NAME_WATER
            "transportation", "transport" -> CODE_TRANSPORTATION to NAME_TRANSPORTATION
            "salaries", "salary", "wages" -> CODE_SALARIES to NAME_SALARIES
            "fuel" -> CODE_FUEL to NAME_FUEL
            "internet" -> CODE_INTERNET to NAME_INTERNET
            "maintenance" -> CODE_MAINTENANCE to NAME_MAINTENANCE
            "food" -> CODE_FOOD to NAME_FOOD
            "stationery" -> CODE_STATIONERY to NAME_STATIONERY
            "marketing" -> CODE_MARKETING to NAME_MARKETING
            else -> CODE_MISC_EXPENSE to NAME_MISC_EXPENSE
        }
    }
}
