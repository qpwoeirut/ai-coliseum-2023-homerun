package g_fixgodnavbugs;

import aic2023.user.*;

public class HqPlayer extends BasePlayer {
    private final int OFFSET = 10;  // vision radius is 8
    private int grassSensingIncrement, currentGrassVision;

    HqPlayer(UnitController uc) {
        super(uc);
    }

    void run() {
        Location[] visibleBases = senseAndReportBases();
        Location[] visibleStadiums = senseAndReportStadiums();
        grassSensingIncrement = Math.max(2, 20 / (visibleBases.length + visibleStadiums.length + 1));
        currentGrassVision = grassSensingIncrement;

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
            recruitUnitToward(UnitType.CATCHER, Direction.values()[(int) (uc.getRandomDouble() * 8)]);
        } else {
            recruitUnitToward(UnitType.CATCHER, Direction.values()[(int) (uc.getRandomDouble() * 8)]);
            recruitUnitToward(UnitType.PITCHER, Direction.values()[(int) (uc.getRandomDouble() * 8)]);
        }

        final int hqX = uc.getLocation().x;
        final int hqY = uc.getLocation().y;
        // handle other turns
        while (true) {
            comms.checkIn();
            comms.decayEnemySightingUrgencies();
            UnitInfo[] enemies = senseAndReportEnemies();

            // uc.println("Round " + uc.getRound() + ". bases: " + comms.countBases() + ", stadiums: " + comms.countStadiums() + ". batters: " + comms.countBatters() + ", catchers: " + comms.countCatchers() + ", pitchers: " + comms.countPitchers());

            int enemyBattersNearby = 0;
            boolean[][] hasEnemyBatter = new boolean[20][20];
            for (int i = enemies.length - 1; i >= 0; --i) {
                if (enemies[i].getType() == UnitType.BATTER) {
                    ++enemyBattersNearby;
                    hasEnemyBatter[enemies[i].getLocation().x - hqX + OFFSET][enemies[i].getLocation().y - hqY + OFFSET] = true;
                }
            }

            if (comms.countBatters() * 2 < enemyBattersNearby * 3 || comms.countBatters() * 10 < comms.listEnemySightings()) {
                while (uc.getReputation() >= UnitType.BATTER.getStat(UnitStat.REP_COST) && recruitUnitNextToEnemy(UnitType.BATTER, hasEnemyBatter)) {
                    // recruit batters to hit the nearby enemy batters
                }
                while (uc.getReputation() >= UnitType.BATTER.getStat(UnitStat.REP_COST) && recruitUnitRandomly(UnitType.BATTER)) {
                    // then just recruit batters in general
                }
            } else if (comms.countBases() + comms.countStadiums() > comms.countPitchers()) {
                while (uc.getReputation() >= UnitType.PITCHER.getStat(UnitStat.REP_COST) && recruitUnitSafely(UnitType.PITCHER, hasEnemyBatter)) {
                    // recruitUnitSafely will spawn units until we can't anymore
                }
            } else if (comms.countBatters() <= comms.countCatchers() * 4 || comms.countCatchers() >= 10) {
                recruitUnitSafely(UnitType.BATTER, hasEnemyBatter);
            } else {
                recruitUnitSafely(UnitType.CATCHER, hasEnemyBatter);
            }

            endTurn();
        }
    }

    @Override
    protected void senseAndReportGrassIfNecessary() {
        // first round is round 0
        if (currentGrassVision < VISION) {
            comms.reportNewGrassAtEndOfTurn(uc.senseObjects(MapObject.GRASS, currentGrassVision));
            currentGrassVision = Math.min(currentGrassVision + grassSensingIncrement, (int)VISION);
        } else if (currentGrassVision == VISION) {
            comms.reportNewGrassAtEndOfTurn(uc.senseObjects(MapObject.GRASS, currentGrassVision));
            ++currentGrassVision;
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

    boolean recruitUnitNextToEnemy(UnitType type, boolean[][] hasEnemyBatter) {
        for (int i = 7; i >= 0; --i) {
            if (uc.canRecruitUnit(type, Direction.values()[i]) && directionAdjacentToEnemy(Direction.values()[i], hasEnemyBatter)) {
                uc.recruitUnit(type, Direction.values()[i]);
                return true;
            }
        }
        for (int i = 7; i >= 0; --i) {
            if (uc.canRecruitUnit(type, Direction.values()[i]) && !directionIsSafe(Direction.values()[i], hasEnemyBatter)) {
                uc.recruitUnit(type, Direction.values()[i]);
                return true;
            }
        }
        // TODO: experiment with scheduling?
        return false;
    }

    // this isnt actually random, just statically shuffled, but hopefully it should be fine
    boolean recruitUnitRandomly(UnitType type) {
        if (uc.canRecruitUnit(type, Direction.NORTHEAST)) {
            uc.recruitUnit(type, Direction.NORTHEAST);
            return true;
        }
        if (uc.canRecruitUnit(type, Direction.SOUTH)) {
            uc.recruitUnit(type, Direction.SOUTH);
            return true;
        }
        if (uc.canRecruitUnit(type, Direction.WEST)) {
            uc.recruitUnit(type, Direction.WEST);
            return true;
        }
        if (uc.canRecruitUnit(type, Direction.SOUTHEAST)) {
            uc.recruitUnit(type, Direction.SOUTHEAST);
            return true;
        }
        if (uc.canRecruitUnit(type, Direction.NORTHWEST)) {
            uc.recruitUnit(type, Direction.NORTHWEST);
            return true;
        }
        if (uc.canRecruitUnit(type, Direction.SOUTHWEST)) {
            uc.recruitUnit(type, Direction.SOUTHWEST);
            return true;
        }
        if (uc.canRecruitUnit(type, Direction.NORTH)) {
            uc.recruitUnit(type, Direction.NORTH);
            return true;
        }
        return false;
    }

    boolean directionAdjacentToEnemy(Direction dir, boolean[][] hasEnemyBatter) {
        final int x = OFFSET + dir.dx;
        final int y = OFFSET + dir.dy;
        return hasEnemyBatter[x - 1][y - 1] || hasEnemyBatter[x - 1][y] || hasEnemyBatter[x - 1][y + 1] ||
               hasEnemyBatter[x    ][y - 1] || hasEnemyBatter[x    ][y] || hasEnemyBatter[x    ][y + 1] ||
               hasEnemyBatter[x + 1][y - 1] || hasEnemyBatter[x + 1][y] || hasEnemyBatter[x + 1][y + 1];
    }

    boolean directionIsSafe(Direction dir, boolean[][] hasEnemyBatter) {
        final int x = OFFSET + dir.dx;
        final int y = OFFSET + dir.dy;
        for (int dx = -2; dx <= 2; ++dx) {
            if (hasEnemyBatter[x + dx][y - 2] ||
                    hasEnemyBatter[x + dx][y - 1] ||
                    hasEnemyBatter[x + dx][y    ] ||
                    hasEnemyBatter[x + dx][y + 1] ||
                    hasEnemyBatter[x + dx][y + 2]) {
                return false;
            }
        }
        return true;
    }
}
