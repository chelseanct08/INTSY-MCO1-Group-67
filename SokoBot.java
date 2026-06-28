package solver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

public class SokoBot {

  private static final int HEURISTIC_WEIGHT = 2;

  public String solveSokobanPuzzle(int width, int height, char[][] mapData, char[][] itemsData) {
    this.mapData = mapData;
    this.itemsData = itemsData;
    this.goalPositions = getGoalPosition(mapData);
        // precompute goal distance maps for heuristic (BFS per goal)
        int rows = mapData.length;
        int cols = mapData[0].length;
        goalDistanceMaps = new ArrayList<>();
        for (int[] g : goalPositions) {
            goalDistanceMaps.add(bfsDistanceMap(g[0], g[1], rows, cols));
        }
        computeDeadSquares();

    int[] playerPos = getPlayerPosition(itemsData);
    ArrayList<int[]> posCrates = getCratePosition(itemsData);

    if (playerPos == null || posCrates.isEmpty()) {
      return "";
    }

    State startState = new State(playerPos, posCrates, 0, 0, null, "", mapData, itemsData);
        computeReachableParents(startState);
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

                int tempGCost = neighbor.getPathCost();
                Integer previousBest = bestGCosts.get(neighborKey);
                if (previousBest != null && tempGCost >= previousBest) {
                    continue;
                }

                neighbor.setHeuristicCost(computeHeuristic(neighbor.getCrateCoordinates(), goalPositions));
                neighbor.setPreviousState(current);
                bestGCosts.put(neighborKey, tempGCost);
                open.add(neighbor);
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

        // Use precomputed BFS distance maps per goal.
        List<int[][]> goalDists = goalDistanceMaps;
        if (goalDists == null || goalDists.size() != posGoals.size()) {
            return 0;
        }

        boolean[] assignedGoals = new boolean[posGoals.size()];
        int totalHeuristic = 0;

        for (int[] crate : posCrates) {
            int bestDistance = Integer.MAX_VALUE;
            int bestGoalIndex = -1;

            for (int i = 0; i < goalDists.size(); i++) {
                if (assignedGoals[i]) continue;
                int[][] dist = goalDists.get(i);
                int d = dist[crate[0]][crate[1]];
                if (d >= 0 && d < bestDistance) {
                    bestDistance = d;
                    bestGoalIndex = i;
                }
            }

            if (bestGoalIndex != -1) {
                assignedGoals[bestGoalIndex] = true;
                totalHeuristic += bestDistance;
            } else {
                // fallback: pick minimal reachable distance ignoring assignment
                int fallback = Integer.MAX_VALUE;
                for (int[][] dist : goalDists) {
                    int d = dist[crate[0]][crate[1]];
                    if (d >= 0 && d < fallback) fallback = d;
                }
                if (fallback == Integer.MAX_VALUE) return Integer.MAX_VALUE / 4;
                totalHeuristic += fallback;
            }
        }
        return totalHeuristic;
    }

    private int[][] bfsDistanceMap(int sx, int sy, int rows, int cols) {
        int[][] dist = new int[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) dist[i][j] = Integer.MAX_VALUE;
        }

        Queue<int[]> q = new LinkedList<>();
        if (mapData[sx][sy] == '#') return dist; // unreachable goal (shouldn't happen)
        dist[sx][sy] = 0;
        q.add(new int[]{sx, sy});

        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};

        while (!q.isEmpty()) {
            int[] cur = q.poll();
            int cx = cur[0];
            int cy = cur[1];
            for (int k = 0; k < 4; k++) {
                int nx = cx + dx[k];
                int ny = cy + dy[k];
                if (nx < 0 || nx >= rows || ny < 0 || ny >= cols) continue;
                if (mapData[nx][ny] == '#') continue;
                if (dist[nx][ny] > dist[cx][cy] + 1) {
                    dist[nx][ny] = dist[cx][cy] + 1;
                    q.add(new int[]{nx, ny});
                }
            }
        }

        // convert unreachable markers
        for (int i = 0; i < rows; i++) for (int j = 0; j < cols; j++) if (dist[i][j] == Integer.MAX_VALUE) dist[i][j] = -1;
        return dist;
    }

    private String createStateKey(State state) {
        int[] canon = state.getCanonicalPlayerCoordinates();
        int[] playerPosition = canon != null ? canon : state.getPlayerCoordinates();
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
        int[][] parentDir = computeReachableParents(currentState);
        if (parentDir == null) {
            return nextStates;
        }

        int rows = mapData.length;
        int cols = mapData[0].length;
        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};
        char[] moveChars = {'u', 'd', 'l', 'r'};

        HashSet<String> crateSet = new HashSet<>();
        for (int[] crate : currentState.getCrateCoordinates()) {
            crateSet.add(crate[0] + "," + crate[1]);
        }

        for (int[] crate : currentState.getCrateCoordinates()) {
            int crateX = crate[0];
            int crateY = crate[1];

            for (int i = 0; i < 4; i++) {
                int playerPushX = crateX - dx[i];
                int playerPushY = crateY - dy[i];
                int newCrateX = crateX + dx[i];
                int newCrateY = crateY + dy[i];

                if (playerPushX < 0 || playerPushX >= rows
                        || playerPushY < 0 || playerPushY >= cols
                        || newCrateX < 0 || newCrateX >= rows
                        || newCrateY < 0 || newCrateY >= cols) {
                    continue;
                }

                if (parentDir[playerPushX][playerPushY] == -1) {
                    continue;
                }

                if (mapData[newCrateX][newCrateY] == '#' || crateSet.contains(newCrateX + "," + newCrateY)) {
                    continue;
                }

                String pathToPush = buildPathFromParents(parentDir, currentState.getPlayerCoordinates(), playerPushX, playerPushY, dx, dy, moveChars);
                String moveSequence = pathToPush + moveChars[i];

                ArrayList<int[]> newCrates = new ArrayList<>();
                for (int[] existingCrate : currentState.getCrateCoordinates()) {
                    if (existingCrate[0] == crateX && existingCrate[1] == crateY) {
                        newCrates.add(new int[]{newCrateX, newCrateY});
                    } else {
                        newCrates.add(new int[]{existingCrate[0], existingCrate[1]});
                    }
                }

                // push-based cost: each crate push costs 1 (walking steps not counted in g)
                int newGCost = currentState.getPathCost() + 1;
                int newHCost = computeHeuristic(newCrates, goalPositions);
                int[] newPlayerPos = new int[]{crateX, crateY};
                State nextState = new State(newPlayerPos, newCrates, newGCost, newHCost, currentState, moveSequence, mapData, itemsData);
                computeReachableParents(nextState);
                nextStates.add(nextState);
            }
        }

        return nextStates;
    }

    private String buildPathFromParents(int[][] parentDir, int[] start, int tx, int ty, int[] dx, int[] dy, char[] moveChars) {
        StringBuilder sb = new StringBuilder();
        int cx = tx;
        int cy = ty;
        while (!(cx == start[0] && cy == start[1])) {
            int dir = parentDir[cx][cy];
            if (dir < 0) { // unreachable
                return "";
            }
            sb.insert(0, moveChars[dir]);
            cx -= dx[dir];
            cy -= dy[dir];
        }
        return sb.toString();
    }

    private int[][] computeReachableParents(int[] start, ArrayList<int[]> crates) {
        int rows = mapData.length;
        int cols = mapData[0].length;
        int[][] parentDir = new int[rows][cols];
        boolean[][] visited = new boolean[rows][cols];
        boolean[][] crateBlocked = new boolean[rows][cols];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                parentDir[i][j] = -1; // -1 = unreachable/unvisited
            }
        }

        for (int[] crate : crates) {
            crateBlocked[crate[0]][crate[1]] = true;
        }

        Queue<int[]> queue = new LinkedList<>();
        queue.add(start);
        visited[start[0]][start[1]] = true;
        parentDir[start[0]][start[1]] = -2; // start marker

        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};

        while (!queue.isEmpty()) {
            int[] current = queue.poll();

            for (int i = 0; i < 4; i++) {
                int nextX = current[0] + dx[i];
                int nextY = current[1] + dy[i];

                if (nextX < 0 || nextX >= rows
                        || nextY < 0 || nextY >= cols
                        || visited[nextX][nextY]
                        || mapData[nextX][nextY] == '#'
                        || crateBlocked[nextX][nextY]) {
                    continue;
                }

                visited[nextX][nextY] = true;
                parentDir[nextX][nextY] = i; // reached by move i from parent
                queue.add(new int[]{nextX, nextY});
            }
        }

        return parentDir;
    }

    private int[][] computeReachableParents(State state) {
        int[][] parentDir = computeReachableParents(state.getPlayerCoordinates(), state.getCrateCoordinates());
        if (parentDir == null) {
            return null;
        }

        int[] canonicalPlayer = state.getPlayerCoordinates();
        for (int i = 0; i < parentDir.length; i++) {
            for (int j = 0; j < parentDir[0].length; j++) {
                if (parentDir[i][j] != -1) {
                    if (canonicalPlayer == null
                            || i < canonicalPlayer[0]
                            || (i == canonicalPlayer[0] && j < canonicalPlayer[1])) {
                        canonicalPlayer = new int[]{i, j};
                    }
                }
            }
        }
        state.setCanonicalPlayerCoordinates(canonicalPlayer);
        return parentDir;
    }

        private void computeDeadSquares() {
            int rows = mapData.length;
            int cols = mapData[0].length;
            deadSquares = new boolean[rows][cols];

            // reverse BFS from goals: mark positions from which a box CAN reach a goal
            boolean[][] reachable = new boolean[rows][cols];
            Queue<int[]> q = new LinkedList<>();
            for (int[] g : goalPositions) {
                reachable[g[0]][g[1]] = true;
                q.add(new int[]{g[0], g[1]});
            }

            int[] dx = {-1, 1, 0, 0};
            int[] dy = {0, 0, -1, 1};

            while (!q.isEmpty()) {
                int[] cur = q.poll();
                int cx = cur[0];
                int cy = cur[1];

                for (int d = 0; d < 4; d++) {
                    int prevX = cx - dx[d];
                    int prevY = cy - dy[d];
                    int playerX = cx - 2 * dx[d];
                    int playerY = cy - 2 * dy[d];

                    if (prevX < 0 || prevX >= rows || prevY < 0 || prevY >= cols) continue;
                    if (playerX < 0 || playerX >= rows || playerY < 0 || playerY >= cols) continue;
                    if (mapData[prevX][prevY] == '#') continue;
                    if (mapData[playerX][playerY] == '#') continue;
                    if (reachable[prevX][prevY]) continue;

                    reachable[prevX][prevY] = true;
                    q.add(new int[]{prevX, prevY});
                }
            }

            // deadSquares = floor AND not reachable AND not goal
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    deadSquares[i][j] = (mapData[i][j] != '#') && !reachable[i][j] && !goalSet.contains(i + "," + j);
                }
            }
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

                // dead square check: if crate on a square that can never reach any goal
                if (deadSquares != null && deadSquares[x][y]) {
                    return true;
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

        List<State> path = new ArrayList<>();
        State current = goalState;

        while (current != null) {
            path.add(current);
            current = current.getPreviousState();
        }

        Collections.reverse(path);
        StringBuilder moves = new StringBuilder();

        for (int i = 1; i < path.size(); i++) {
            String sequence = path.get(i).getMoveSequence();
            if (sequence != null) {
                moves.append(sequence);
            }
        }

        return moves.toString();
    }
    private HashSet<String> goalSet;
    private char[][] mapData;
    private char[][] itemsData;
    private ArrayList<int[]> goalPositions;
    private boolean[][] deadSquares;
    private List<int[][]> goalDistanceMaps;
}