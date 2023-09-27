package d_player.util;

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

    public static boolean batterMayInteract(UnitController uc, Location loc) {
        Location cur = uc.getLocation();
        if (Util.chebyshevDistance(cur, loc) <= 1) return true;
        if (Util.chebyshevDistance(cur, loc) > 3) return false;
        Direction dir1 = cur.directionTo(loc).rotateLeft().rotateLeft();
        MapObject obj1, obj2;
        for (int i = 5; i > 0; --i) {
            if (!uc.isOutOfMap(cur)) {
                Location newCur = cur.add(dir1);
                obj1 = uc.senseObjectAtLocation(newCur, true);
                if (obj1 != MapObject.WATER && obj1 != MapObject.BALL) {
                    final Direction dir2 = loc.directionTo(cur);

                    if (cur.distanceSquared(loc.add(dir2)) <= 20 && !uc.isOutOfMap(loc.add(dir2))) {
                        obj2 = uc.senseObjectAtLocation(loc.add(dir2), true);
                        if (obj2 != MapObject.WATER && obj2 != MapObject.BALL && Util.chebyshevDistance(newCur, loc.add(dir2)) <= 1) return true;
                    }

                    if (cur.distanceSquared(loc.add(dir2.rotateLeft())) <= 20 && !uc.isOutOfMap(loc.add(dir2.rotateLeft()))) {
                        obj2 = uc.senseObjectAtLocation(loc.add(dir2.rotateLeft()), true);
                        if (obj2 != MapObject.WATER && obj2 != MapObject.BALL && Util.chebyshevDistance(newCur, loc.add(dir2.rotateLeft())) <= 1) return true;
                    }

                    if (cur.distanceSquared(loc.add(dir2.rotateRight())) <= 20 && !uc.isOutOfMap(loc.add(dir2.rotateRight()))) {
                        obj2 = uc.senseObjectAtLocation(loc.add(dir2.rotateRight()), true);
                        if (obj2 != MapObject.WATER && obj2 != MapObject.BALL && Util.chebyshevDistance(newCur, loc.add(dir2.rotateRight())) <= 1) return true;
                    }

                    if (cur.distanceSquared(loc.add(dir2.rotateLeft().rotateLeft())) <= 20 && !uc.isOutOfMap(loc.add(dir2.rotateLeft().rotateLeft()))) {
                        obj2 = uc.senseObjectAtLocation(loc.add(dir2.rotateLeft().rotateLeft()), true);
                        if (obj2 != MapObject.WATER && obj2 != MapObject.BALL && Util.chebyshevDistance(newCur, loc.add(dir2.rotateLeft().rotateLeft())) <= 1) return true;
                    }

                    if (cur.distanceSquared(loc.add(dir2.rotateRight().rotateRight())) <= 20 && !uc.isOutOfMap(loc.add(dir2.rotateRight().rotateRight()))) {
                        obj2 = uc.senseObjectAtLocation(loc.add(dir2.rotateRight().rotateRight()), true);
                        if (obj2 != MapObject.WATER && obj2 != MapObject.BALL && Util.chebyshevDistance(newCur, loc.add(dir2.rotateRight().rotateRight())) <= 1) return true;
                    }
                }
            }
            dir1 = dir1.rotateRight();
        }
        return false;
    }
}
