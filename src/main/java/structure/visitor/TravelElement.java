package structure.visitor;

public interface TravelElement {
    void acceptVisitor(TravelVisitor visitor);
}
