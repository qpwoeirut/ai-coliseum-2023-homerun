package c2_squads;

import aic2023.user.*;
import c1_moreaggressive.util.Util;

public class BatterPlayer extends BasePlayer {
    BatterPlayer(UnitController uc) {
        super(uc);
    }

    void run() {
        while (true) {
            comms.checkIn();
            senseAndReportBases();
            senseAndReportStadiums();

            UnitInfo[] enemies = senseAndReportEnemies();
            final UnitInfo toAttack = pickTargetToAttack(enemies);
            if (toAttack != null) {
//                uc.println(toAttack.getLocation().x + " " + toAttack.getLocation().y);
                attack(toAttack);
            }

            if (uc.canMove()) {
                final UnitInfo nearestEnemyBatter = Util.getNearest(uc.getLocation(), enemies, UnitType.BATTER);
                final UnitInfo nearestEnemy = Util.getNearest(uc.getLocation(), enemies);
                if (nearestEnemyBatter == null) {
                    if (nearestEnemy != null) {
                        Util.tryMoveInDirection(uc, uc.getLocation().directionTo(nearestEnemy.getLocation()));
                    } else {
                        final int reportedEnemyCount = comms.listEnemySightings();
                        final int targetEnemySightingIndex = Util.getMaxIndex(comms.returnedUrgencies, reportedEnemyCount);
                        if (targetEnemySightingIndex == -1) {
                            Util.tryMoveInDirection(uc, Direction.values()[(int)(uc.getRandomDouble() * 8)]);
                        } else {
                            final Direction toMove = bg.move(comms.returnedLocations[targetEnemySightingIndex]);
                            if (uc.canMove(toMove)) {
                                uc.move(toMove);
                            } else {
                                Util.tryMoveInDirection(uc, uc.getLocation().directionTo(comms.returnedLocations[targetEnemySightingIndex]));
                            }
                        }
                    }
                } else {
                    if (uc.getLocation().distanceSquared(nearestEnemyBatter.getLocation()) <= 18) {
                        Util.tryMoveInDirection(uc, nearestEnemyBatter.getLocation().directionTo(uc.getLocation()));
                    } else {
                        Util.tryMoveInDirection(uc, uc.getLocation().directionTo(nearestEnemyBatter.getLocation()));
                    }
                }
            }
            uc.yield();
        }
    }

    UnitInfo pickTargetToAttack(UnitInfo[] enemies) {
        if (!uc.canAct()) return null;
//        uc.println("pickTarget start " + uc.getEnergyUsed());

        UnitInfo toAttack = null;
        int bestAttackScore = -1;
        for (int i = enemies.length - 1; i >= 0; --i) {
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

//        uc.println("pickTarget end " + uc.getEnergyUsed());
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
//        uc.println("directionToMove start " + uc.getEnergyUsed());
        if (!uc.canMove()) {
            if (uc.getLocation().distanceSquared(target.getLocation()) <= 2) {
                final int effectiveness = hitEffectiveness(target, uc.getLocation().directionTo(target.getLocation()));
                if (effectiveness >= 0) {
                    return effectiveness * 9 + Direction.ZERO.ordinal();
                }
            }
//            uc.println("directionToMove end " + uc.getEnergyUsed());
            return -1;
        }

        int bestDir = -1;
        int bestEffectiveness = -1;
        // try cardinal directions first so that move cooldown is smaller
        for (int i = 8; i >= 0; --i) {
            final Location loc = uc.getLocation().add(Direction.values()[i]);
            if (uc.canMove(Direction.values()[i]) && loc.distanceSquared(target.getLocation()) <= 2) {
                int effectiveness = hitEffectiveness(target, loc.directionTo(target.getLocation()));
                if (bestEffectiveness < effectiveness) {
                    bestEffectiveness = effectiveness;
                    bestDir = i;
                }
            }
        }

//        uc.println("directionToMove end " + uc.getEnergyUsed());
        return bestEffectiveness * 9 + bestDir;
    }

    int hitEffectiveness(UnitInfo target, Direction dir) {
//        uc.println("hitEffectiveness start " + uc.getEnergyUsed());

        Location loc = target.getLocation();
        for (int i = 0; i < GameConstants.MAX_STRENGTH; ++i) {
//            uc.println("hitEffectiveness start " + i + " " + uc.getEnergyUsed());
            loc = loc.add(dir);

            if (uc.getLocation().distanceSquared(loc) > uc.getType().getStat(UnitStat.VISION_RANGE)) break;

            if (uc.isOutOfMap(loc)) {
//                uc.println("hitEffectiveness end " + uc.getEnergyUsed());
                return (int)target.getType().getStat(UnitStat.REP_COST);
            }

            final MapObject map = uc.senseObjectAtLocation(loc, true);
            if (map == MapObject.BALL || map == MapObject.WATER) {
//                uc.println("hitEffectiveness end " + uc.getEnergyUsed());
                return (int)target.getType().getStat(UnitStat.REP_COST);
            }

            final UnitInfo unit = uc.senseUnitAtLocation(loc);
            if (unit != null) {
//                uc.println("hitEffectiveness end " + uc.getEnergyUsed());
                return (int)target.getType().getStat(UnitStat.REP_COST) + (unit.getTeam() == uc.getOpponent() ? 1 : -1) * (int)unit.getType().getStat(UnitStat.REP_COST);
            }
        }

//        uc.println("hitEffectiveness end " + uc.getEnergyUsed());
        return 0;
    }
}
