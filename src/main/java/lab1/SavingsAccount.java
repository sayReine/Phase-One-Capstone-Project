package lab1;

public class SavingsAccount extends Account{


    public SavingsAccount(String customerId) {
        super(customerId, "SAVINGS");
    }

    @Override
    public boolean withdraw(double amount, String referenceId) {


        double fee = 0;
        if (amount >=3000){
            fee = 500;
        }else {
            fee = 100;
        }

        amount +=  fee;

        if(amount <= 0 || amount >= getBalance()){
            System.out.println("insufficient fund.");
            return false;
        } else if (amount >=10500) {
            System.out.println("Sorry, you can't withdraw an amount above 10500 ");
            return false;

        }



            setBalance((int) (getBalance() - amount  ));
            System.out.println("transaction successfull /n you sent"+amount+"a fee of "+fee+"has been applied "+getBalance());
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
