package d_player;

import aic2023.user.*;
import d_player.util.Communications;

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

    protected void move(Location fin) {
        uc.move(bg.move(fin));
    }

    protected Location [] senseAndReportBases ()
    {
        Location[] bases = uc.senseObjects(MapObject.BASE, VISION);
        comms.reportNewBases(bases);
        return bases;
    }

    protected Location [] senseAndReportStadiums ()
    {
        Location[] stadiums = uc.senseObjects(MapObject.STADIUM, VISION);
        comms.reportNewStadiums(stadiums);
        return stadiums;
    }

    protected UnitInfo[] senseAndReportEnemies() {
        UnitInfo[] enemies = uc.senseUnits(VISION, uc.getOpponent());
        reportEnemies(enemies);
        return enemies;
    }

    protected void reportEnemies(UnitInfo[] enemies) {
        for (int i = enemies.length - 1; i >= 0; --i) {
            if (enemies[i].getType() != UnitType.HQ) {
                comms.reportEnemySighting(enemies[i].getLocation(), enemies[i].getType() == UnitType.BATTER ? 8 : 3);
            }
            // TODO: tune urgency numbers, consider changing behaviors for different units doing the sensing
        }
    }
}