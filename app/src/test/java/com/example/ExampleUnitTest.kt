package com.example

import com.example.obitrust.data.accounting.AccountingEngine
import com.example.obitrust.data.accounting.ChartOfAccounts
import com.example.obitrust.data.model.JournalLineEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testDoubleEntryTrialBalanceBalancing() {
    val lines = listOf(
      JournalLineEntity(
        id = 1,
        journalEntryId = 1,
        accountCode = ChartOfAccounts.CODE_CASH,
        accountName = ChartOfAccounts.NAME_CASH,
        debit = 1000.0,
        credit = 0.0
      ),
      JournalLineEntity(
        id = 2,
        journalEntryId = 1,
        accountCode = ChartOfAccounts.CODE_SALES_REVENUE,
        accountName = ChartOfAccounts.NAME_SALES_REVENUE,
        debit = 0.0,
        credit = 1000.0
      )
    )

    val tb = AccountingEngine.generateTrialBalance(lines)
    assertEquals(1000.0, tb.totalDebit, 0.01)
    assertEquals(1000.0, tb.totalCredit, 0.01)
    assertTrue("Trial balance must be balanced when debits equal credits", tb.isBalanced)
  }

  @Test
  fun testProfitLossCalculation() {
    val lines = listOf(
      JournalLineEntity(
        id = 1,
        journalEntryId = 1,
        accountCode = ChartOfAccounts.CODE_SALES_REVENUE,
        accountName = ChartOfAccounts.NAME_SALES_REVENUE,
        debit = 0.0,
        credit = 2500.0
      ),
      JournalLineEntity(
        id = 2,
        journalEntryId = 1,
        accountCode = ChartOfAccounts.CODE_COGS,
        accountName = ChartOfAccounts.NAME_COGS,
        debit = 1500.0,
        credit = 0.0
      ),
      JournalLineEntity(
        id = 3,
        journalEntryId = 2,
        accountCode = ChartOfAccounts.CODE_ELECTRICITY,
        accountName = ChartOfAccounts.NAME_ELECTRICITY,
        debit = 300.0,
        credit = 0.0
      )
    )

    val pl = AccountingEngine.generateProfitLoss(lines)
    assertEquals(2500.0, pl.salesRevenue, 0.01)
    assertEquals(1500.0, pl.costOfGoodsSold, 0.01)
    assertEquals(1000.0, pl.grossProfit, 0.01)
    assertEquals(300.0, pl.totalExpenses, 0.01)
    assertEquals(700.0, pl.netProfit, 0.01)
    assertEquals(28.0, pl.netMarginPercent, 0.1)
  }
}
