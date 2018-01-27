import java.util.Map;

public class Variable {
	
	public Variable(String name, Type type) {
		this.name = name;
		this.type = type;
	}

	public String getName() {
		return name;
	}
	
	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	//public void setType(Type type, SymbolTable table) {
	//	int sizeDif = type.size() - this.type.size();
	//	this.type = type;
	//
	//	for (SymbolTableItem item : table.items.values()) {
	//		if (item instanceof SymbolTableLocalVariableItem) {
	//			SymbolTableLocalVariableItem x = (SymbolTableLocalVariableItem)item;
	//
	//			x.offset += sizeDif;
	//		}
	//	}
	//
	//	((SymbolTableLocalVariableItem)table.get(name)).offset -= sizeDif;
	//}

	public int size() {
		return type.size();
	}

	@Override
	public String toString() {
		return type.toString() + " " + name + ", size = " + size() + " bytes";
	}

	private String name;
	private Type type;
}