package structure.records;

import structure.visitor.TravelVisitor;

public class WalkRecord implements ConnectionRecord{
    private int costMinutes;

    @Override
    public int getTotalCost(){
        return costMinutes;
    }



    public WalkRecord(int costMinutes){
        this.costMinutes = costMinutes;
    }


    @Override
    public void acceptVisitor(TravelVisitor visitor) {

    }

    @Override
    public String toString(){
        return "Walk Record - walk : "+costMinutes;
    }

}
