import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class MyBot {

    // constants used throughout the game
    final static double enemyToEmptyRatio = 1.5;
    final static int productionGrowthFactor = 5;
    final static double goodRatio = 1.25;
    final static int minimumStrengthForAttack = 20;
    final static int maximumStrength = 255;

    public static void main(String[] args) {

        final InitPackage iPackage = Networking.getInit();
        final int myID = iPackage.myID;
        final GameMap gameMap = iPackage.map;

        // calculate the average strength and average production of the map, so that we can
        // decide when it is worth attacking a site
        int mapAverageStrength = getMapAverageStrength(gameMap);
        int mapAverageProduction = getMapAverageProduction(gameMap);

        // send the message that our bot is ready to the environment
        Networking.sendInit("First Force Bot");

        while(true) {
            List<Move> moves = new ArrayList<>();

            // get the new state of the game
            Networking.updateFrame(gameMap);

            // determine the current positions of the enemy / enemies
            List<Location> enemyPositions = getEnemyPositions(myID, gameMap);

            // determine the current empty squares (that do not belong to anyone)
            // and choose only the valuable ones
            List<Location> emptyLocations = getEmptyLocations(gameMap, mapAverageStrength, mapAverageProduction);

            // queue used so that the moves are decided from outside to inside
            Queue<Location> queue = new LinkedList<>();

            // matrix used to determine which sites were visited this round
            boolean[][] visited = new boolean[gameMap.width][gameMap.height];

            // start with the outer sites
            for (int y = 0; y < gameMap.height; y++) {
                for (int x = 0; x < gameMap.width; x++) {
                    final Location location = gameMap.getLocation(x, y);
                    final Site site = location.getSite();

                    if(site.owner == myID && location.isOuter(myID, gameMap)) {
                        moves.add(decideMove(location, gameMap, enemyPositions, emptyLocations, myID, moves));
                        // add all neighboring unvisited sites, which are inner sites, inside the queue, and mark
                        // this location in the visited matrix
                        visited[x][y] = true;
                        for (Direction dir : Direction.CARDINALS) {
                            Location neighbor = gameMap.getLocation(location, dir);
                            if (neighbor.getSite().owner == myID && !neighbor.isOuter(myID, gameMap) && !visited[neighbor.x][neighbor.y]) {
                                queue.add(neighbor);
                                visited[neighbor.x][neighbor.y] = true;
                            }
                        }
                    }
                }
            }

            // determine the moves for the inner sites
            while (!queue.isEmpty()) {
                Location location = queue.poll();
                Move move = decideMove(location, gameMap, enemyPositions, emptyLocations, myID, moves);
                moves.add(move);
                if (move.dir != Direction.STILL) {
                    int currentStrength = location.getSite().strength;
                    gameMap.getLocations()[move.loc.x][move.loc.y].getSite().strength = 0;
                    gameMap.getContents()[move.loc.x][move.loc.y].strength = 0;
                    Location dest = gameMap.getLocation(move.loc, move.dir);
                    gameMap.getLocations()[dest.x][dest.y].getSite().strength = Math.min(dest.getSite().strength + currentStrength, maximumStrength);
                    gameMap.getContents()[dest.x][dest.y].strength = Math.min(dest.getSite().strength + currentStrength, maximumStrength);
                }

                // add the current site's unvisited neighbors in the queue
                for (Direction dir : Direction.CARDINALS) {
                    Location neighbor = gameMap.getLocation(location, dir);
                    if (!neighbor.isOuter(myID, gameMap) && !visited[neighbor.x][neighbor.y]) {
                        queue.add(neighbor);
                        visited[neighbor.x][neighbor.y] = true;
                    }
                }
            }

            // send the moves to the environment
            Networking.sendFrame(moves);
        }
    }

    // determine the move for a location
    private static Move decideMove(final Location location, final GameMap gameMap,
                                   final List<Location> enemyPositions,
                                   final List<Location> emptyLocations, final int myID,
                                   final List<Move> moves) {
        final Site site = location.getSite();
        Location nearestEnemy = location.nearestLocation(enemyPositions, gameMap);
        Location nearestEmptySpace = location.nearestLocation(emptyLocations, gameMap);
        // calculate distance to these locations
        double distanceToEnemy = gameMap.getDistance(nearestEnemy, location);
        double distanceToEmptySpace = gameMap.getDistance(nearestEmptySpace, location);

        Direction chosenDirection = null;
        // decide which of the two possible moves is better
        chosenDirection = (distanceToEmptySpace < enemyToEmptyRatio * distanceToEnemy) ?
                getDirection(location, nearestEmptySpace, gameMap, moves) :
                getDirection(location, nearestEnemy, gameMap, moves);

        // check if it would be better to stay still
        Location move = gameMap.getLocation(location, chosenDirection);

        // it is better to stay still if:
        // - current location has a very low strength
        // - the site cannot be conquered
        // - not enough strength was gain in this location
        if (site.strength < minimumStrengthForAttack || site.strength < site.production * productionGrowthFactor ||
                (move.getSite().owner != myID && move.getSite().strength >= site.strength)) {
            // it is better to stay still
            chosenDirection = Direction.STILL;
        }

        return new Move(location, chosenDirection);
    }

    // determine the average strength of a game map
    private static int getMapAverageStrength(final GameMap gameMap) {
        double totalStrength = 0.0;
        for (int i = 0; i < gameMap.width; i++) {
            for (int j = 0; j < gameMap.height; j++) {
                Location currentLoc = gameMap.getLocation(i, j);
                totalStrength += currentLoc.getSite().strength;
            }
        }

        int numberOfSquares = gameMap.height * gameMap.width;

        if ((int) totalStrength % numberOfSquares == 0) {
            return (int) Math.ceil(totalStrength / numberOfSquares) + 1;
        }

        return (int) Math.ceil(totalStrength / numberOfSquares);
    }

    // determine the average production of a game map
    private static int getMapAverageProduction(final GameMap gameMap) {
        double totalProduction = 0.0;
        for (int i = 0; i < gameMap.width; i++) {
            for (int j = 0; j < gameMap.height; j++) {
                Location currentLoc = gameMap.getLocation(i, j);
                totalProduction += currentLoc.getSite().production;
            }
        }

        int numberOfSquares = gameMap.height * gameMap.width;

        if ((int) totalProduction % numberOfSquares == 0) {
            return (int) Math.ceil(totalProduction / numberOfSquares) + 1;
        }

        return (int) Math.ceil(totalProduction / numberOfSquares);
    }

    // return the location that belong to an enemy in a specific state
    private static List<Location> getEnemyPositions(int myID, GameMap gameMap) {
        List<Location> enemies = new ArrayList<>();

        // search sites with owner ID that differs from myID
        for (int i = 0; i < gameMap.width; i++) {
            for (int j = 0; j < gameMap.height; j++) {
                Location currentLoc = gameMap.getLocation(i, j);
                int siteOwnerID = currentLoc.getSite().owner;

                if (siteOwnerID != myID && siteOwnerID != 0) {
                    // site belongs to an enemy
                    enemies.add(currentLoc);
                }
            }
        }

        return enemies;
    }

    // return the location that do not belong to anyone in a specific state
    private static List<Location> getEmptyLocations(final GameMap gameMap, int mapAverageStrength, int mapAverageProduction) {
        List<Location> emptyLocations = new ArrayList<>();

        // determine the average value of a game site
        double averageValue = (double) mapAverageProduction / mapAverageStrength;

        // search locations that are neutral and are worth taking (have a greater value than the average)
        for (int i = 0; i < gameMap.width; i++) {
            for (int j = 0; j < gameMap.height; j++) {
                Location currentLoc = gameMap.getLocation(i, j);
                if (currentLoc.isNeutral() &&
                        currentLoc.locationValue(gameMap) > goodRatio * averageValue) {
                    emptyLocations.add(currentLoc);
                }
            }
        }

        return emptyLocations;
    }

    // return the best direction to go to in order to reach target, starting from source
    private static Direction getDirection(Location source, Location target, final GameMap gameMap,
                                          final List<Move> moves) {
        // get original distance between source and target
        double prevDistance = gameMap.getDistance(source, target);

        // look at all directions that improve the initial distance
        List<Direction> candidateDirections = new ArrayList<>();
        for (Direction dir : Direction.CARDINALS) {
            if (gameMap.getDistance(target, gameMap.getLocation(source, dir)) < prevDistance) {
                candidateDirections.add(dir);
            }
        }

        // choose the direction that moves us to the most valuable site
        Direction bestDirection = candidateDirections.get(0);

        for (int i = 1; i < candidateDirections.size(); i++) {
            if (gameMap.getLocation(source, bestDirection).locationValue(gameMap) <
                    gameMap.getLocation(source, candidateDirections.get(i)).locationValue(gameMap)) {
                bestDirection = candidateDirections.get(i);
            }
        }

        // avoid overflow
        if (actualStrength(gameMap.getLocation(source, bestDirection), moves) + source.getSite().strength >=
                maximumStrength && candidateDirections.size() > 1) {
            int minStr = Integer.MAX_VALUE;
            Direction chosenDir = bestDirection;
            // we have an overflow, choose another option (the first we find)
            for (Direction otherDirection : candidateDirections) {
                if (actualStrength(gameMap.getLocation(source, otherDirection), moves) + source.getSite().strength <
                        maximumStrength) {
                    return otherDirection;
                } else {
                    if (actualStrength(gameMap.getLocation(source, otherDirection), moves) + source.getSite().strength < minStr) {
                        minStr = actualStrength(gameMap.getLocation(source, otherDirection), moves) + source.getSite().strength;
                        chosenDir = otherDirection;
                    }
                }
            }
            return chosenDir;
        }

        // no overflow is done, choose the previously determined best direction
        return bestDirection;
    }

    private static int actualStrength(Location location, List<Move> moves) {
        for (Move move : moves) {
            if (move.loc.equals(location) && move.dir != Direction.STILL) {
                return 0;
            }
        }
        return location.getSite().strength;
    }
}