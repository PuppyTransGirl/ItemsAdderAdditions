package toutouchien.itemsadderadditions.clientcreative;

public enum TabBucket {
    COMPLEX_FURNITURE("complex_furniture", "Complex Furniture"),
    FURNITURE("furniture", "Furniture"),
    BLOCK("block", "Block"),
    ITEM("item", "Item");

    public final String id;
    public final String displayName;

    TabBucket(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }
}
