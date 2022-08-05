import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.List;
import java.util.stream.Collectors;

public class Location {

    // Public for backward compability
    public final int x, y;
    private final Site site;

    public Location(int x, int y, Site site) {
        this.x = x;
        this.y = y;
        this.site = site;
    }

    public boolean isNeutral() {
        return this.site.owner == 0;
    }

    public boolean isEnemy(int myId) {
        return this.site.owner != myId;
    }

    public boolean isOuter(int myId, GameMap gameMap) {
        return Arrays.stream(Direction.CARDINALS).anyMatch(dir -> gameMap.getLocation(this, dir).isEnemy(myId));
    }

    // get the potential of the current location in the current game state
    public double locationValue(final GameMap gameMap) {
        if (this.isNeutral()) {
            return (this.site.strength == 0) ? this.site.production : (double) this.site.production / this.site.strength;
        }

        // determine the total damage done to all surrounding enemies after conquering this location
        return Arrays.stream(Direction.CARDINALS)
                .map(direction -> gameMap.getSite(this, direction))
                .filter(s -> s.owner != 0 && s.owner != this.site.owner)
                .map(s -> s.strength)
                .reduce(0, Integer::sum);
    }

    // determine the nearest location to the current location, choosing from the
    // candidates list
    public Location nearestLocation(List<Location> candidates, final GameMap gameMap) {
        Location nearest = null;
        double smallestDistance = Double.MAX_VALUE;

        for (Location candidate: candidates) {
            double currentDistance = gameMap.getDistance(this, candidate);
            if (Double.compare(currentDistance, smallestDistance) < 0) {
                smallestDistance = currentDistance;
                nearest = candidate;
            }
        }

        return nearest;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public Site getSite() {
        return site;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Location location = (Location) o;
        return x == location.x && y == location.y;
    }
}