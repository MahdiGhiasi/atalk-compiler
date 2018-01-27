import java.util.ArrayList;
import java.util.Arrays;

public class Receiver {
	
	public Receiver(String name) {
		this.name = name;
		inputTypes = new ArrayList<Type>();
    }

	public Receiver(String name, Type[] inputTypes, SymbolTable symTable) {
		this.name = name;
		if (inputTypes == null)
			this.inputTypes = new ArrayList<Type>();
		else
			this.inputTypes = new ArrayList<Type>(Arrays.asList(inputTypes));
		this.symTable = symTable;
	}

    public Receiver(String name, Type[] inputTypes, SymbolTable symTable, int index) {
		this(name, inputTypes, symTable);
		this.index = index;
	}

	public String getName() {
		return name;
    }
    
    public Type[] getTypes() {
        return inputTypes.toArray(new Type[inputTypes.size()]);
    }

	public int getArgumentsTotalSize() {
		int size = 0;
		for (Type t : inputTypes) {
			size += t.size();
		}
		return size;
	}

	@Override
	public String toString() {
        String key = name + " (";
        for (Type t : inputTypes) {
            key += t.toString() + ", ";
		}
		key = key.substring(0, inputTypes.size() > 0 ? key.length() - 2 : key.length()) + ")";
        return key;
	}

	public void setIndex(int index) { this.index = index; }
	public int getIndex() { return this.index; }

	private String name;
	ArrayList<Type> inputTypes;
	SymbolTable symTable;
	int index;
}