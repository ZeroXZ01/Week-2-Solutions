package com.banking;

import com.banking.exception.AccountNotFoundException;
import com.banking.model.Account;
import com.banking.model.AccountType;
import com.banking.model.CheckingAccount;
import com.banking.model.SavingsAccount;
import com.banking.service.AccountService;
import com.banking.util.TransactionLogger;
import org.h2.tools.Server;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

public class Main {
    private final Scanner scanner;
    private final AccountService accountService;
    private final TransactionLogger transactionLogger;
    private Server h2Server;

    public Main() {
        this.scanner = new Scanner(System.in);
        this.accountService = new AccountService();
        this.transactionLogger = new TransactionLogger();
    }

    public void start() {
        startH2Console();
        while (true) {
            displayMenu();
            int choice = getIntInput("Enter choice: ");

            try {
                processChoice(choice);
                if (choice == 12) {
                    break;
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
            System.out.println("\nPress Enter to continue...");
            scanner.nextLine();
        }
        stopH2Console();
    }

    private void startH2Console() {
        try {
            h2Server = Server.createWebServer("-web", "-webAllowOthers", "-webPort", "8082").start();
            System.out.println("H2 Console available at http://localhost:8082");
            System.out.println("Login with username 'sa' and empty password");
            System.out.println("JDBC URL: jdbc:h2:./banking_db\n");
        } catch (SQLException e) {
            System.out.println("Warning: Could not start H2 Console: " + e.getMessage());
        }
    }

    private void stopH2Console() {
        if (h2Server != null) {
            h2Server.stop();
        }
    }

    private void displayMenu() {
        System.out.println("=== Banking System Menu ===");
        System.out.println("1. Create Account");
        System.out.println("2. View Account");
        System.out.println("3. Deposit");
        System.out.println("4. Withdraw");
        System.out.println("5. Transfer");
        System.out.println("6. View Transactions");
        System.out.println("OPTIONALS");
        System.out.println("7. Apply Monthly Fees");
        System.out.println("8. Calculate the Total Balance of Accounts");
        System.out.println("9. Display Account(s) with the Lowest Balance");
        System.out.println("10. Calculate the Total Number of Accounts");
        System.out.println("11. Clear Transactions");
        System.out.println("12. Exit");
        System.out.println("========================");
    }

    private void processChoice(int choice) {
        switch (choice) {
            case 1:
                createAccount();
                break;
            case 2:
                viewAccount();
                break;
            case 3:
                makeDeposit();
                break;
            case 4:
                makeWithdrawal();
                break;
            case 5:
                makeTransfer();
                break;
            case 6:
                viewTransactions();
                break;
            case 7:
                accountService.applyMonthlyFeesAndInterest();
                transactionLogger.fetchTransactions();
                break;
            case 8:
                BigDecimal totalBalance = accountService.getTotalBalance();
                System.out.println("The combined balance of all accounts is $" + totalBalance);
                break;
            case 9:
                accountService.getAccountWithMinBalance();
                break;
            case 10:
                System.out.println("The total number of accounts is: " + accountService.getNumberOfAccounts());
                break;
            case 11:
                transactionLogger.clearTransactions();
                break;
            case 12:
                System.out.println("Thank you for using the Banking System!");
                break;
            default:
                System.out.println("Invalid choice. Please try again.");
        }
    }

    private void createAccount() {
        System.out.println("\n=== Create New Account ===");
        System.out.println("1. Savings Account");
        System.out.println("2. Checking Account");

        int accountType = getIntInput("Account type (1-Savings, 2-Checking): ");
        BigDecimal initialDeposit = getBigDecimalInput("Initial deposit: $");

        try {
            String accountNumber;

            if (accountType == 1) {
                accountNumber = "SAV" + System.currentTimeMillis() % 10000;
                accountService.createAccount(AccountType.SAVINGS, accountNumber, initialDeposit);
            } else if (accountType == 2) {
                accountNumber = "CHK" + System.currentTimeMillis() % 10000;
                accountService.createAccount(AccountType.CHECKING, accountNumber, initialDeposit);
            } else {
                throw new IllegalArgumentException("Invalid account type");
            }

            System.out.println("Account created successfully!");
            System.out.println("Your account number is: " + accountNumber);

            // To be removed?
            System.out.println();
            accountService.fetchAccount();

        } catch (Exception e) {
            System.out.println("Error creating account: " + e.getMessage());
        }
    }

    private void viewAccount() {
        System.out.println("\n=== View Account ===");
        String accountNumber = getStringInput("Enter account number: ");

        try {
            Account account = accountService.findAccount(accountNumber);
            System.out.println("\nAccount Details:");
            System.out.println("Account Number: " + account.getAccountNumber());
            System.out.println("Type: " + account.getClass().getSimpleName());
            System.out.println("Balance: $" + account.getBalance());

            if (account instanceof SavingsAccount) {
                System.out.println("Interest Rate: " + ((SavingsAccount) account).getInterestRate().multiply(new BigDecimal("100")) + "%");
            } else if (account instanceof CheckingAccount) {
                System.out.println("Monthly Transactions: " + ((CheckingAccount) account).getMonthlyTransactions());
            }
        } catch (AccountNotFoundException e) {
            System.out.println("Account not found: " + accountNumber);
        }
    }

    private void makeDeposit() {
        System.out.println("\n=== Make Deposit ===");
        String accountNumber = getStringInput("Enter account number: ");
        BigDecimal amount = getBigDecimalInput("Enter deposit amount: $");

        try {
            accountService.deposit(accountNumber, amount);
            System.out.println("Deposit successful!");
            System.out.println("New balance: $" + accountService.findAccount(accountNumber).getBalance());

            // To be removed?
            System.out.println();
            transactionLogger.fetchTransactions();

        } catch (Exception e) {
            System.out.println("Deposit failed: " + e.getMessage());
        }
    }

    private void makeWithdrawal() {
        System.out.println("\n=== Make Withdrawal ===");
        String accountNumber = getStringInput("Enter account number: ");
        BigDecimal amount = getBigDecimalInput("Enter withdrawal amount: $");

        try {
            accountService.withdraw(accountNumber, amount);
            System.out.println("Withdrawal successful!");
            System.out.println("New balance: $" + accountService.findAccount(accountNumber).getBalance());

            // To be removed?
            System.out.println();
            transactionLogger.fetchTransactions();

        } catch (Exception e) {
            System.out.println("Withdrawal failed: " + e.getMessage());
        }
    }

    private void makeTransfer() {
        System.out.println("\n=== Make Transfer ===");
        String fromAccount = getStringInput("Enter source account number: ");
        String toAccount = getStringInput("Enter destination account number: ");
        BigDecimal amount = getBigDecimalInput("Enter transfer amount: $");

        try {
            accountService.transfer(fromAccount, toAccount, amount);
            System.out.println("Transfer successful!");
            System.out.println("Source account balance: $" + accountService.findAccount(fromAccount).getBalance());
            System.out.println("Destination account balance: $" + accountService.findAccount(toAccount).getBalance());

            // To be removed?
            System.out.println();
            accountService.fetchAccount();

            System.out.println();
            transactionLogger.fetchTransactions();

        } catch (Exception e) {
            System.out.println("Transfer failed: " + e.getMessage());
        }
    }

    private void viewTransactions() {
        System.out.println("\n=== View Transactions ===");
        String accountNumber = getStringInput("Enter account number: ");

        try {
            List<TransactionLogger.TransactionRecord> transactions =
                    transactionLogger.getTransactionHistory(accountNumber);

            if (transactions.isEmpty()) {
                System.out.println("No transactions found for this account.");
                return;
            }

            System.out.println("\nTransaction History:");
            System.out.println("----------------------------------------");
            for (TransactionLogger.TransactionRecord transaction : transactions) {
                System.out.printf("Date: %s\n", transaction.getDate());
                System.out.printf("Amount: $%.2f\n", transaction.getAmount());
                System.out.println("----------------------------------------");
            }
        } catch (Exception e) {
            System.out.println("Error retrieving transactions: " + e.getMessage());
        }
    }

    private String getStringInput(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private int getIntInput(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                int value = Integer.parseInt(scanner.nextLine().trim());
                if (value < 0) {
                    System.out.println("Please enter a positive number.");
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    private BigDecimal getBigDecimalInput(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                BigDecimal value = new BigDecimal(scanner.nextLine().trim());
                if (value.compareTo(BigDecimal.ZERO) <= 0) {
                    System.out.println("Please enter a positive amount.");
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid amount.");
            }
        }
    }

    public static void main(String[] args) {
        Main app = new Main();
        app.start();
    }
}
