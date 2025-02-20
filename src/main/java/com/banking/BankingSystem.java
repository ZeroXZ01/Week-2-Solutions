package com.banking;

import com.banking.db.DatabaseConnection;
import com.banking.exception.BankingException;
import com.banking.exception.InsufficientFundsException;
import com.banking.model.Account;
import com.banking.exception.AccountNotFoundException;
import com.banking.model.CheckingAccount;
import com.banking.model.SavingsAccount;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;

public class BankingSystem {
    public void addAccount(Account account) {
        String sql = "INSERT INTO accounts (account_id, type, balance) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, account.getAccountNumber());
            stmt.setString(2, account.getClass().getSimpleName().replace("Account", "").toUpperCase());
            stmt.setBigDecimal(3, account.getBalance());
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new BankingException("Failed to create account", e);
        }
    }

    public Account findAccount(String accountId) {
        String sql = "SELECT * FROM accounts WHERE account_id = ?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, accountId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String type = rs.getString("type");
                BigDecimal balance = rs.getBigDecimal("balance");

                if ("SAVINGS".equals(type)) {
                    return new SavingsAccount(accountId, balance);
                } else {
                    return new CheckingAccount(accountId, balance);
                }
            }
            throw new AccountNotFoundException(accountId);

        } catch (SQLException e) {
            throw new BankingException("Failed to find account", e);
        }
    }

    public void updateAccountAfterDeposit(String accountId, BigDecimal amount) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Update account balance
                String updateSql = "UPDATE accounts SET balance = ? WHERE account_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                    stmt.setBigDecimal(1, amount);
                    stmt.setString(2, accountId);
                    int updated = stmt.executeUpdate();
                    if (updated == 0) {
                        throw new AccountNotFoundException(accountId);
                    }
                }

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw new BankingException("Deposit failed", e);
            }
        } catch (SQLException e) {
            throw new BankingException("Database error during deposit", e);
        }
    }

    public void updateAccountAfterWithdrawal(String accountId, BigDecimal amount) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Check sufficient funds
                String checkSql = "SELECT balance FROM accounts WHERE account_id = ?";
                BigDecimal currentBalance;
                try (PreparedStatement stmt = conn.prepareStatement(checkSql)) {
                    stmt.setString(1, accountId);
                    ResultSet rs = stmt.executeQuery();
                    if (!rs.next()) {
                        throw new AccountNotFoundException(accountId);
                    }
                    currentBalance = rs.getBigDecimal("balance");
                }

                if (currentBalance.compareTo(amount) < 0) {
                    throw new InsufficientFundsException(accountId, amount, currentBalance);
                }

                // Update account balance
                String updateSql = "UPDATE accounts SET balance = ? WHERE account_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                    stmt.setBigDecimal(1, amount);
                    stmt.setString(2, accountId);
                    stmt.executeUpdate();
                }

                conn.commit();

            } catch (Exception e) {
                conn.rollback();
                throw new BankingException("Withdrawal failed", e);
            }
        } catch (SQLException e) {
            throw new BankingException("Database error during withdrawal", e);
        }
    }

    public static void fetchAccount() {
        String sql = "SELECT * FROM accounts";

        try (Connection conn = DatabaseConnection.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("+------------+----------+---------+");
            System.out.println("| Account ID | Type     | Balance |");
            System.out.println("+------------+----------+---------+");

            while (rs.next()) {
                String accountId = rs.getString("account_id");
                String type = rs.getString("type");
                double balance = rs.getDouble("balance");

                System.out.printf("| %-10s | %-8s | %7.2f |\n", accountId, type, balance);
            }

            System.out.println("+------------+----------+---------+");

        } catch (SQLException e) {
            throw new BankingException("No account created on the table yet", e);
        }
    }

    /**
     * // Get accounts sorted by balance [Week 1]
     * public List<Account> getAccountsSortedByBalance() {
     * return accounts.stream()
     * .sorted(Comparator.comparing(Account::getBalance))
     * .collect(Collectors.toList());
     * }
     */
    // Get accounts sorted by balance [Week 2 Version]
    public static void getAccountsSortedByBalance() {
        String sql = "SELECT * FROM accounts ORDER BY balance ASC";

        try (Connection conn = DatabaseConnection.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("+------------+----------+---------+");
            System.out.println("| Account ID | Type     | Balance |");
            System.out.println("+------------+----------+---------+");

            while (rs.next()) {
                String accountId = rs.getString("account_id");
                String type = rs.getString("type");
                double balance = rs.getDouble("balance");

                System.out.printf("| %-10s | %-8s | %7.2f |\n", accountId, type, balance);
            }

            System.out.println("+------------+----------+---------+");

        } catch (SQLException e) {
            throw new BankingException("No account created on the table yet", e);
        }
    }

    /**
     * // Process monthly fees for all accounts [Week 1]
     * public void processMonthlyFees() {
     * accounts.forEach(Account::processMonthlyFees);
     * }
     */

    // Process monthly fees for all accounts [Week 2 Version]
    public static void applyMonthlyFeesAndInterest() {
        String sql = "SELECT * FROM accounts";
        try (Connection conn = DatabaseConnection.getConnection(); Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                String accountId = rs.getString("account_id");
                String type = rs.getString("type");
                BigDecimal balance = rs.getBigDecimal("balance");

                if ("SAVINGS".equals(type)) {
                    SavingsAccount account = new SavingsAccount(accountId, balance);
                    account.processMonthlyFees();
                    updateAccountBalance(account.getAccountNumber(), account.getBalance());
                    BigDecimal originalBalance = account.getBalance().subtract(balance);
                    addTransactionForMonthlyFeesAndInterest(account.getAccountNumber(), originalBalance);

                } else if ("CHECKING".equals(type)) {
                    CheckingAccount account = new CheckingAccount(accountId, balance);
                    account.processMonthlyFees();
                    updateAccountBalance(account.getAccountNumber(), account.getBalance());
                    BigDecimal originalBalance = account.getBalance().subtract(balance);
                    addTransactionForMonthlyFeesAndInterest(account.getAccountNumber(), originalBalance);
                }
            }
            System.out.println("Monthly fees and interest applied successfully.");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void updateAccountBalance(String accountNumber, BigDecimal newBalance) {
        String sql = "UPDATE accounts SET balance = ? WHERE account_id = ?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBigDecimal(1, newBalance);
            pstmt.setString(2, accountNumber);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void addTransactionForMonthlyFeesAndInterest(String accountId, BigDecimal amount) {
        String insertSql = "INSERT INTO transactions (account_id, amount, date) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement stmt = conn.prepareStatement(insertSql)) {
            stmt.setString(1, accountId);
            stmt.setBigDecimal(2, amount);
            stmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * // Get total balance across all accounts [Week 1]
     * public BigDecimal getTotalBalance() {
     * return accounts.stream().map(Account::getBalance).reduce(BigDecimal.ZERO, BigDecimal::add);
     * }
     */
    // Get total balance across all accounts [Week 2 Version]
    public static BigDecimal getTotalBalance() {
        String sql = "SELECT SUM(balance) AS total_balance FROM accounts";
        try (Connection conn = DatabaseConnection.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getBigDecimal("total_balance");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return BigDecimal.ZERO;
    }

    /**
     * // Get accounts filtered by minimum balance [Week 1]
     * public List<Account> getAccountsAboveBalance(BigDecimal minimumBalance) {
     * return accounts.stream().filter(a -> a.getBalance().compareTo(minimumBalance) > 0).collect(Collectors.toList());
     * }
     **/
    // Get accounts filtered by minimum balance [Week 2 Version]
    public static void getAccountWithMinBalance() {
        String sql = "SELECT account_id, balance FROM accounts ORDER BY balance ASC LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                System.out.println("Account with Minimum Balance:");
                System.out.println("+---------------+----------+");
                System.out.println("| Account No    | Balance  |");
                System.out.println("+---------------+----------+");
                System.out.printf("| %-13s | %8.2f |\n", rs.getString("account_id"), rs.getDouble("balance"));
                System.out.println("+---------------+----------+");
            } else {
                System.out.println("No accounts found.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * // Get number of accounts [Week 1]
     * public int getNumberOfAccounts() {
     * return accounts.size();
     * }
     **/
    // Get number of accounts [Week 2 Version]
    public static int getNumberOfAccounts() {
        String sql = "SELECT COUNT(*) AS total_accounts FROM accounts";
        try (Connection conn = DatabaseConnection.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt("total_accounts");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * // Clear all accounts [Week 1]
     * public void clearAccounts() {
     * accounts.clear();
     * accountMap.clear();
     * }
     */
    // Clear all accounts [Week 2 Version]
    public static void clearAccounts() {
        try (Connection conn = DatabaseConnection.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM accounts");
            System.out.println("All accounts have been deleted.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
