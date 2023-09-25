package b_bugnav;

import aic2023.user.Direction;
import aic2023.user.Location;
import aic2023.user.UnitController;
import b_bugnav.util.IntHashMap;
import b_bugnav.util.Util;

public class BugMover {

    private final UnitController uc;
    private final int INF = 10000;
    private int stage = 0;
    private Location target = null;
    private Direction prev_move = Direction.SOUTH;
    private int minDist;

    private final boolean leftTurn;  // decide randomly whether this bug will turn left or right
    private final IntHashMap[] visited = new IntHashMap[Direction.values().length];
    private final int BUCKET_COUNT = 8;

    private final int STUCK_THRESHOLD = 5;  // after this many failed moves, restart the algorithm
    private int stuckCount = 0;
    private int attempts = 0;

    BugMover(UnitController uc) {
        this.uc = uc;
        this.leftTurn = uc.getInfo().getID() % 2 == 0;
    }

    void init(Location fin) {
        uc.println("Setting target to " + fin);
        target = fin;
        stage = 0;

        prev_move = uc.getLocation().directionTo(fin);
        stuckCount = 0;
        attempts = 0;
        minDist = INF;

        for (int i = Direction.values().length - 1; i >= 0; --i) {
            visited[i] = new IntHashMap(BUCKET_COUNT);
        }
    }
    void useDirection(Direction dir) {
        prev_move = dir;
        stuckCount = 0;
        attempts = 0;
    }

    /**
     * Recommends best direction to move to the provided location. If at the location, returns ZERO.
     * Uses bug 0 algorithm until it hits a loop, then uses bug 1 algorithm.
     * If the location is unreachable, will return Direction.ZERO
     * Bug0 can be tested with the Flooded map. Bug1 can be tested on Comb.
     * @param fin Location to move to
     * @return direction to move in
     */
    Direction move(Location fin) {
        // TODO: add an @param nullIfUnreachable whether to return null or Direction.ZERO if the location is unreachable

        if (!uc.canMove()) return Direction.ZERO;
        if (!uc.canMove(Direction.NORTH) && !uc.canMove(Direction.NORTHEAST) &&
                !uc.canMove(Direction.EAST) && !uc.canMove(Direction.SOUTHEAST) &&
                !uc.canMove(Direction.SOUTH) && !uc.canMove(Direction.SOUTHWEST) &&
                !uc.canMove(Direction.WEST) && !uc.canMove(Direction.NORTHWEST)) {
            return Direction.ZERO;
        }

        Location loc = uc.getLocation();
        if (target != fin) {
            init(fin);
        }
        visited[prev_move.ordinal()].put(Util.packLoc(loc), 1);

        if (stage == 0) {  // try bug0 first
            uc.println("stage 0, fin " + fin);
            final Direction dir = bug0(fin);
            if (dir != null) {
                useDirection(dir);
                return dir;
            }

            stage = 1;
            for (int i = Direction.values().length - 1; i >= 0; --i) {
                visited[i] = new IntHashMap(BUCKET_COUNT);
            }
        }
        if (stage == 1) {  // bug0 has failed >:( try bug1
            uc.println("stage 1, fin " + fin);
            minDist = Math.min(minDist, loc.distanceSquared(fin));
            final Direction dir = wallFollow();
            if (dir != null) {
                useDirection(dir);
                return dir;
            }

            stage = 2;
            for (int i = Direction.values().length - 1; i >= 0; --i) {
                visited[i] = new IntHashMap(BUCKET_COUNT);
            }
        }
        if (stage == 2) {
            uc.println("stage 2, fin, minDist " + fin + ", " + minDist);
            if (loc.distanceSquared(fin) <= minDist) {
                stage = 0;
                prev_move = loc.directionTo(fin);
                for (int i = Direction.values().length - 1; i >= 0; --i) {
                    visited[i] = new IntHashMap(BUCKET_COUNT);
                }

                final Direction dir = move(fin);
                if (dir != null) {
                    useDirection(dir);
                    return dir;
                }
            }
            final Direction dir = wallFollow();
            if (dir != null) {
                useDirection(dir);
                return dir;
            }
        }

        // return ZERO to avoid making invalid moves
        if (++stuckCount >= STUCK_THRESHOLD) {
            init(fin);
            ++attempts;  // TODO: once this is too high, assume the target is unreachable
            return move(fin);
        }
        return Direction.ZERO;
    }

    Direction bug0(Location fin) {
        Location loc = uc.getLocation();
        if (uc.canMove(loc.directionTo(fin))) {
            prev_move = loc.directionTo(fin);

            if (visited[prev_move.ordinal()].get(Util.packLoc(loc.add(prev_move))) == 1) {
                return null;
            } else {
                return prev_move;
            }
        }
        return wallFollow();
    }

    Direction wallFollow() {
        Location loc = uc.getLocation();
        Direction dir = leftTurn ? prev_move.rotateRight() : prev_move.rotateLeft();
        // initial heading should be reversed since we're cutting back inside
        for (int i = 7; i >= 0; --i) {
            if (uc.canMove(dir)) {
                if (visited[dir.ordinal()].get(Util.packLoc(loc.add(dir))) == 1) {
                    return null;
                } else {
                    prev_move = dir;
                    return dir;
                }
            }
            dir = leftTurn ? dir.rotateLeft() : dir.rotateRight();
        }
        return null;
    }

    Direction move_to_objects() {
//        uc.println("startvar: " + startvar + " " + Direction.values()[startvar]);
        Location loc = uc.getLocation();
        int current_index = 0;
        for (int i = 0; i < 8; i++) {
            Direction current_dir = Direction.values()[i];
            if (current_dir.isEqual(prev_move)) {
                current_index = i;
            }
        }

        if ((int) (uc.getRandomDouble() * 30) == 0 && uc.canMove(Direction.values()[(current_index + 1) % 8])) {
            prev_move = Direction.values()[(current_index + 1) % 8];
            return prev_move;
        }

        if ((int) (uc.getRandomDouble() * 30) == 0 && uc.canMove(Direction.values()[(current_index + 5) % 8])) {
            prev_move = Direction.values()[(current_index + 5) % 8];
            return prev_move;
        }
        if (uc.canMove(prev_move)) {
            return prev_move;
        }

        Direction next_dir = Direction.values()[(current_index + 2) % 8];
        Direction prev_dir = Direction.values()[(current_index + 6) % 8];

        if ((int) (uc.getRandomDouble() * 2) == 0) {
            if (uc.canMove(next_dir)) {
                prev_move = next_dir;
                return prev_move;
            } else if (uc.canMove(prev_dir)) {
                prev_move = prev_dir;
                return prev_move;
            }
        } else {
            if (uc.canMove(prev_dir)) {
                prev_move = prev_dir;
                return prev_move;
            } else if (uc.canMove(next_dir)) {
                prev_move = next_dir;
                return prev_move;
            }
        }

//        float vision_range = uc.getType().getStat(UnitStat.VISION_RANGE);
        for (int i = 0; i < 8; i++) {
            Direction current_dir = Direction.values()[i];
            if (!uc.canMove(current_dir)) {
                prev_move = current_dir;
                return prev_move;
            }
        }
        Direction final_dir = Direction.NORTHEAST;
//        uc.println("here again");
        prev_move = final_dir;
        return final_dir;
    }
}
