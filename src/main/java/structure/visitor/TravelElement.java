package structure.visitor;

public interface TravelElement {
    void accept(TravelVisitor visitor);
}
