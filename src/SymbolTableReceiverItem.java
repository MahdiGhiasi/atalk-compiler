public class SymbolTableReceiverItem extends SymbolTableItem {
	
	public SymbolTableReceiverItem(Receiver receiver) {
        super();
        this.receiver = receiver;
    }

	public Receiver getReceiver() {
		return receiver;
    }

	@Override
	public String getKey() {
        return receiver.toString();
    }
    
    @Override
    public String toString() {
        return "Receiver was " + receiver.toString();
    }

    Receiver receiver;
}