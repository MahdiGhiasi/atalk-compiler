public class SymbolTableActorItem extends SymbolTableItem {
	
	public SymbolTableActorItem(Actor actor) {
		super();
		this.actor = actor;
	}

	public int getCapacity() {
		return actor.getCapacity();
	}

	public Actor getActor() {
		return actor;
	}

	@Override
	public String getKey() {
		return actor.getName();
	}

	@Override
    public String toString() {
        return "Actor: " + actor.toString();
    }

	Actor actor;
}