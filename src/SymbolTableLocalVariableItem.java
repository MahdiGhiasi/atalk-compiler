public class SymbolTableLocalVariableItem extends SymbolTableVariableItemBase {
	
	public SymbolTableLocalVariableItem(Variable variable, int offset) {
		super(variable, offset);
	}

	@Override
	public Register getBaseRegister() {
		return Register.SP;
	}

	@Override
	public boolean useMustBeComesAfterDef() {
		return true;
	}

	@Override
	public String toString() {
		return "Local variable: " +variable.toString() + ", location = " + offset;
	}
}