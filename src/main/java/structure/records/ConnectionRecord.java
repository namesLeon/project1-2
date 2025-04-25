package structure.records;

import structure.TravelVisitor;

public interface ConnectionRecord {
    void acceptVisitor(TravelVisitor visitor);
}
