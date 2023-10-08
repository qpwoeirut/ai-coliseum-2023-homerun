package m2_bugfix;

import aic2023.user.*;
import m2_bugfix.util.Util;

public class CatcherPlayer extends BasePlayer {
    int scoutTimer = 30;
    boolean reachedFocalPoint = false;
    Location target = null;

    Direction scoutDir = Direction.values()[uc.getInfo().getID() % 8];
    final int SCOUT_TIMER = 100;
    Location spawn;

    CatcherPlayer(UnitController uc) {
        super(uc);
        this.spawn = uc.getLocation();
    }

    void run() {
        while (true) {
//            debugBytecode("start turn");
            comms.checkIn();
            senseAndReportBases();
            senseAndReportStadiums();
//            debugBytecode("after reporting objects");

            final UnitInfo[] nearbyEnemies = uc.senseUnits(REACHABLE_VISION, uc.getOpponent());
            final int directionOkay = calculateOkayDirections(nearbyEnemies);
            final UnitInfo nearestEnemyBatter = Util.getNearestChebyshev(uc.getLocation(), nearbyEnemies, UnitType.BATTER);
//            debug("nearest enemy batter is " + nearestEnemyBatter);
            if (nearestEnemyBatter != null && ((directionOkay >> Direction.ZERO.ordinal()) & 1) > 0) {
                Util.tryMoveInDirection(uc, nearestEnemyBatter.getLocation().directionTo(uc.getLocation()));
            }

//            debugBytecode("before move");
            if (uc.canMove()) {
                if (target == null ||
                        uc.getLocation().distanceSquared(target) <= 4 ||  // this cutoff is intentionally lower than the cutoff in comms
                        (uc.getLocation().distanceSquared(target) <= VISION &&
                                (uc.isOutOfMap(target) || uc.senseObjectAtLocation(target, false) == MapObject.WATER))) {
                    target = comms.popNearestScoutingQueue();
                    reachedFocalPoint = false;
                    if (target != null) scoutTimer = (int)(Util.movementDistance(uc.getLocation(), target) * 2);
                } else {
                    comms.updateScoutingQueue();
                }
//                debug("target = " + target + ", scoutTimer = " + scoutTimer);
                if (target != null) {
                    scoutTarget(directionOkay);
                } else {
                    scoutRandomLine();
                }
            }

            endTurn();
        }
    }

    void scoutTarget(int directionOkay) {
//        debug("lb dist = " + comms.lowerBoundDistance(uc.getLocation(), target) + ", reachedFP = " + reachedFocalPoint);
        Direction dir = null;
        if (!reachedFocalPoint) {
            final int mapIdx = comms.findBestDistanceMapIdx(uc.getLocation(), target);
//            debug("mapIdx = " + mapIdx + ", distFp = " + comms.distanceFromFocalPoint(mapIdx, target));
            if (comms.lowerBoundDistanceGreaterThan(uc.getLocation(), target, comms.distanceFromFocalPoint(mapIdx, target))) {
                dir = comms.directionViaFocalPoint(target, directionOkay);
            }
        }
//        debug("dir from comms = " + dir);
        if (dir == Direction.ZERO) {
            dir = null;
            reachedFocalPoint = true;
        }
        if (dir == null) {
            dir = bg.moveUsingBug0(target);  // don't bother with bug1. if bug0 fails, just change targets
        }
        if (dir == null) target = null;

        if (dir != null && dir != Direction.ZERO && uc.canMove(dir) && ((directionOkay >> dir.ordinal()) & 1) > 0) {
            uc.move(dir);
        } else {
            Util.tryMoveInOkayDirection(uc, Direction.values()[(int)(uc.getRandomDouble() * 8)], directionOkay);
        }

        if (--scoutTimer <= 0) {
            target = null;
        }
    }

    void scoutRandomLine() {
        final UnitInfo[] nearbyEnemies = uc.senseUnits(REACHABLE_VISION, uc.getOpponent());
        final int directionOkay = calculateOkayDirections(nearbyEnemies);
        final UnitInfo nearestEnemyBatter = Util.getNearestChebyshev(uc.getLocation(), nearbyEnemies, UnitType.BATTER);
//            debug("nearest enemy batter is " + nearestEnemyBatter);
        if (nearestEnemyBatter != null && ((directionOkay >> Direction.ZERO.ordinal()) & 1) > 0) {
            Util.tryMoveInDirection(uc, nearestEnemyBatter.getLocation().directionTo(uc.getLocation()));
        }

        if (uc.isOutOfMap(uc.getLocation().add(scoutDir.dx * 4, scoutDir.dy * 4)) || --scoutTimer == 0) {
            int shift = (int)(uc.getRandomDouble() * 6) + 1;
            if (shift >= 4) ++shift;
            scoutDir = Direction.values()[(scoutDir.ordinal() + shift) % 8];
            scoutTimer = SCOUT_TIMER;
        }
        final Direction dir = bg.move(spawn.add(scoutDir.dx * 55, scoutDir.dy * 55));
        if (dir != null && dir != Direction.ZERO) {
            Util.tryMoveInOkayDirection(uc, dir, directionOkay);
        } else {
            Util.tryMoveInOkayDirection(uc, scoutDir, directionOkay);
        }
    }
}
