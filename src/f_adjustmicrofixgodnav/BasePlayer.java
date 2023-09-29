package f_adjustmicrofixgodnav;

import aic2023.user.*;
import f_adjustmicrofixgodnav.util.Communications;

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
        if (uc.getRound() == currentRound) comms.useRemainingBytecode();
        if (uc.getRound() == currentRound) uc.yield();
    }

    protected void debug(String message) {
        if (uc.getRound() <= 200) uc.println(message);
    }

    protected void debugBytecode(String message) {
        debug(message + " " + uc.getEnergyUsed());
    }
}