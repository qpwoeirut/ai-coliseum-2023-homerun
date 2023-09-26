package c1_moreaggressive;

import aic2023.user.*;
import c1_moreaggressive.util.Communications;

abstract public class BasePlayer {
    protected final UnitController uc;
    protected final Communications comms;
    protected final BugMover bg;
    protected final float VISION;

    BasePlayer(UnitController uc) {
        this.uc = uc;
        this.comms = new Communications(uc);
        this.bg = new BugMover(uc);

        VISION = uc.getType().getStat(UnitStat.VISION_RANGE);
    }

    void move(Location fin) {
        uc.move(bg.move(fin));
    }

    Location [] senseAndReportBases ()
    {
        Location[] bases = uc.senseObjects(MapObject.BASE, VISION);
        comms.reportNewBases(bases);
        return bases;
    }

    Location [] senseAndReportStadiums ()
    {
        Location[] stadiums = uc.senseObjects(MapObject.STADIUM, VISION);
        comms.reportNewStadiums(stadiums);
        return stadiums;
    }

    UnitInfo[] senseAndReportEnemies() {
        UnitInfo[] enemies = uc.senseUnits(VISION, uc.getOpponent());
        for (int i = enemies.length - 1; i >= 0; --i) {
            if (enemies[i].getType() != UnitType.HQ) {
                comms.reportEnemySighting(enemies[i].getLocation(), enemies[i].getType() == UnitType.BATTER ? 8 : 3);
            }
            // TODO: tune urgency numbers, consider changing behaviors for different units doing the sensing
        }
        return enemies;
    }


}