package solver;

import java.util.ArrayList;
import java.util.Arrays;

public class State {
    private final int[] playerCoordinates;
    private final ArrayList<int[]> crateCoordinates;
    private int pathCost;
    private int heuristicCost;
    private State previousState;
    private final char[][] itemLayout;
    private final char[][] mapLayout;

    public State(char[][] mapData, char[][] itemsData) {
        this(new int[2], new ArrayList<>(), 0, 0, null, mapData, itemsData);
    }

    public State(int[] playerPosition, ArrayList<int[]> cratePositions, int gCost, int hCost,
                 State parent, char[][] mapData, char[][] itemsData) {
        this.playerCoordinates = playerPosition;
        this.crateCoordinates = cratePositions;
        this.pathCost = gCost;
        this.heuristicCost = hCost;
        this.previousState = parent;
        this.itemLayout = itemsData;
        this.mapLayout = mapData;
    }

    public int[] getPlayerCoordinates() {
        return playerCoordinates;
    }

    public ArrayList<int[]> getCrateCoordinates() {
        return crateCoordinates;
    }

    public int getPathCost() {
        return pathCost;
    }

    public int getHeuristicCost() {
        return heuristicCost;
    }

    public int getCombinedCost() {
        return pathCost + heuristicCost;
    }

    public State getPreviousState() {
        return previousState;
    }

    public char[][] getMapLayout() {
        return mapLayout;
    }

    public char[][] getItemLayout() {
        return itemLayout;
    }

    public void setPlayerCoordinates(int[] playerPosition) {
        this.playerCoordinates[0] = playerPosition[0];
        this.playerCoordinates[1] = playerPosition[1];
    }

    public void setCrateCoordinates(ArrayList<int[]> cratePositions) {
        this.crateCoordinates.clear();
        this.crateCoordinates.addAll(cratePositions);
    }

    public void setPathCost(int pathCost) {
        this.pathCost = pathCost;
    }

    public void setHeuristicCost(int heuristicCost) {
        this.heuristicCost = heuristicCost;
    }

    public void setPreviousState(State previousState) {
        this.previousState = previousState;
    }

    public int[] getPlayerPosition() {
        return getPlayerCoordinates();
    }

    public ArrayList<int[]> getCratePositions() {
        return getCrateCoordinates();
    }

    public int getGCost() {
        return getPathCost();
    }

    public int getHCost() {
        return getHeuristicCost();
    }

    public int getFCost() {
        return getCombinedCost();
    }

    public State getParent() {
        return getPreviousState();
    }

    public char[][] getMapData() {
        return getMapLayout();
    }

    public char[][] getItemsData() {
        return getItemLayout();
    }

    public void setPlayerPosition(int[] playerPosition) {
        setPlayerCoordinates(playerPosition);
    }

    public void setCratePositions(ArrayList<int[]> cratePositions) {
        setCrateCoordinates(cratePositions);
    }

    public void setGCost(int gCost) {
        setPathCost(gCost);
    }

    public void setHCost(int hCost) {
        setHeuristicCost(hCost);
    }

    public void setParent(State parent) {
        setPreviousState(parent);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof State)) {
            return false;
        }

        State other = (State) obj;
        if (!Arrays.equals(playerCoordinates, other.playerCoordinates)) {
            return false;
        }
        if (crateCoordinates.size() != other.crateCoordinates.size()) {
            return false;
        }

        ArrayList<String> thisCrates = normalizeCrateCoordinates(crateCoordinates);
        ArrayList<String> otherCrates = normalizeCrateCoordinates(other.crateCoordinates);
        return thisCrates.equals(otherCrates);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(playerCoordinates);
        for (String crate : normalizeCrateCoordinates(crateCoordinates)) {
            result = 31 * result + crate.hashCode();
        }
        return result;
    }

    private ArrayList<String> normalizeCrateCoordinates(ArrayList<int[]> crates) {
        ArrayList<String> normalized = new ArrayList<>();
        for (int[] crate : crates) {
            normalized.add(crate[0] + "," + crate[1]);
        }
        normalized.sort(String::compareTo);
        return normalized;
    }
}