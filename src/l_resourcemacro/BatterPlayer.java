package l_resourcemacro;

import aic2023.user.*;
import l_resourcemacro.util.Util;

public class BatterPlayer extends BasePlayer {
    private final int BATTER_REACHABLE_DISTANCE = 8;
    private final int BATTER_MOVABLE_DISTANCE = 12;

    BatterPlayer(UnitController uc) {
        super(uc);
    }

    void run() {
        MapObject claimedObjectType = null;
        Location claimedObjectLocation = null;
        int claimedObjectId = -1;
        while (true) {
            comms.checkIn();
            senseAndReportBases();
            senseAndReportStadiums();
//            debugBytecode("after reporting bases/stadiums");

            UnitInfo[] enemies = uc.senseUnits(VISION, uc.getOpponent());
//            debug("Enemies: " + enemies.length);
            if (uc.canAct()) {
                final UnitInfo toAttack = pickTargetToAttack(enemies);
//                debugBytecode("after pickTarget");
                if (toAttack != null) {
//                    debug("toAttack " + toAttack.getLocation());
                    attack(toAttack);
                    enemies = senseAndReportEnemies();  // if we need to cut bytecode we can make this more efficient
                } else {
                    comms.reportEnemySightings(enemies, URGENCY_FACTOR);
                }
            }
            if (uc.canAct()) {
                Location[] balls = uc.senseObjects(MapObject.BALL, REACHABLE_VISION);
                final int ballToHit = pickBallIndex(balls);
                if (ballToHit != -1) {
                    final int index = ballToHit / 9 / 4, dirIdx = (ballToHit / 4) % 9, strength = ballToHit % 4;
//                    debug("toSelfBat " + allies[index].getLocation() + " " + strength);
                    if (dirIdx != 8 && uc.canMove(Direction.values()[dirIdx])) {
                        uc.move(Direction.values()[dirIdx]);
                    }
                    if (uc.canBat(uc.getLocation().directionTo(balls[index]), strength)) {
                        uc.bat(uc.getLocation().directionTo(balls[index]), strength);
                    }
                }
            }

            if (uc.canAct()) {
                final UnitInfo[] allies = uc.senseUnits(8, uc.getTeam());
                final int toSelfBat = pickTargetIndexToSelfBat(allies);
                if (toSelfBat != -1) {
                    final int index = toSelfBat / 9 / 4, dirIdx = (toSelfBat / 4) % 9, strength = toSelfBat % 4;
//                    debug("toSelfBat " + allies[index].getLocation() + " " + strength);
                    selfBat(allies[index], strength, Direction.values()[dirIdx]);
                }
            }
//            debugBytecode("after attack");

            if (uc.canMove()) {
//                debugBytecode("start normalBehavior");
                final UnitInfo[] nearbyEnemies = uc.senseUnits(REACHABLE_VISION, uc.getOpponent());
                final int directionOkay = calculateOkayDirections(nearbyEnemies);
//                debugBytecode("end directionOkay");

                final UnitInfo nearestEnemyBatter = Util.getNearest(uc.getLocation(), enemies, UnitType.BATTER);
                if (nearestEnemyBatter != null && comms.lowerBoundDistance(nearestEnemyBatter.getLocation()) <= comms.DISTANCE_UNIT * BATTER_REACHABLE_DISTANCE) {
//                    debug("enemy at " + nearestEnemyBatter.getLocation() + ", lb dist: " + comms.lowerBoundDistance(nearestEnemyBatter.getLocation()));
                    int bestDir = -1;
                    int bestChebyshevDist = 100;
                    for (int i = 8; i >= 0; --i) {
                        if (((directionOkay >> i) & 1) > 0 && (uc.canMove(Direction.values()[i]) || i == 8)) {
                            final int nearestChebyshevDistance = Util.getNearestChebyshevDistance(uc.getLocation().add(Direction.values()[i]), enemies, UnitType.BATTER);
                            if (bestChebyshevDist > nearestChebyshevDistance && nearestChebyshevDistance > 2) {
                                bestChebyshevDist = nearestChebyshevDistance;
                                bestDir = i;
                            }
                        }
                    }
                    if (bestDir != -1 && bestDir != 8) {
                        uc.move(Direction.values()[bestDir]);
                    }
                } else {
                    final UnitInfo nearestEnemy = Util.getNearest(uc.getLocation(), enemies);
                    if (nearestEnemy != null && comms.lowerBoundDistance(nearestEnemy.getLocation()) <= comms.DISTANCE_UNIT * BATTER_REACHABLE_DISTANCE) {
//                        debug("enemy at " + nearestEnemy.getLocation() + ", lb dist: " + comms.lowerBoundDistance(nearestEnemy.getLocation()));
                        Util.tryMoveInDirection(uc, uc.getLocation().directionTo(nearestEnemy.getLocation()));
                    } else {
                        final int reportedEnemyCount = comms.listEnemySightings();
                        final int targetEnemySightingIndex = Util.getMaxIndex(comms.returnedUrgencies, reportedEnemyCount);
                        if (targetEnemySightingIndex == -1) {
                            if (claimedObjectLocation == null) {
                                final int unclaimedStadiumCount = comms.listUnclaimedStadiumsAsBatter();
                                final int claimedStadiumIndex = Util.getNearestIndex(uc.getLocation(), comms.returnedLocations, unclaimedStadiumCount);
                                if (claimedStadiumIndex != -1) {
                                    claimedObjectType = MapObject.STADIUM;
                                    claimedObjectLocation = comms.returnedLocations[claimedStadiumIndex];
                                    claimedObjectId = comms.returnedIds[claimedStadiumIndex];
                                    comms.claimStadiumAsBatter(claimedObjectId);
                                }

                                if (claimedObjectLocation == null) {
                                    final int unclaimedBaseCount = comms.listUnclaimedBasesAsBatter();
                                    final int claimedBaseIndex = Util.getNearestIndex(uc.getLocation(), comms.returnedLocations, unclaimedBaseCount);
                                    if (claimedBaseIndex != -1) {
                                        claimedObjectType = MapObject.BASE;
                                        claimedObjectLocation = comms.returnedLocations[claimedBaseIndex];
                                        claimedObjectId = comms.returnedIds[claimedBaseIndex];
                                        comms.claimBaseAsBatter(claimedObjectId);
                                    }
                                }
                            }
                            if (claimedObjectLocation != null) {
                                if (claimedObjectType == MapObject.BASE) {
                                    comms.claimBaseAsBatter(claimedObjectId);
                                } else {
                                    comms.claimStadiumAsBatter(claimedObjectId);
                                }
                                patrol(claimedObjectLocation, enemies, directionOkay);
                            } else if (uc.canMove()) {
                                Util.tryMoveInOkayDirection(uc, Direction.values()[(int)(uc.getRandomDouble() * 8)], directionOkay);
                            }
                        } else {
                            if (uc.getLocation().distanceSquared(comms.returnedLocations[targetEnemySightingIndex]) <= 16) {  // try to avoid batters all going to same place by reducing urgency
                                comms.reduceSightingUrgency(comms.returnedLocations[targetEnemySightingIndex], URGENCY_FACTOR * 3);
                            }
                            Direction toMove = null;
                            final int distance = comms.lowerBoundDistance(comms.returnedLocations[targetEnemySightingIndex]);
                            if (distance > comms.DISTANCE_UNIT * BATTER_MOVABLE_DISTANCE) {
                                toMove = comms.directionViaFocalPoint(comms.returnedLocations[targetEnemySightingIndex], directionOkay);
                            }
                            if (toMove == null) toMove = bg.move(comms.returnedLocations[targetEnemySightingIndex]);
                            if (toMove != null && toMove != Direction.ZERO && uc.canMove(toMove) && ((directionOkay >> toMove.ordinal()) & 1) > 0) {
                                uc.move(toMove);
                            } else {
                                Util.tryMoveInOkayDirection(uc, uc.getLocation().directionTo(comms.returnedLocations[targetEnemySightingIndex]), directionOkay);
                            }
                        }
                    }
                }
//                debugBytecode("end normalBehavior");
            }

            endTurn();
        }
    }

    UnitInfo pickTargetToAttack(UnitInfo[] enemies) {
//        debugBytecode("pickTarget start");

        UnitInfo toAttack = null;
        int bestAttackScore = -1;
        for (int i = enemies.length - 1; i >= 0; --i) {
            if (enemies[i].getType() == UnitType.HQ || Util.chebyshevDistance(uc.getLocation(), enemies[i].getLocation()) > 2) continue;
            final int val = directionToMoveToAttack(enemies[i].getLocation(), enemies[i].getType().getStat(UnitStat.REP_COST));
            if (val != -1) {
                // score by attack effectiveness (how much net reputation we gain), tiebreak by closest enemy
                // batters are implicitly targeted first because they are worth 75 rep, which is more than the others

                // add 8 because we want to attack anything within distance 8 no matter what
                final int attackScore = 10 * (val / 9) - uc.getLocation().distanceSquared(enemies[i].getLocation()) + 8;
                if (bestAttackScore < attackScore) {
                    bestAttackScore = attackScore;
                    toAttack = enemies[i];
                }
            }
        }

//        debugBytecode("pickTarget end. score = " + bestAttackScore);
        return toAttack;
    }

    boolean attack(UnitInfo target) {  // return value currently unused but might be useful later, will leave for now
//        uc.println("attack start " + uc.getEnergyUsed());

        final int val = directionToMoveToAttack(target.getLocation(), target.getType().getStat(UnitStat.REP_COST));
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

    int directionToMoveToAttack(Location targetLocation, float targetCost) {
//        debugBytecode("directionToMove start");
        if (!uc.canMove()) {
            if (Util.chebyshevDistance(uc.getLocation(), targetLocation) <= 1) {
                final int effectiveness = hitEffectiveness(targetLocation, targetCost, uc.getLocation().directionTo(targetLocation));
                if (effectiveness >= 0) {
//                    debugBytecode("directionToMove end0");
                    return effectiveness * 9 + Direction.ZERO.ordinal();
                }
            }
//            debugBytecode("directionToMove end1");
            return -1;
        }

        int bestDir = -1;
        int bestEffectiveness = 0;
        // TODO try cardinal directions first so that move cooldown is smaller
        for (int i = 8; i >= 0; --i) {
            if (uc.canMove(Direction.values()[i]) || i == 8) {
                final Location loc = uc.getLocation().add(Direction.values()[i]);
                if (loc.distanceSquared(targetLocation) <= 2) {
                    final int effectiveness = hitEffectiveness(targetLocation, targetCost, loc.directionTo(targetLocation));
                    if (bestEffectiveness <= effectiveness) {
                        bestEffectiveness = effectiveness;
                        bestDir = i;
                    }
                }
            }
        }

//        debugBytecode("directionToMove end2");
        return bestEffectiveness * 9 + bestDir;
    }

    int hitEffectiveness(Location targetLocation, float targetCost, Direction dir) {
//        debugBytecode("hitEffectiveness start");

        // run this GameConstants.MAX_STRENGTH=3 times
        Location loc = targetLocation.add(dir);
        if (uc.getLocation().distanceSquared(loc) > VISION) return 0;
        if (uc.isOutOfMap(loc)) return (int) targetCost;
        MapObject map = uc.senseObjectAtLocation(loc, true);
        if (map == MapObject.BALL || map == MapObject.WATER) return (int) targetCost;
        UnitInfo unit = uc.senseUnitAtLocation(loc);
        if (unit != null) return (int) targetCost + (unit.getTeam() == uc.getOpponent() ? 1 : -1) * (int) unit.getType().getStat(UnitStat.REP_COST);

        loc = loc.add(dir);
        if (uc.getLocation().distanceSquared(loc) > VISION) return 0;
        if (uc.isOutOfMap(loc)) return (int) targetCost;
        map = uc.senseObjectAtLocation(loc, true);
        if (map == MapObject.BALL || map == MapObject.WATER) return (int) targetCost;
        unit = uc.senseUnitAtLocation(loc);
        if (unit != null) return (int) targetCost + (unit.getTeam() == uc.getOpponent() ? 1 : -1) * (int) unit.getType().getStat(UnitStat.REP_COST);

        loc = loc.add(dir);
        if (uc.getLocation().distanceSquared(loc) > VISION) return 0;
        if (uc.isOutOfMap(loc)) return (int) targetCost;
        map = uc.senseObjectAtLocation(loc, true);
        if (map == MapObject.BALL || map == MapObject.WATER) return (int) targetCost;
        unit = uc.senseUnitAtLocation(loc);
        if (unit != null) return (int) targetCost + (unit.getTeam() == uc.getOpponent() ? 1 : -1) * (int) unit.getType().getStat(UnitStat.REP_COST);

//        debugBytecode("hitEffectiveness end");
        return 0;
    }

    /**
     * Picks a nearby ball to hit, or returns -1 if no ball should be hit
     * @param balls list of nearby balls that can be hit
     * @return (idx * 9 + direction ordinal) * 4 + strength to hit ball or -1
     */
    int pickBallIndex(Location[] balls) {
//        debugBytecode("pickBallIndex start");
        int bestBallIdx = -1;
        int bestDirOrd = -1;
        int bestStrength = -1;
        int bestAttackScore = -1;
        for (int i = balls.length - 1; i >= 0; --i) {
            // TODO unroll and try cardinal directions first so that move cooldown is smaller
            for (int d = 8; d >= 0; --d) {
                if (uc.canMove(Direction.values()[d]) || i == 8) {
                    final Location loc = uc.getLocation().add(Direction.values()[d]);
                    if (loc.distanceSquared(balls[i]) <= 2) {
                        final int strength = ballStrength(balls[i], loc.directionTo(balls[i]));
                        if (strength > 0 && bestAttackScore < strength / 4) {
                            bestAttackScore = strength / 4;
                            bestBallIdx = i;
                            bestDirOrd = d;
                            bestStrength = strength % 4;
                        }
                    }
                }
            }
        }
//        debugBytecode("pickBallIndex end. score = " + bestAttackScore);
        return bestBallIdx == -1 ? -1 : (bestBallIdx * 9 + bestDirOrd) * 4 + bestStrength;
    }

    /**
     * Recommends a strength to hit the ball at, given the ball and direction it will be hit
     * @return score * 4 + strength, or -1 if the ball should not be hit
     */
    int ballStrength(Location targetLocation, Direction dir) {
        Location loc = targetLocation.add(dir);
        if (uc.getLocation().distanceSquared(loc) > VISION) return -1;
        if (uc.isOutOfMap(loc)) return 0;  // hitting it out is okay
        MapObject map = uc.senseObjectAtLocation(loc, true);
        if (map == MapObject.BALL || map == MapObject.WATER) return 0;
        UnitInfo unit = uc.senseUnitAtLocation(loc);
        final int score1 = unit == null ? 0 : ((unit.getTeam() == uc.getOpponent() ? 1 : -1) * (unit.getType() == UnitType.HQ ? 100 : (int) unit.getType().getStat(UnitStat.REP_COST)));

        loc = loc.add(dir);
        if (uc.getLocation().distanceSquared(loc) > VISION) return score1 <= 1 ? -1 : score1 * 4 + 1;  // higher cutoff because we might be hitting it back to an enemy batter
        if (uc.isOutOfMap(loc)) return score1 <= 0 ? -1 : score1 * 4 + 1;
        map = uc.senseObjectAtLocation(loc, true);
        if (map == MapObject.BALL || map == MapObject.WATER) return score1 <= 0 ? -1 : score1 * 4 + 1;
        unit = uc.senseUnitAtLocation(loc);
        final int score2 = score1 + (unit == null ? 0 : ((unit.getTeam() == uc.getOpponent() ? 1 : -1) * (unit.getType() == UnitType.HQ ? 100 : (int) unit.getType().getStat(UnitStat.REP_COST))));

        loc = loc.add(dir);
        if (uc.getLocation().distanceSquared(loc) > VISION) return score2 > score1 ? (score2 <= 1 ? -1 : score2 * 4 + 2) : (score1 <= 1 ? -1 : score1 * 4 + 1);
        if (uc.isOutOfMap(loc)) return score2 >= score1 ? (score2 <= 0 ? -1 : score2 * 4 + 2) : (score1 <= 0 ? -1 : score1 * 4 + 1);
        map = uc.senseObjectAtLocation(loc, true);
        if (map == MapObject.BALL || map == MapObject.WATER) return score2 > score1 ? (score2 <= 0 ? -1 : score2 * 4 + 2) : (score1 <= 0 ? -1 : score1 * 4 + 1);
        unit = uc.senseUnitAtLocation(loc);
        final int score3 = score2 + (unit == null ? 0 : ((unit.getTeam() == uc.getOpponent() ? 1 : -1) * (unit.getType() == UnitType.HQ ? 100 : (int) unit.getType().getStat(UnitStat.REP_COST))));

        return score3 > score2 && score3 > score1 ?
                (score3 <= 0 ? -1 : score3 * 4 + 3) :
                (score2 > score1 ? (score2 <= 0 ? -1 : score2 * 4 + 2) : (score1 <= 0 ? -1 : score1 * 4 + 1));
    }

    int pickTargetIndexToSelfBat(UnitInfo[] allies) {
        for (int i = allies.length - 1; i >= 0; --i) {
            if (allies[i].getType() != UnitType.BATTER ||
                    !uc.canSchedule(allies[i].getID()) ||
                    uc.getInfo().getCurrentMovementCooldown() >= 1 ||
                    uc.getInfo().getCurrentActionCooldown() >= 1 ||
                    Util.chebyshevDistance(uc.getLocation(), allies[i].getLocation()) > 2) {
                continue;
            }
            if (!uc.canMove()) {
                if (Util.chebyshevDistance(uc.getLocation(), allies[i].getLocation()) <= 1) {
                    final int strength = selfBatStrength(allies[i], uc.getLocation().directionTo(allies[i].getLocation()));
                    if (strength > 0) return (i * 9 + Direction.ZERO.ordinal()) * 4 + strength;
                }
                continue;
            }

            // TODO try cardinal directions first so that move cooldown is smaller
            for (int d = 8; d >= 0; --d) {
                if (uc.canMove(Direction.values()[d]) || i == 8) {
                    final Location loc = uc.getLocation().add(Direction.values()[d]);
                    if (loc.distanceSquared(allies[i].getLocation()) <= 2) {
                        final int strength = selfBatStrength(allies[i], loc.directionTo(allies[i].getLocation()));
                        if (strength > 0) return (i * 9 + d) * 4 + strength;
                    }
                }
            }
        }
        return -1;
    }

    int selfBatStrength(UnitInfo target, Direction dir) {
        Location loc = target.getLocation().add(dir);
        if (uc.getLocation().distanceSquared(loc) > VISION || uc.isOutOfMap(loc)) return 0;
        MapObject map = uc.senseObjectAtLocation(loc, true);
        if (map == MapObject.BALL || map == MapObject.WATER) return 0;
        if (uc.senseUnitAtLocation(loc) != null) return 0;

        loc = loc.add(dir);
        if (uc.getLocation().distanceSquared(loc) > VISION || uc.isOutOfMap(loc)) return 1;
        map = uc.senseObjectAtLocation(loc, true);
        if (map == MapObject.BALL || map == MapObject.WATER) return 1;
        if (uc.senseUnitAtLocation(loc) != null) return 1;

        loc = loc.add(dir);
        if (uc.getLocation().distanceSquared(loc) > VISION || uc.isOutOfMap(loc)) return 2;
        map = uc.senseObjectAtLocation(loc, true);
        if (map == MapObject.BALL || map == MapObject.WATER) return 2;
        if (uc.senseUnitAtLocation(loc) != null) return 2;

        return 3;
    }

    void selfBat(UnitInfo ally, int strength, Direction toMove) {
        if (toMove != Direction.ZERO && uc.canMove(toMove)) {
            uc.move(toMove);
        }
        if (uc.canBat(uc.getLocation().directionTo(ally.getLocation()), strength)) {
            uc.schedule(ally.getID());
            uc.bat(uc.getLocation().directionTo(ally.getLocation()), strength);
        }
    }

    void patrol(Location patrolLoc, UnitInfo[] enemies, int directionOkay) {
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
                if (uc.getLocation().distanceSquared(patrolLoc) < IDLE_DISTANCE) {
                    //randomly move around in the zone
                    Util.tryMoveInDirection(uc, Direction.values()[(int)(uc.getRandomDouble() * 8)]);
                }
                //outside idle zone
                else{
                    Direction toMove = comms.directionViaFocalPoint(patrolLoc, directionOkay);
                    if (toMove == null) toMove = bg.move(patrolLoc);
                    if (toMove != null && toMove != Direction.ZERO && uc.canMove(toMove)) {
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
                    if (uc.canMove(toMove) && toMove != Direction.ZERO) {
                        uc.move(toMove);
                    }
                }
            }
        }
    }

    Direction spreadOut(Location idealTarget) {
        return spreadOut(idealTarget.x - uc.getLocation().x, idealTarget.y - uc.getLocation().y);
    }
    Direction spreadOut() {
        return spreadOut(0, 0);
    }

    Direction spreadOut(float weightX, float weightY) {
        final Location currentLocation = uc.getLocation();
        float x = currentLocation.x, y = currentLocation.y;
        UnitInfo[] allies = uc.senseUnits(VISION, uc.getTeam());
        float dist;
        float allyWeightX = 0, allyWeightY = 0;
        Location loc;
        for (int i = allies.length; i --> 0;) {
            if (allies[i].getType() == UnitType.BATTER || allies[i].getType() == UnitType.PITCHER) {
                loc = allies[i].getLocation();
                dist = currentLocation.distanceSquared(loc) + 0.01f; // Avoid div by 0
                allyWeightX -= (loc.x - x) / dist;
                allyWeightY -= (loc.y - y) / dist;
            }
        }
        weightX += allyWeightX * 30;
        weightY += allyWeightY * 30;

        int finalDx = uc.getRandomDouble() * 40 > weightX + 20 ? -1 : 1;
        int finalDy = uc.getRandomDouble() * 40 > weightY + 20 ? -1 : 1;
        return Direction.getDirection(finalDx, finalDy);
    }
}
