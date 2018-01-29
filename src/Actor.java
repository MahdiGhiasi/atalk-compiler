public class Actor {
	
	public Actor(String name, int capacity, SymbolTable symTable) {
		this.name = name;
		this.capacity = capacity + 1;
		if (this.capacity < 1)
			this.capacity = 1;

		this.symTable = symTable;		
    }
    
    public Actor(String name, String capacityBil, SymbolTable symTable) {
		this.name = name;
		this.capacity = Integer.parseInt(capacityBil.substring(1, capacityBil.length()-1)) + 1;
		if (this.capacity < 1)
			this.capacity = 1;

		this.symTable = symTable;	
	}

	public String getName() {
		return name;
	}
	
	public int getCapacity() {
		return capacity;
	}

	public SymbolTable getSymbolTable() {
		return symTable;
	}

	@Override
	public String toString() {
		return name + ", inbox size = " + capacity;
	}

	private String name;
	private int capacity;
	SymbolTable symTable;	
}