package g_fixgodnavbugs.util;

import aic2023.user.*;

public class Util {
    public static UnitInfo getNearest(Location loc, UnitInfo[] units) {
        int minDist = 10000;
        UnitInfo closest = null;
        for (int i = units.length - 1; i >= 0; --i) {
            if (minDist > loc.distanceSquared(units[i].getLocation())) {
                minDist = loc.distanceSquared(units[i].getLocation());
                closest = units[i];
            }
        }
        return closest;
    }
    public static UnitInfo getNearest(Location loc, UnitInfo[] units, UnitType type) {
        int minDist = 10000;
        UnitInfo closest = null;
        for (int i = units.length - 1; i >= 0; --i) {
            if (units[i].getType() == type && minDist > loc.distanceSquared(units[i].getLocation())) {
                minDist = loc.distanceSquared(units[i].getLocation());
                closest = units[i];
            }
        }
        return closest;
    }

    public static Location getNearest(Location loc, Location[] locs) {
        return getNearest(loc, locs, locs.length);
    }

    public static Location getNearest(Location loc, Location[] locs, int n) {
        int minDist = 10000;
        Location closest = null;
        for (int i = n - 1; i >= 0; --i) {
            if (minDist > loc.distanceSquared(locs[i])) {
                minDist = loc.distanceSquared(locs[i]);
                closest = locs[i];
            }
        }
        return closest;
    }

    public static int getNearestIndex(Location loc, Location[] locs) {
        return getNearestIndex(loc, locs, locs.length);
    }

    public static int getNearestIndex(Location loc, Location[] locs, int n) {
        int minDist = 10000;
        int closestIndex = -1;
        for (int i = n - 1; i >= 0; --i) {
            if (minDist > loc.distanceSquared(locs[i])) {
                minDist = loc.distanceSquared(locs[i]);
                closestIndex = i;
            }
        }
        return closestIndex;
    }
    public static int getNearestDistance(Location loc, UnitInfo[] units, UnitType type) {
        int minDist = 10000;
        for (int i = units.length - 1; i >= 0; --i) {
            if (units[i].getType() == type && minDist > loc.distanceSquared(units[i].getLocation())) {
                minDist = loc.distanceSquared(units[i].getLocation());
            }
        }
        return minDist;
    }

    public static UnitInfo getNearestChebyshev(Location loc, UnitInfo[] units, UnitType type) {
        int minDist = 10000;
        UnitInfo closest = null;
        for (int i = units.length - 1; i >= 0; --i) {
            if (units[i].getType() == type && minDist > chebyshevDistance(loc, units[i].getLocation())) {
                minDist = chebyshevDistance(loc, units[i].getLocation());
                closest = units[i];
            }
        }
        return closest;
    }
    // https://en.wikipedia.org/wiki/Chebyshev_distance
    public static int getNearestChebyshevDistance(Location loc, UnitInfo[] units, UnitType type) {
        int minDist = 10000;
        for (int i = units.length - 1; i >= 0; --i) {
            if (units[i].getType() == type && minDist > chebyshevDistance(loc, units[i].getLocation())) {
                minDist = chebyshevDistance(loc, units[i].getLocation());
            }
        }
        return minDist;
    }

    public static int chebyshevDistance(Location a, Location b) {
        return Math.max(a.x >= b.x ? a.x - b.x : b.x - a.x, a.y >= b.y ? a.y - b.y : b.y - a.y);
    }

    public static int getMaxIndex(int[] values, int n) {
        if (n <= 0) return -1;
        int bestValue = values[0];
        int bestIndex = 0;
        for (int i = n - 1; i > 0; --i) {
            if (bestValue < values[i]) {
                bestValue = values[i];
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    public static int countType(UnitInfo[] units, UnitType type) {
        int count = 0;
        for (int i = units.length - 1; i >= 0; --i) count += units[i].getType() == type ? 1 : 0;
        return count;
    }

    public static void tryMoveInDirection(UnitController uc, Direction dir) {
        if (uc.canMove(dir)) uc.move(dir);
        else if (uc.canMove(dir.rotateLeft())) uc.move(dir.rotateLeft());
        else if (uc.canMove(dir.rotateRight())) uc.move(dir.rotateRight());
        else if (uc.canMove(dir.rotateLeft().rotateLeft())) uc.move(dir.rotateLeft().rotateLeft());
        else if (uc.canMove(dir.rotateRight().rotateRight())) uc.move(dir.rotateRight().rotateRight());
    }

    public static int packLoc(Location loc) {
        return loc.x * 10000 + loc.y;
    }
}
