public class SymbolTableGlobalVariableItem extends SymbolTableVariableItemBase {
	
	public SymbolTableGlobalVariableItem(Variable variable, int offset) {
		super(variable, offset);
	}

	@Override
	public Register getBaseRegister() {
		return Register.GP;
	}

	@Override
	public boolean useMustBeComesAfterDef() {
		return false;
	}

	@Override
	public String toString() {
		return "Global variable: " +variable.toString() + ", location = " + offset;
	}
}