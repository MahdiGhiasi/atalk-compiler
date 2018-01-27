public class ArrayType extends Type {
    Type baseType;
    int size;

    public ArrayType(Type baseType, String sizeBil) {
        this.baseType = baseType;
		this.size = Integer.parseInt(sizeBil.substring(1, sizeBil.length()-1));
		if (this.size < 0)
			this.size = 0;
	}
	
	public ArrayType(Type baseType, int size) {
        this.baseType = baseType;
        this.size = size;
    }
    
	public int size() {
		return size * baseType.size();
	}

	@Override
	public boolean equals(Object other) {
		if(other instanceof ArrayType)
			return ((ArrayType)other).getSize() == size && ((ArrayType)other).getBaseType().equals(baseType);
		return false;
	}

	@Override
	public String toString() {
		return "array " + size + " of " + baseType.toString();
    }
    
    public int getSize() { return size; }
    public Type getBaseType() { return baseType; }
}