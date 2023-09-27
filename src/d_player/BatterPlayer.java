package d_player;

import aic2023.user.*;
import d_player.util.Util;

public class BatterPlayer extends BasePlayer {
    // Add functionality to claim a function
    private Location patrolLoc = null;  // location the batter should hover around

    BatterPlayer(UnitController uc) {
        super(uc);
    }

    void run() {
        while (true) {
            comms.checkIn();
            senseAndReportBases();
            senseAndReportStadiums();
            debugBytecode("after reporting bases/stadiums");

            UnitInfo[] enemies = uc.senseUnits(VISION, uc.getOpponent());
            debug("Enemies: " + enemies.length);
            if (uc.canAct()) {
                final UnitInfo toAttack = pickTargetToAttack(enemies);
                debugBytecode("after pickTarget");
                if (toAttack != null) {
//                uc.println(toAttack.getLocation().x + " " + toAttack.getLocation().y);
                    attack(toAttack);
                    enemies = senseAndReportEnemies();  // if we need to cut bytecode we can make this more efficient
                } else {
                    comms.reportEnemySightings(enemies, URGENCY_FACTOR);
                }
            }
            debugBytecode("after attack");

            if (uc.canMove()) {
                if (patrolLoc != null) {
                    patrol(enemies);
                } else {
                    normalBehavior(enemies);
                }
            }
            uc.yield();
        }
    }

    void normalBehavior(UnitInfo[] enemies) {
        debugBytecode("start normalBehavior");
        final UnitInfo nearestEnemyBatter = Util.getNearest(uc.getLocation(), enemies, UnitType.BATTER);
        if (nearestEnemyBatter != null && Util.batterMayInteract(uc, nearestEnemyBatter.getLocation())) {
            uc.println("enemy at " + nearestEnemyBatter.getLocation());
            // TODO: move batters in knight's move shapes. until then we should probably just run away
//            UnitInfo[] allies = uc.senseUnits(VISION, uc.getTeam());
//            int batters = 0, catchers = 0, pitchers = 0, hq = 0;
//            for (int i = allies.length - 1; i >= 0; --i) {
//                if (allies[i].getType() == UnitType.BATTER) ++batters;
//                else if (allies[i].getType() == UnitType.CATCHER) ++catchers;
//                else if (allies[i].getType() == UnitType.PITCHER) ++pitchers;
//                else ++hq;
//            }
//            if (batters > 1 || pitchers > 0 || hq > 0) {  // just attack or smth, its probably fine
//                Util.tryMoveInDirection(uc, uc.getLocation().directionTo(nearestEnemy.getLocation()));
//            }

            int bestDir = Direction.ZERO.ordinal();
            int bestChebyshevDist = Util.chebyshevDistance(uc.getLocation(), nearestEnemyBatter.getLocation());
            for (int i = 7; i >= 0; --i) {
                if (uc.canMove(Direction.values()[i])) {
                    final int nearestChebyshevDistance = Util.getNearestChebyshevDistance(uc.getLocation().add(Direction.values()[i]), enemies, UnitType.BATTER);
                    if (bestChebyshevDist > nearestChebyshevDistance && nearestChebyshevDistance >= 2) {
                        bestChebyshevDist = nearestChebyshevDistance;
                        bestDir = i;
                    }
                }
            }
            if (uc.canMove(Direction.values()[bestDir])) {
                uc.move(Direction.values()[bestDir]);
            }
        } else {
            final UnitInfo nearestEnemy = Util.getNearest(uc.getLocation(), enemies);
            if (nearestEnemy != null && Util.batterMayInteract(uc, nearestEnemy.getLocation())) {
                Util.tryMoveInDirection(uc, uc.getLocation().directionTo(nearestEnemy.getLocation()));
            } else {
                final int reportedEnemyCount = comms.listEnemySightings();
                final int targetEnemySightingIndex = Util.getMaxIndex(comms.returnedUrgencies, reportedEnemyCount);
                if (targetEnemySightingIndex == -1) {
                    Util.tryMoveInDirection(uc, spreadOut());
                } else {
                    final Direction toMove = bg.move(comms.returnedLocations[targetEnemySightingIndex]);
                    if (uc.canMove(toMove)) {
                        uc.move(toMove);
                    } else {
                        Util.tryMoveInDirection(uc, uc.getLocation().directionTo(comms.returnedLocations[targetEnemySightingIndex]));
                    }
                }
            }
        }
        debugBytecode("end normalBehavior");
    }

    void patrol(UnitInfo[] enemies) {
        final int PATROL_DISTANCE = 72;  // range it can go to attack other units, relative to patrolLoc
        final int IDLE_DISTANCE = 8;  // range the batter should stay in the designated patrol location, when no enemies
        final UnitInfo nearestEnemyBatter = Util.getNearest(uc.getLocation(), enemies, UnitType.BATTER);
        final UnitInfo nearestEnemy = Util.getNearest(uc.getLocation(), enemies);
        if (nearestEnemyBatter == null) {
            //try to go after unit sighted, within patrol zone
            if (nearestEnemy != null && uc.getLocation().distanceSquared(nearestEnemy.getLocation()) < PATROL_DISTANCE) {
                Util.tryMoveInDirection(uc, uc.getLocation().directionTo(nearestEnemy.getLocation()));
            } else {//no enemies within range
                //within idle zone
                if (uc.getLocation().distanceSquared(patrolLoc) < IDLE_DISTANCE){
                    //randomly move around in the zone
                    Util.tryMoveInDirection(uc, Direction.values()[(int)(uc.getRandomDouble() * 8)]);
                }
                //outside idle zone
                else{
                    final Direction toMove = bg.move(patrolLoc);
                    if (uc.canMove(toMove)) {
                        uc.move(toMove);
                    }
                }
            }
        } else {
            if (uc.getLocation().distanceSquared(nearestEnemyBatter.getLocation()) <= PATROL_DISTANCE) {
                Util.tryMoveInDirection(uc, uc.getLocation().directionTo(nearestEnemyBatter.getLocation()));
            }
            else{//enemy is out of range, so just chill in the zone
                if (uc.getLocation().distanceSquared(patrolLoc) < IDLE_DISTANCE){
                    //randomly move around in the zone
                    Util.tryMoveInDirection(uc, Direction.values()[(int)(uc.getRandomDouble() * 8)]);
                }
                else{
                    final Direction toMove = bg.move(patrolLoc);
                    if (uc.canMove(toMove)) {
                        uc.move(toMove);
                    } else {
                        uc.yield();
                        //Util.tryMoveInDirection(uc, uc.getLocation().directionTo(comms.returnedLocations[targetEnemySightingIndex]));
                    }
                }
            }
        }
    }

    UnitInfo pickTargetToAttack(UnitInfo[] enemies) {
        debugBytecode("pickTarget start");

        UnitInfo toAttack = null;
        int bestAttackScore = -1;
        for (int i = enemies.length - 1; i >= 0; --i) {
            if (enemies[i].getType() == UnitType.HQ || Util.chebyshevDistance(uc.getLocation(), enemies[i].getLocation()) > 2) continue;
            final int val = directionToMoveToAttack(enemies[i]);
            if (val != -1) {
                // score by attack effectiveness (how much net reputation we gain), tiebreak by closest enemy
                // batters are implicitly targeted first because they are worth 60 rep, which is more than the others

                // add 2 because we want to attack anything within distance 2 no matter what
                final int attackScore = 10 * (val / 9) - uc.getLocation().distanceSquared(enemies[i].getLocation()) + 2;
                if (bestAttackScore < attackScore) {
                    bestAttackScore = attackScore;
                    toAttack = enemies[i];
                }
            }
        }

        debugBytecode("pickTarget end");
        return toAttack;
    }

    boolean attack(UnitInfo target) {  // return value current unused but might be useful later, will leave for now
//        uc.println("attack start " + uc.getEnergyUsed());

        final int val = directionToMoveToAttack(target);
        if (val == -1) return false;  // this should only ever happen if we ran out of bytecode
        Direction dir = Direction.values()[val % 9];
        if (dir != Direction.ZERO) {
            if (!uc.canMove(dir)) return false;  // also should only happen if we run out of bytecode
            uc.move(dir);
        }

        dir = uc.getLocation().directionTo(target.getLocation());
        if (uc.canBat(dir, GameConstants.MAX_STRENGTH)) {
            uc.bat(dir, GameConstants.MAX_STRENGTH);
//            uc.println("attack end " + uc.getEnergyUsed());
            return true;
        }

//        uc.println("attack end " + uc.getEnergyUsed());
        return false;
    }

    int directionToMoveToAttack(UnitInfo target) {
        debugBytecode("directionToMove start");
        if (!uc.canMove()) {
            if (Util.chebyshevDistance(uc.getLocation(), target.getLocation()) <= 1) {
                final int effectiveness = hitEffectiveness(target, uc.getLocation().directionTo(target.getLocation()));
                if (effectiveness >= 0) {
                    debugBytecode("directionToMove end0");
                    return effectiveness * 9 + Direction.ZERO.ordinal();
                }
            }
            debugBytecode("directionToMove end1");
            return -1;
        }

        int bestDir = -1;
        int bestEffectiveness = -1;
        // try cardinal directions first so that move cooldown is smaller
        for (int i = 8; i >= 0; --i) {
            if (uc.canMove(Direction.values()[i])) {
                final Location loc = uc.getLocation().add(Direction.values()[i]);
                if (loc.distanceSquared(target.getLocation()) <= 2) {
                    final int effectiveness = hitEffectiveness(target, loc.directionTo(target.getLocation()));
                    if (bestEffectiveness < effectiveness) {
                        bestEffectiveness = effectiveness;
                        bestDir = i;
                    }
                }
            }
        }

        debugBytecode("directionToMove end2");
        return bestEffectiveness * 9 + bestDir;
    }

    int hitEffectiveness(UnitInfo target, Direction dir) {
        debugBytecode("hitEffectiveness start");

        Location loc = target.getLocation();
        for (int i = 0; i < GameConstants.MAX_STRENGTH; ++i) {
//            uc.println("hitEffectiveness start " + i + " " + uc.getEnergyUsed());
            loc = loc.add(dir);

            if (uc.getLocation().distanceSquared(loc) > uc.getType().getStat(UnitStat.VISION_RANGE)) break;

            if (uc.isOutOfMap(loc)) {
//                uc.println("hitEffectiveness end " + uc.getEnergyUsed());
                return (int) target.getType().getStat(UnitStat.REP_COST);
            }

            final MapObject map = uc.senseObjectAtLocation(loc, true);
            if (map == MapObject.BALL || map == MapObject.WATER) {
//                uc.println("hitEffectiveness end " + uc.getEnergyUsed());
                return (int) target.getType().getStat(UnitStat.REP_COST);
            }

            final UnitInfo unit = uc.senseUnitAtLocation(loc);
            if (unit != null) {
//                uc.println("hitEffectiveness end " + uc.getEnergyUsed());
                return (int) target.getType().getStat(UnitStat.REP_COST) + (unit.getTeam() == uc.getOpponent() ? 1 : -1) * (int) unit.getType().getStat(UnitStat.REP_COST);
            }
        }

        debugBytecode("hitEffectiveness end");
        return 0;
    }

    Direction spreadOut() {
        final Location currentLocation = uc.getLocation();
        float x = currentLocation.x, y = currentLocation.y;
        UnitInfo[] allies = uc.senseUnits(VISION, uc.getTeam());
        float dist;
        float allyWeightX = 0, allyWeightY = 0;
        Location loc;
        for (int i = allies.length; i --> 0;) {
            loc = allies[i].getLocation();
            // add one to avoid div by 0 when running out of bytecode
            dist = currentLocation.distanceSquared(loc) + 1;
            // subtract since we want to move away
            allyWeightX -= (loc.x - x) / dist;
            allyWeightY -= (loc.y - y) / dist;
        }
        float weightX = allyWeightX * 10;
        float weightY = allyWeightY * 10;

        int finalDx = uc.getRandomDouble() * 10 > weightX + 5 ? -1 : 1;
        int finalDy = uc.getRandomDouble() * 10 > weightY + 5 ? -1 : 1;
        return Direction.getDirection(finalDx, finalDy);
    }
}
