package structure.records;

import structure.visitor.TravelVisitor;

public interface ConnectionRecord {
    void acceptVisitor(TravelVisitor visitor);

    int getTotalCost();
}
