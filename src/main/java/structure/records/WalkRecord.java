package structure.records;

import structure.TravelVisitor;

public class WalkRecord implements ConnectionRecord{
    private int costMinutes;

    public int getCostMinutes() {
        return costMinutes;
    }



    public WalkRecord(int costMinutes){
        this.costMinutes = costMinutes;
    }


    @Override
    public void acceptVisitor(TravelVisitor visitor) {

    }

}
