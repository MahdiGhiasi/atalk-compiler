public class UnknownType extends Type {
	
	public int size() {
		return 4;
	}

	@Override
	public boolean equals(Object other) {
		if(other instanceof UnknownType)
			return true;
		return false;
	}

	@Override
	public String toString() {
		return "unknownType";
	}

	private static UnknownType instance;

	public static UnknownType getInstance() {
		if(instance == null)
			return instance = new UnknownType();
		return instance;
	}
}