package lab1;

public class WalletAccount extends Account{
    public WalletAccount(String customerId ) {
        super(customerId, "WALLET");
    }

    @Override
    public boolean withdraw(double amount, String referenceId) {

       if(amount <= 0 || amount >= getBalance()  ) return false;
       setBalance((int) (getBalance() - amount));
       return true;
    }

    @Override
    public boolean deposit(double amount, String referenceId) {
        if(amount <= 0 ) return false;
        setBalance((int) (getBalance() + amount));
        return true;
    }

    @Override
    public String processTransaction(Transaction t) {
        if(t.getType().equals("Deposit")){

            return deposit(t.getAmount(),t.getReferenceId())? " SUCCESS" : " FAILED";
        } else if (t.getType().equals("Withdraw")) {

            return withdraw(t.getAmount(),t.getReferenceId())? "SUCCESS" : "FAILED";
        }
        return "UNKNOWN_Type";

    }
}
