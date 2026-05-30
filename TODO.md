# TODO - IgirePay JavaFX + Exercises

## 1) UI flow updates (Login 2. Register 0. Exit)
- [ ] Add Exit button to `src/main/resources/com/igirepay/igirepay/lab3/Login.fxml`.
- [ ] Implement `handleExit` in `src/main/java/lab3/LoginController.java` to close the JavaFX stage.
- [ ] Verify Register button wiring remains correct.

## 2) Dashboard.fxml wiring fixes
- [ ] Verify that each `onAction` in `Dashboard.fxml` matches methods in `DashboardController` (case-sensitive).
- [ ] Fix mismatched handler IDs (e.g. `#ViewHistory` vs `viewHistory`).

## 3) Exercise 3.2 exception handling (transactions + DB)
- [ ] Ensure invalid amount handling is displayed clearly for deposit/withdraw/transfer.
- [ ] Implement duplicate transaction request detection (likely via DAO/unique reference checks; add if missing).
- [ ] Ensure insufficient balance is shown for withdraw/transfer.
- [ ] Validate destination/from account IDs and show “Invalid account IDs”.
- [ ] Standardize database connection and SQL exceptions into user-friendly alerts.

## 4) Exercise 3.3 transaction reports
- [ ] Add “Customer transaction statement” feature/button.
- [ ] Implement statement formatting (selected account and time period; or last N days).
- [ ] Keep existing daily summary + export CSV + history.

## 5) Exercise 3.4 authentication (PIN)
- [ ] Ensure PIN creation/validation/check is consistent.
- [ ] Ensure PIN change validates current PIN + new format.
- [ ] Ensure failed login attempts lock account at 3 (verify and correct Customer/Login logic if needed).

## 6) Frontend styling
- [ ] Ensure `styles.css` uses the icon-like palette (green + orange #008000 and #FF8C00).
- [ ] Update any FXML inline styles to use CSS classes where possible.

## 7) Build & verify
- [ ] Run Maven build and test JavaFX startup.
- [ ] Manually verify: login/register/dashboard; transaction flows; reports; PIN change; exit.

