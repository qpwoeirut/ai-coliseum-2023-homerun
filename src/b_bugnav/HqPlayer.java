package b_bugnav;

import aic2023.user.*;

public class HqPlayer extends BasePlayer {
    private final int OFFSET = 10;  // vision radius is 8

    HqPlayer(UnitController uc) {
        super(uc);
    }

    void run() {
        final float SENSING_RADIUS = 64;

        Location[] visibleBases = uc.senseObjects(MapObject.BASE, SENSING_RADIUS);
        Location[] visibleStadiums = uc.senseObjects(MapObject.STADIUM, SENSING_RADIUS);
        Location[] visibleWater = uc.senseObjects(MapObject.WATER, SENSING_RADIUS);

        comms.reportNewBases(visibleBases);
        comms.reportNewStadiums(visibleStadiums);
        comms.reportNewWater(visibleWater);

        // handle first turn separately, if we can see a stadium
        if (visibleStadiums.length >= 2) {
            // TODO: pick two closest stadiums instead of just first two
            Direction dir1 = uc.getLocation().directionTo(visibleStadiums[0]);
            Direction dir2 = uc.getLocation().directionTo(visibleStadiums[1]);

            recruitUnitToward(UnitType.PITCHER, dir1);
            recruitUnitToward(UnitType.PITCHER, dir2);
        } else if (visibleStadiums.length == 1) {
            Direction dir = uc.getLocation().directionTo(visibleStadiums[0]);

            recruitUnitToward(UnitType.PITCHER, dir);
            recruitUnitToward(UnitType.CATCHER, Direction.values()[(int)(uc.getRandomDouble() * 8)]);
        } else {
            recruitUnitToward(UnitType.CATCHER, Direction.values()[(int)(uc.getRandomDouble() * 8)]);
            recruitUnitToward(UnitType.PITCHER, Direction.values()[(int)(uc.getRandomDouble() * 8)]);
        }

        final int hqX = uc.getLocation().x;
        final int hqY = uc.getLocation().y;
        // handle other turns
        while (true) {
            comms.checkIn();

//            uc.println("bases: " + comms.countBases() + ", stadiums: " + comms.countStadiums() + ". batters: " + comms.countBatters() + ", catchers: " + comms.countCatchers() + ", pitchers: " + comms.countPitchers());

            UnitInfo[] enemies = senseAndReportEnemies();
            boolean enemyBattersNearby = false;
            boolean[][] hasEnemyBatter = new boolean[20][20];
            for (int i = enemies.length - 1; i >= 0; --i) {
                if (enemies[i].getType() == UnitType.BATTER) {
                    enemyBattersNearby = true;
                    hasEnemyBatter[enemies[i].getLocation().x - hqX + OFFSET][enemies[i].getLocation().y - hqY + OFFSET] = true;
                }
            }

            if (comms.countBases() + comms.countStadiums() > comms.countPitchers()) {
                while (uc.getReputation() >= UnitType.PITCHER.getStat(UnitStat.REP_COST) && recruitUnitSafely(UnitType.PITCHER, hasEnemyBatter)) {
                    // recruitUnitSafely will spawn units until we can't anymore
                }
            } else if (enemyBattersNearby || comms.countBatters() <= comms.countCatchers() * 4) {
                recruitUnitSafely(UnitType.BATTER, hasEnemyBatter);
            } else {
                recruitUnitSafely(UnitType.CATCHER, hasEnemyBatter);
            }
            uc.yield();
        }
    }

    void recruitUnitToward(UnitType type, Direction dir) {
        if (uc.canRecruitUnit(type, dir)) uc.recruitUnit(type, dir);
        else if (uc.canRecruitUnit(type, dir.rotateLeft())) uc.recruitUnit(type, dir.rotateLeft());
        else if (uc.canRecruitUnit(type, dir.rotateRight())) uc.recruitUnit(type, dir.rotateRight());
        else if (uc.canRecruitUnit(type, dir.rotateLeft().rotateLeft())) uc.recruitUnit(type, dir.rotateLeft().rotateLeft());
        else if (uc.canRecruitUnit(type, dir.rotateRight().rotateRight())) uc.recruitUnit(type, dir.rotateRight().rotateRight());
        else if (uc.canRecruitUnit(type, dir.opposite().rotateRight())) uc.recruitUnit(type, dir.opposite().rotateRight());
        else if (uc.canRecruitUnit(type, dir.opposite().rotateLeft())) uc.recruitUnit(type, dir.opposite().rotateLeft());
        else if (uc.canRecruitUnit(type, dir.opposite())) uc.recruitUnit(type, dir.opposite());
    }

    boolean recruitUnitSafely(UnitType type, boolean[][] hasEnemyBatter) {
        for (int i = 7; i >= 0; --i) {
            if (uc.canRecruitUnit(type, Direction.values()[i]) && directionIsSafe(Direction.values()[i], hasEnemyBatter)) {
                uc.recruitUnit(type, Direction.values()[i]);
                return true;
            }
        }
        return false;
    }

    boolean directionIsSafe(Direction dir, boolean[][] hasEnemyBatter) {
        for (int dx = -2; dx <= 2; ++dx) {
            for (int dy = -2; dy <= 2; ++dy) {
                if (hasEnemyBatter[dx + dir.dx + OFFSET][dy + dir.dy + OFFSET]) return false;
            }
        }
        return true;
    }
}
