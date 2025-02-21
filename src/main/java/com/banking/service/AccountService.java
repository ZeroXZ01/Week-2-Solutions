package com.banking.service;

import com.banking.BankingSystem;
import com.banking.db.DatabaseConnection;
import com.banking.model.Account;
import com.banking.model.AccountFactory;
import com.banking.model.AccountType;
import com.banking.exception.*;
import com.banking.util.TransactionLogger;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * This class handles all banking operations like:
 * - Creating new accounts
 * - Depositing money
 * - Withdrawing money
 * - Transferring between accounts
 */
public class AccountService {
    // We need these to work with accounts and save transactions
    private final BankingSystem bankingSystem;
    private final TransactionLogger logger;

    // When we create AccountService, we need a BankingSystem
    public AccountService() {
        this.bankingSystem = new BankingSystem();
        this.logger = new TransactionLogger();
    }

    /**
     * Create a new bank account
     */
    public Account createAccount(AccountType type, String accountId, BigDecimal initialBalance)
            throws BankingException {
        // Check that initial balance is positive
        if (initialBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new BankingException("Initial balance cannot be negative");
        }

        // Create the account
        Account account = AccountFactory.createAccount(type, accountId, initialBalance);

        // Save it in the banking system
        bankingSystem.addAccount(account);

        // Log the initial deposit
        logger.addTransaction(accountId, initialBalance);

        return account;
    }

    /**
     * Deposit money into an account
     */
    public void deposit(String accountId, BigDecimal amount) throws BankingException {
        // Check that deposit amount is positive
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankingException("Deposit amount must be positive");
        }

        // Find the account
        Account account = findAccount(accountId);

        // Add the money
        account.deposit(amount);

        // Update the new balance of the said account in database
        bankingSystem.updateAccountAfterDeposit(account.getAccountNumber(), account.getBalance());

        // Save the transaction
        logger.addTransaction(accountId, amount);
    }

    /**
     * Withdraw money from an account
     */
    public void withdraw(String accountId, BigDecimal amount) throws BankingException {
        // Check that withdrawal amount is positive
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankingException("Withdrawal amount must be positive");
        }

        // Find the account
        Account account = findAccount(accountId);

        // Take out the money
        account.withdraw(amount);

        // Update the new balance of the said account in database
        bankingSystem.updateAccountAfterWithdrawal(account.getAccountNumber(), account.getBalance());

        // Save the transaction (negative amount for withdrawal)
        logger.addTransaction(accountId, amount.negate());
    }

    /**
     * Transfer money between accounts
     */
    public void transfer(String fromAccountId, String toAccountId, BigDecimal amount)
            throws BankingException, InsufficientFundsException, AccountNotFoundException {
        // Check that transfer amount is positive
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankingException("Transfer amount must be positive");
        }

        // Find both accounts
        Account fromAccount = findAccount(fromAccountId);
        Account toAccount = findAccount(toAccountId);

        // Take money from first account
        fromAccount.withdraw(amount);

        // Add money to second account
        toAccount.deposit(amount);

        // Update the new balances of the accounts
        bankingSystem.updateAccountAfterWithdrawal(fromAccount.getAccountNumber(), fromAccount.getBalance());
        bankingSystem.updateAccountAfterDeposit(toAccount.getAccountNumber(), toAccount.getBalance());

        // Save both transactions
        logger.addTransaction(fromAccountId, amount.negate());
        logger.addTransaction(toAccountId, amount);
    }

    /**
     * Helper method to find an account
     */
    public Account findAccount(String accountId) throws AccountNotFoundException {
        // Make sure account ID is valid
        if (accountId == null || accountId.isEmpty()) {
            throw new AccountNotFoundException("Account number cannot be empty");
        }

        // Try to find the account
        return bankingSystem.findAccount(accountId);
    }

    public void fetchAccount() {
        bankingSystem.fetchAccount();
    }

    public void getAccountsSortedByBalance(){
        bankingSystem.getAccountsSortedByBalance();
    }

    public void applyMonthlyFeesAndInterest() {
        bankingSystem.applyMonthlyFeesAndInterest();
    }

    public BigDecimal getTotalBalance(){
        return bankingSystem.getTotalBalance();
    }

    public void getAccountWithMinBalance(){
        bankingSystem.getAccountWithMinBalance();
    }

    public int getNumberOfAccounts(){
        return bankingSystem.getNumberOfAccounts();
    }
}



