package h2_micro;

import aic2023.user.*;
import h2_micro.util.Communications;
import h2_micro.util.Util;

abstract public class BasePlayer {
    protected final UnitController uc;
    protected final Communications comms;
    protected final BugMover bg;
    protected final float VISION;
    protected final int URGENCY_FACTOR;

    BasePlayer(UnitController uc) {
        this.uc = uc;
        this.comms = new Communications(uc);
        this.bg = new BugMover(uc);

        VISION = uc.getType().getStat(UnitStat.VISION_RANGE);
        if (uc.getType() == UnitType.BATTER) {
            URGENCY_FACTOR = 1;
        } else if (uc.getType() == UnitType.CATCHER) {
            URGENCY_FACTOR = 2;
        } else if (uc.getType() == UnitType.PITCHER) {
            URGENCY_FACTOR = 10;
        } else if (uc.getType() == UnitType.HQ) {
            URGENCY_FACTOR = 6;
        } else URGENCY_FACTOR = 1;
    }

    protected Location[] senseAndReportBases() {
        Location[] bases = uc.senseObjects(MapObject.BASE, VISION);
        comms.reportNewBases(bases);
        return bases;
    }

    protected Location[] senseAndReportStadiums() {
        Location[] stadiums = uc.senseObjects(MapObject.STADIUM, VISION);
        comms.reportNewStadiums(stadiums);
        return stadiums;
    }

    protected UnitInfo[] senseAndReportEnemies() {
        UnitInfo[] enemies = uc.senseUnits(VISION, uc.getOpponent());
        comms.reportEnemySightings(enemies, URGENCY_FACTOR);
        return enemies;
    }

    protected void senseAndReportGrassIfNecessary() {
        if (uc.getEnergyLeft() >= 1500 && !comms.grassAlreadySensedAtLocation()) {
            comms.reportNewGrassAtEndOfTurn(uc.senseObjects(MapObject.GRASS, VISION));
        }
    }

    protected void endTurn() {
        final int currentRound = uc.getRound();
        senseAndReportGrassIfNecessary();
        comms.useRemainingBytecode(currentRound);
        if (uc.getRound() == currentRound) uc.yield();
    }

    // ignores water
    protected boolean enemyBatterCanHitLocation(float movementCooldown, Location loc, UnitInfo[] enemies) {
        int turnsBeforeCanMove = (int)movementCooldown;
//        debug("turns: " + turnsBeforeCanMove + ", loc: " + loc);
        for (int i = enemies.length - 1; i >= 0; --i) {
//            debug(i + " " + enemies[i].getCurrentActionCooldown() + " " + enemies[i].getCurrentMovementCooldown() + " " + Util.movementAdjacentDistance(loc, enemies[i].getLocation()) + " " + comms.lowerBoundDistance(loc, enemies[i].getLocation()));
            if (enemies[i].getType() == UnitType.BATTER &&
                    turnsBeforeCanMove >= (int)(enemies[i].getCurrentActionCooldown() - 1) &&
                    Util.movementAdjacentDistance(loc, enemies[i].getLocation()) + (int)(enemies[i].getCurrentMovementCooldown() - 1) <= turnsBeforeCanMove + 1.01f &&
                    comms.lowerBoundDistance(loc, enemies[i].getLocation()) <= (2 + turnsBeforeCanMove) * comms.DISTANCE_ROOT
            ) {
                return true;
            }
        }
        return false;
    }

    protected boolean enemyBatterCanHitNewLocation(Direction dir, UnitInfo[] enemies) {
        return enemyBatterCanHitLocation(
                uc.getInfo().getCurrentMovementCooldown() + uc.getType().getStat(UnitStat.MOVEMENT_COOLDOWN) * (dir.ordinal() % 2 == 1 ? 1.14142f : 1f),
                uc.getLocation().add(dir),
                enemies
        );
    }

    protected int calculateOkayDirections(UnitInfo[] enemies) {
        return  // i'th bit represent direction with ordinal i
                (enemyBatterCanHitNewLocation(Direction.NORTH    , enemies) ? 0 : 1     ) |
                (enemyBatterCanHitNewLocation(Direction.NORTHWEST, enemies) ? 0 : 1 << 1) |
                (enemyBatterCanHitNewLocation(Direction.WEST     , enemies) ? 0 : 1 << 2) |
                (enemyBatterCanHitNewLocation(Direction.SOUTHWEST, enemies) ? 0 : 1 << 3) |
                (enemyBatterCanHitNewLocation(Direction.SOUTH    , enemies) ? 0 : 1 << 4) |
                (enemyBatterCanHitNewLocation(Direction.SOUTHEAST, enemies) ? 0 : 1 << 5) |
                (enemyBatterCanHitNewLocation(Direction.EAST     , enemies) ? 0 : 1 << 6) |
                (enemyBatterCanHitNewLocation(Direction.NORTHEAST, enemies) ? 0 : 1 << 7) |
                (enemyBatterCanHitLocation(uc.getInfo().getCurrentMovementCooldown(), uc.getLocation(), enemies) ? 0 : 1 << 8);
    }

    protected void debug(String message) {
        if (uc.getRound() <= 100) uc.println(message);
    }

    protected void debugBytecode(String message) {
        debug(message + " " + uc.getEnergyUsed());
    }
}