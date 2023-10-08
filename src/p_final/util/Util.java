package p_final.util;

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

    public static float movementDistance(Location a, Location b) {
        int max = Util.chebyshevDistance(a, b);
        return max + (max - Math.min(a.x >= b.x ? a.x - b.x : b.x - a.x, a.y >= b.y ? a.y - b.y : b.y - a.y)) * 1.4142f;
    }

    public static float movementNearDistance(Location a, Location b) {
        final int dx = Math.max(0, Math.abs(a.x - b.x) - 2);
        final int dy = Math.max(0, Math.abs(a.y - b.y) - 2);
        return Math.abs(dx - dy) + (Math.max(dx, dy) - Math.abs(dx - dy)) * 1.4142f;
    }

    public static void tryMoveInDirection(UnitController uc, Direction dir) {
        if (uc.canMove(dir)) uc.move(dir);
        else if (uc.canMove(dir.rotateLeft())) uc.move(dir.rotateLeft());
        else if (uc.canMove(dir.rotateRight())) uc.move(dir.rotateRight());
        else if (uc.canMove(dir.rotateLeft().rotateLeft())) uc.move(dir.rotateLeft().rotateLeft());
        else if (uc.canMove(dir.rotateRight().rotateRight())) uc.move(dir.rotateRight().rotateRight());
    }

    public static void tryMoveInOkayDirection(UnitController uc, Direction dir, int directionOkay) {
        if (uc.canMove(dir) && ((directionOkay >> dir.ordinal()) & 1) > 0) uc.move(dir);
        else if (uc.canMove(dir.rotateLeft()) && ((directionOkay >> dir.rotateLeft().ordinal()) & 1) > 0) uc.move(dir.rotateLeft());
        else if (uc.canMove(dir.rotateRight()) && ((directionOkay >> dir.rotateRight().ordinal()) & 1) > 0) uc.move(dir.rotateRight());
        else if (uc.canMove(dir.rotateLeft().rotateLeft()) && ((directionOkay >> dir.rotateLeft().rotateLeft().ordinal()) & 1) > 0) uc.move(dir.rotateLeft().rotateLeft());
        else if (uc.canMove(dir.rotateRight().rotateRight()) && ((directionOkay >> dir.rotateRight().rotateRight().ordinal()) & 1) > 0) uc.move(dir.rotateRight().rotateRight());
    }

    public static int packLoc(Location loc) {
        return loc.x * 10000 + loc.y;
    }
}
