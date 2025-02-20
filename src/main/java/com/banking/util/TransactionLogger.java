package com.banking.util;

import com.banking.db.DatabaseConnection;
import com.banking.exception.AccountNotFoundException;
import com.banking.exception.BankingException;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TransactionLogger {

    public List<TransactionRecord> getTransactionHistory(String accountId) {
        String sql = "SELECT * FROM transactions WHERE account_id = ? ORDER BY date DESC";
        List<TransactionRecord> transactions = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, accountId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                transactions.add(new TransactionRecord(rs.getString("account_id"), rs.getBigDecimal("amount"), rs.getTimestamp("date").toLocalDateTime()));
            }
            return transactions;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to get transaction history", e);
        }
    }

    public static void addTransaction(String accountId, BigDecimal amount) {
        String insertSql = "INSERT INTO transactions (account_id, amount, date) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement stmt = conn.prepareStatement(insertSql)){
            stmt.setString(1, accountId);
            stmt.setBigDecimal(2, amount);
            stmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void fetchTransactions() {
        String sql = "SELECT * FROM transactions";

        try (Connection conn = DatabaseConnection.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("+------------+---------+----------------------------+");
            System.out.println("| Account ID | Amount  | Date                       |");
            System.out.println("+------------+---------+----------------------------+");

            while (rs.next()) {
                String accountId= rs.getString("account_id");
                double amount= rs.getDouble("amount");
                String date = rs.getNString("date");

                System.out.printf("| %-10s | %7.2f | %-26s |\n", accountId, amount, date);
            }

            System.out.println("+------------+---------+----------------------------+");

        } catch (SQLException e) {
            throw new BankingException("No transaction created on the table yet", e);
        }
    }

    // Clear all transactions
    public static void clearTransactions() {
        try (Connection conn = DatabaseConnection.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM transactions");
            System.out.println("All transactions have been deleted.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static class TransactionRecord {
        private final String accountId;
        private final BigDecimal amount;
        private final LocalDateTime date;

        public TransactionRecord(String accountId, BigDecimal amount, LocalDateTime date) {
            this.accountId = accountId;
            this.amount = amount;
            this.date = date;
        }

        public String getAccountId() {
            return accountId;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public LocalDateTime getDate() {
            return date;
        }

        @Override
        public String toString() {
            return String.format("%s: $%.2f on %s", accountId, amount, date);
        }
    }
}
