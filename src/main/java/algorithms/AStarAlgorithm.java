package algorithms;

public class AStarAlgorithm extends BaseAlgorithm{
    @Override
    public int getCostTotal(int costTravel, int heuristicCost) {
        return costTravel +heuristicCost;
    }
}
