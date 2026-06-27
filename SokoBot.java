package solver;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;

public class SokoBot {

  private static final int HEURISTIC_WEIGHT = 2;

  public String solveSokobanPuzzle(int width, int height, char[][] mapData, char[][] itemsData) {
    this.mapData = mapData;
    this.itemsData = itemsData;
    this.goalPositions = getGoalPosition(mapData);

    int[] playerPos = getPlayerPosition(itemsData);
    ArrayList<int[]> posCrates = getCratePosition(itemsData);

    if (playerPos == null || posCrates.isEmpty()) {
      return "";
    }

    State startState = new State(playerPos, posCrates, 0, 0, null, mapData, itemsData);
    State goalState = aStarSearch(startState);
    return constructPath(goalState);
  }

    public State aStarSearch(State startState) {
        PriorityQueue<State> open = new PriorityQueue<>(Comparator.comparingInt(state ->
                state.getPathCost() + HEURISTIC_WEIGHT * state.getHeuristicCost()));
        HashSet<String> closed = new HashSet<>();
        HashMap<String, Integer> bestGCosts = new HashMap<>();

        startState.setHeuristicCost(computeHeuristic(startState.getCrateCoordinates(), goalPositions));
        open.add(startState);
        bestGCosts.put(createStateKey(startState), 0);

        while (!open.isEmpty()) {
            State current = open.poll();
            String stateKey = createStateKey(current);
            Integer bestG = bestGCosts.get(stateKey);

            if (bestG == null || current.getPathCost() != bestG) {
                continue;
            }
            if (closed.contains(stateKey)) {
                continue;
            }
            closed.add(stateKey);

            if (isGoalState(current)) {
                return current;
            }

            for (State neighbor : getPossibleMoves(current)) {
                if (neighbor == null || isDeadlock(neighbor)) {
                    continue;
                }

                String neighborKey = createStateKey(neighbor);
                if (closed.contains(neighborKey)) {
                    continue;
                }

                int tempGCost = current.getPathCost() + 1;
                Integer previousBest = bestGCosts.get(neighborKey);
                if (previousBest != null && tempGCost >= previousBest) {
                    continue;
                }

                neighbor.setPathCost(tempGCost);
                neighbor.setHeuristicCost(computeHeuristic(neighbor.getCrateCoordinates(), goalPositions));
                neighbor.setPreviousState(current);
                bestGCosts.put(neighborKey, tempGCost);
                open.add(neighbor);
            }
        }
        return null;
    }

    public State GBFS(State startState) {
        PriorityQueue<State> open = new PriorityQueue<>(Comparator.comparingInt(State::getHeuristicCost));
        HashSet<State> closed = new HashSet<>();
        HashSet<State> openSet = new HashSet<>();

        startState.setHeuristicCost(computeHeuristic(startState.getCrateCoordinates(), goalPositions));
        open.add(startState);
        openSet.add(startState);

        while (!open.isEmpty()) {
            // get the node with the lowest f cost in open list
            State current = open.poll();
            openSet.remove(current);
            closed.add(current);

            // path has been found
            if (isGoalState(current)) {
                return current;
            }

            // check all of its neighboring nodes
            for (State neighbor : getPossibleMoves(current)) {
                // State neighbor = applyMove(current, move, goalPositions);
                if (neighbor == null) {
                    continue;
                }
                if (isDeadlock(neighbor) || closed.contains(neighbor)) {
                    continue; // skip to next neighbor
                }

                if (!openSet.contains(neighbor)) {
                    neighbor.setHeuristicCost(computeHeuristic(neighbor.getCrateCoordinates(), goalPositions));
                    neighbor.setPreviousState(current);

                    if (!openSet.contains(neighbor)) {
                        open.add(neighbor);
                        openSet.add(neighbor);
                    }
                }
            }
        }
        return null;
    }

    public ArrayList<int[]> getCratePosition(char[][] itemsData) {
        ArrayList<int[]> posCrates = new ArrayList<>();
        for (int i = 0; i < itemsData.length; i++) {
            for (int j = 0; j < itemsData[0].length; j++) {
                if (itemsData[i][j] == '$') {
                    posCrates.add(new int[]{i, j});
                }
            }
        }
        return posCrates;
    }

    public ArrayList<int[]> getGoalPosition(char[][] mapData) {
        ArrayList<int[]> posGoals = new ArrayList<>();
        goalSet = new HashSet<>();

        for (int i = 0; i < mapData.length; i++) {
            for (int j = 0; j < mapData[0].length; j++) {
                if (mapData[i][j] == '.') {
                    posGoals.add(new int[]{i, j});
                    goalSet.add(i + "," + j);
                }
            }
        }
        return posGoals;
    }

    public boolean isGoalState(State currentState) {
        ArrayList<int[]> crates = currentState.getCrateCoordinates();

        for (int[] crate : crates) {
            if (!goalSet.contains(crate[0] + "," + crate[1])) {
                return false;
            }
        }
        return true;
    }

    public int computeHeuristic(ArrayList<int[]> posCrates, ArrayList<int[]> posGoals) {
        if (posCrates == null || posCrates.isEmpty() || posGoals == null || posGoals.isEmpty()) {
            return 0;
        }

        boolean[] assignedGoals = new boolean[posGoals.size()];
        int totalHeuristic = 0;

        for (int[] crate : posCrates) {
            int bestDistance = Integer.MAX_VALUE;
            int bestGoalIndex = -1;

            for (int i = 0; i < posGoals.size(); i++) {
                if (assignedGoals[i]) {
                    continue;
                }

                int[] goal = posGoals.get(i);
                int distance = Math.abs(goal[0] - crate[0]) + Math.abs(goal[1] - crate[1]);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestGoalIndex = i;
                }
            }

            if (bestGoalIndex != -1) {
                assignedGoals[bestGoalIndex] = true;
                totalHeuristic += bestDistance;
            } else {
                int fallbackDistance = Integer.MAX_VALUE;
                for (int[] goal : posGoals) {
                    int distance = Math.abs(goal[0] - crate[0]) + Math.abs(goal[1] - crate[1]);
                    if (distance < fallbackDistance) {
                        fallbackDistance = distance;
                    }
                }
                totalHeuristic += fallbackDistance;
            }
        }
        return totalHeuristic;
    }

    private String createStateKey(State state) {
        int[] playerPosition = state.getPlayerCoordinates();
        ArrayList<int[]> crates = state.getCrateCoordinates();
        ArrayList<String> cratePositions = new ArrayList<>();

        for (int[] crate : crates) {
            cratePositions.add(crate[0] + "," + crate[1]);
        }
        cratePositions.sort(String::compareTo);

        StringBuilder builder = new StringBuilder();
        builder.append(playerPosition[0]).append(',').append(playerPosition[1]).append('|');
        for (String cratePosition : cratePositions) {
            builder.append(cratePosition).append(';');
        }
        return builder.toString();
    }

    public int[] getPlayerPosition(char[][] itemsData) {
        for (int i = 0; i < itemsData.length; i++) {
            for (int j = 0; j < itemsData[0].length; j++) {
                if (itemsData[i][j] == '@') {
                    return new int[]{i, j};
                }
            }
        }
        return null; // player not found
    }

    public List<State> getPossibleMoves(State currentState) {
        List<State> nextStates = new ArrayList<>();

        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};

        int[] playerPos = currentState.getPlayerCoordinates();
        int playerX = playerPos[0];
        int playerY = playerPos[1];

        for (int i = 0; i < 4; i++) {
            int newPlayerX = playerX + dx[i];
            int newPlayerY = playerY + dy[i];

            // check boundaries
            if (newPlayerX < 0 || newPlayerX >= mapData.length
                    || newPlayerY < 0 || newPlayerY >= mapData[0].length) {
                continue;
            }

            // checks if there's a wall, skip if yes
            if (mapData[newPlayerX][newPlayerY] == '#') {
                continue;
            }

            boolean invalidMove = false;
            ArrayList<int[]> newCrates = new ArrayList<>();

            // crate movement
            for (int[] crate : currentState.getCrateCoordinates()) {
                int crateX = crate[0];
                int crateY = crate[1];

                // if player tries to push a crate
                if (newPlayerX == crateX && newPlayerY == crateY) {
                    int newCrateX = crateX + dx[i];
                    int newCrateY = crateY + dy[i];

                    // check the boundaries before accessing
                    if (newCrateX < 0 || newCrateX >= mapData.length
                            || newCrateY < 0 || newCrateY >= mapData[0].length) {
                        invalidMove = true;
                    }// if invalid push (wall/another crate)
                    else if (mapData[newCrateX][newCrateY] == '#'
                            || containsCrate(currentState.getCrateCoordinates(), newCrateX, newCrateY)) {
                        invalidMove = true;
                    }

                    if (invalidMove) {
                        break;
                    }

                    // if valid push
                    newCrates.add(new int[]{newCrateX, newCrateY});
                } else {
                    newCrates.add(new int[]{crateX, crateY});
                }
            }

            if (invalidMove) {
                continue;
            }
            // recompute heuristic
            int newGCost = currentState.getPathCost() + 1;
            int newHCost = computeHeuristic(newCrates, goalPositions);

            // create new state
            int[] newPlayerPos = new int[]{newPlayerX, newPlayerY};
            State nextState = new State(newPlayerPos, newCrates, newGCost, newHCost, currentState, mapData, itemsData);

            nextStates.add(nextState);
        }

        nextStates.sort(Comparator.comparingInt(State::getHeuristicCost));
        return nextStates;
    }

    private boolean containsCrate(ArrayList<int[]> crates, int x, int y) {
        for (int[] crate : crates) {
            if (crate[0] == x && crate[1] == y) {
                return true;
            }
        }
        return false;
    }

    public boolean isDeadlock(State currentState) {
        ArrayList<int[]> crates = currentState.getCrateCoordinates();

        // hashset for quick crate lookup
        HashSet<String> crateSet = new HashSet<>();
        for (int[] crate : crates) {
            crateSet.add(crate[0] + "," + crate[1]);
        }

        for (int[] crate : crates) {
            int x = crate[0];
            int y = crate[1];

            // skip crates already on a goal
            if (goalSet.contains(x + "," + y)) {
                continue;
            }

            boolean wallUp = (x == 0) || mapData[x - 1][y] == '#';
            boolean wallDown = (x == mapData.length - 1) || mapData[x + 1][y] == '#';
            boolean wallLeft = (y == 0) || mapData[x][y - 1] == '#';
            boolean wallRight = (y == mapData[0].length - 1) || mapData[x][y + 1] == '#';

            // checks for adjacent crates
            boolean crateUp = (x > 0) && crateSet.contains((x - 1) + "," + y);
            boolean crateDown = (x < mapData.length - 1) && crateSet.contains((x + 1) + "," + y);
            boolean crateLeft = (y > 0) && crateSet.contains(x + "," + (y - 1));
            boolean crateRight = (y < mapData[0].length - 1) && crateSet.contains(x + "," + (y + 1));

            // corner deadlock - crate stuck in a corner
            if ((wallUp && wallLeft) || (wallUp && wallRight)
                    || (wallDown && wallLeft) || (wallDown && wallRight)) {
                return true;
            }

            // freeze deadlock - crate completely immobilized
            boolean horizontallyTrapped = ((wallLeft || crateLeft) && (wallRight || crateRight));
            boolean verticallyTrapped = ((wallUp || crateUp) && (wallDown || crateDown));

            if (horizontallyTrapped && verticallyTrapped) {
                return true;
            }
        }
        return false;
    }

    public String constructPath(State goalState) {
        if (goalState == null) {
            return ""; // no solution
        }

        // kukunin lahat ng states from goal to start
        List<State> path = new ArrayList<>();
        State current = goalState;

        while (current != null) {
            path.add(current);
            current = current.getPreviousState();
        }

        String moves = "";

        // babalikan yung path from start to goal
        for (int i = path.size() - 2; i >= 0; i--) {
            State from = path.get(i + 1);  // last state
            State to = path.get(i);        // next state

            int[] fromPos = from.getPlayerCoordinates();
            int[] toPos = to.getPlayerCoordinates();

            int deltaX = toPos[0] - fromPos[0];
            int deltaY = toPos[1] - fromPos[1];

            // add move 
            if (deltaX == -1 && deltaY == 0) {
                moves = moves + 'u';  // Up
            } else if (deltaX == 1 && deltaY == 0) {
                moves = moves + 'd';  // Down
            } else if (deltaX == 0 && deltaY == -1) {
                moves = moves + 'l';  // Left
            } else if (deltaX == 0 && deltaY == 1) {
                moves = moves + 'r';  // Right
            }
        }

        return moves;
    }
    private HashSet<String> goalSet;
    private char[][] mapData;
    private char[][] itemsData;
    private ArrayList<int[]> goalPositions;
}