package kz.bank.ui;

import java.math.BigDecimal;
import java.util.Scanner;

/**
 * Centralised wrapper around Scanner.
 * All user input flows through this class so Scanner is never scattered across the project.
 * Handles malformed input gracefully and re-prompts the user.
 */
public class ConsoleInput {

    private final Scanner scanner;

    public ConsoleInput() {
        this.scanner = new Scanner(System.in);
    }

    /**
     * Read a non-blank string from the user.
     * Re-prompts until a non-empty value is entered.
     *
     * @param prompt label shown before the input cursor
     * @return trimmed, non-blank string
     */
    public String readString(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            if (!line.isEmpty()) {
                return line;
            }
            System.out.println("  [!] Value must not be empty. Please try again.");
        }
    }

    /**
     * Read a positive long integer from the user.
     * Re-prompts on non-numeric input or values <= 0.
     *
     * @param prompt label shown before the input cursor
     * @return positive long value
     */
    public long readPositiveLong(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try {
                long value = Long.parseLong(line);
                if (value > 0) {
                    return value;
                }
                System.out.println("  [!] Value must be greater than zero. Please try again.");
            } catch (NumberFormatException e) {
                System.out.println("  [!] Invalid number: '" + line + "'. Please enter a whole number.");
            }
        }
    }

    /**
     * Read an integer from the user within the range [min, max].
     * Re-prompts on non-numeric input or out-of-range values.
     *
     * @param prompt label shown before the input cursor
     * @param min    inclusive lower bound
     * @param max    inclusive upper bound
     * @return integer in [min, max]
     */
    public int readIntInRange(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try {
                int value = Integer.parseInt(line);
                if (value >= min && value <= max) {
                    return value;
                }
                System.out.printf("  [!] Please enter a number between %d and %d.%n", min, max);
            } catch (NumberFormatException e) {
                System.out.println("  [!] Invalid input: '" + line + "'. Please enter a whole number.");
            }
        }
    }

    /**
     * Read a positive BigDecimal amount from the user.
     * Re-prompts on non-numeric input or values <= 0.
     *
     * @param prompt label shown before the input cursor
     * @return BigDecimal > 0
     */
    public BigDecimal readPositiveAmount(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try {
                BigDecimal value = new BigDecimal(line);
                if (value.compareTo(BigDecimal.ZERO) > 0) {
                    return value;
                }
                System.out.println("  [!] Amount must be greater than zero. Please try again.");
            } catch (NumberFormatException e) {
                System.out.println("  [!] Invalid amount: '" + line + "'. Please enter a number (e.g. 1000.00).");
            }
        }
    }

    /**
     * Ask user to choose a deposit type.
     * Returns 1 for WITHDRAWABLE, 2 for SAVINGS.
     */
    public int readDepositTypeChoice() {
        System.out.println("  Deposit types:");
        System.out.println("    1. WITHDRAWABLE (top-up and withdraw)");
        System.out.println("    2. SAVINGS      (top-up only, no withdrawals)");
        return readIntInRange("  Your choice: ", 1, 2);
    }

    /**
     * Ask user to choose an interest rate from the predefined list.
     * Returns the selected rate as BigDecimal.
     *
     * Available rates: 17.0, 17.5, 19.0, 21.0, 22.3
     */
    public java.math.BigDecimal readRate() {
        java.math.BigDecimal[] rates = {
            new java.math.BigDecimal("17.0"),
            new java.math.BigDecimal("17.5"),
            new java.math.BigDecimal("19.0"),
            new java.math.BigDecimal("21.0"),
            new java.math.BigDecimal("22.3")
        };
        System.out.println("  Interest rates (annual %):");
        for (int i = 0; i < rates.length; i++) {
            System.out.printf("    %d. %.1f%%%n", i + 1, rates[i]);
        }
        int choice = readIntInRange("  Your choice: ", 1, rates.length);
        return rates[choice - 1];
    }

    /**
     * Ask user to choose a deposit term in months.
     * Returns one of: 3, 6, 9, 12.
     */
    public int readTermMonths() {
        System.out.println("  Deposit term (months):");
        System.out.println("    1.  3 months");
        System.out.println("    2.  6 months");
        System.out.println("    3.  9 months");
        System.out.println("    4. 12 months");
        int choice = readIntInRange("  Your choice: ", 1, 4);
        int[] terms = {3, 6, 9, 12};
        return terms[choice - 1];
    }

    /** Close the underlying scanner (call on application exit). */
    public void close() {
        scanner.close();
    }
}
