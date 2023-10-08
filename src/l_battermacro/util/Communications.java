package l_battermacro.util;

import aic2023.user.*;

public class Communications {
    private final UnitController uc;

    // for objects (bases, stadiums, water)
    // a[0] = number of specified object
    // a[4i + 1 ... 4i + 4] describes object i with (x, y, id of claiming pitcher or 0 if unclaimed, round that claim was most recently updated)
    private final int OBJECT_SIZE = 4;
    private final int OBJECT_X = 1;
    private final int OBJECT_Y = 2;
    private final int CLAIM_ID = 3;
    private final int CLAIM_ROUND = 4;
    private final int NO_CLAIM = 0;  // IDs are in [1, 10000]
    private final int CLAIM_EXPIRATION = 3;  // if a pitcher doesn't update their claim in 3 rounds, assume they've died

    private final int MAX_OBJECT_COUNT = 3650; // > 60^2
    private final int BASE_OFFSET = 0;
    private final int STADIUM_OFFSET = MAX_OBJECT_COUNT * OBJECT_SIZE;

    private final int CHECK_IN_OFFSET = 30000;
    private final int BATTER_NUMBER = 0;
    private final int CATCHER_NUMBER = 1;
    private final int PITCHER_NUMBER = 2;
    private final int HQ_NUMBER = 3;

    private final int ENEMY_SIGHTING_OFFSET = 40000;
    private final int ENEMY_SIZE = 3;
    private final int ENEMY_X = 1;
    private final int ENEMY_Y = 2;
    private final int ENEMY_URGENCY = 3;
    private final int ENEMY_MERGE_DISTANCE = (int)(UnitType.BATTER.getStat(UnitStat.VISION_RANGE));

    private final int SCOUTING_QUEUE_OFFSET = 50000;
    private final int SCOUTING_QUEUE_SIZE = 5000 - 3;
    private final int SCOUTING_QUEUE_START = SCOUTING_QUEUE_SIZE;
    private final int SCOUTING_QUEUE_END = SCOUTING_QUEUE_SIZE + 1;

    private final int DISTANCE_QUEUE_OFFSET = 55000;
    private final int DISTANCE_QUEUE_SIZE = 95000 - 3;
    private final int DISTANCE_QUEUE_START = DISTANCE_QUEUE_SIZE;
    private final int DISTANCE_QUEUE_END = DISTANCE_QUEUE_SIZE + 1;

    private final int MAP_OFFSET = 150000;
    private final int MAP_DIMENSION = 121;
    // assume our HQ is at (60, 60). This guess is off by at most 60 in either direction, se we need 120 to store the map
    private final int MAP_SIZE = 122 * 122;
    private final int ORIGIN_X = MAP_SIZE - 4, ORIGIN_Y = MAP_SIZE - 3;  // take advantage of extra buffer space
    private final int MAX_MAP_COUNT = ((1_000_000 - MAP_OFFSET) / MAP_SIZE) - 1;

    // passability map states. states can only increase
    // all locations start off as impassable
    private final int UNINITIALIZED = 0;
    private final int UNPROCESSED = 1;  // intermediate state where reportNewObjects has processed but reportNewGrass hasn't
    private final int PASSABLE = 2;
    private final int SENSED = 3;  // implies passable
    private final int DISTANCE_MAP_COUNT = MAP_SIZE - 1;
    // note that the 0th map is the passability map
    // therefore distance maps are 1-indexed

    public final int DISTANCE_UNIT = 10000;  // make this public for easier comparison
    public final int DISTANCE_ROOT = 14142;
    private final int INITIAL_DISTANCE = 1;  // 0 represents INF, since array is 0-initialized
    private final int INF = 1_000_000_000;

    // for functions that require arrays to be returned. this is more efficient because we don't need to allocate an array each time
    public Location[] returnedLocations = new Location[20];  // for both map objects and enemy sightings
    public int[] returnedIds = new int[8];  // only used for map objects
    public int[] returnedUrgencies = new int[20];  // only used for enemy sightings
    public Direction[] returnedDirections = new Direction[200];  // be safe

    public Communications(UnitController uc) {
        this.uc = uc;
        if (uc.getType() == UnitType.HQ) {
            uc.write(MAP_OFFSET + ORIGIN_X, uc.getLocation().x);
            uc.write(MAP_OFFSET + ORIGIN_Y, uc.getLocation().y);
        }
    }

    // ------------------------------------------ BASE AND STADIUM LOCATIONS ------------------------------------------
    // this must be called BEFORE reportNewGrass
    public void reportNewBases(Location[] bases) {
        reportNewObjects(bases, BASE_OFFSET);
    }
    // this must be called BEFORE reportNewGrass
    public void reportNewStadiums(Location[] stadiums) {
        reportNewObjects(stadiums, STADIUM_OFFSET);
    }

    private void reportNewObjects(Location[] locs, int offset) {
        int n = uc.read(offset);
        for (int locIdx = locs.length - 1; locIdx >= 0; --locIdx) {
            final int internalX = convertToInternalX(locs[locIdx].x), internalY = convertToInternalY(locs[locIdx].y);
            if (readMapLocation(0, internalX, internalY) == UNINITIALIZED) {
                writeMapLocation(0, internalX, internalY, UNPROCESSED);
                uc.write(offset + OBJECT_SIZE * n + OBJECT_X, locs[locIdx].x);
                uc.write(offset + OBJECT_SIZE * n + OBJECT_Y, locs[locIdx].y);
                uc.write(offset + OBJECT_SIZE * n + CLAIM_ID, NO_CLAIM);
                uc.write(offset + OBJECT_SIZE * n + CLAIM_ROUND, NO_CLAIM);
                ++n;

                createDistanceMapIfNotExists(locs[locIdx], 1);
            }
        }
        uc.write(offset, n);
    }

    private void reportNewObject(Location loc, int offset) {
        int n = uc.read(offset);
        for (int i = n; i > 0; --i) {
            if (uc.read(offset + OBJECT_SIZE * i + 1) == loc.x && uc.read(offset + OBJECT_SIZE * i + 2) == loc.y) return;
        }

        ++n;
        uc.write(offset, n);
        uc.write(offset + OBJECT_SIZE * n + OBJECT_X, loc.x);
        uc.write(offset + OBJECT_SIZE * n + OBJECT_Y, loc.y);
        uc.write(offset + OBJECT_SIZE * n + CLAIM_ID, NO_CLAIM);
        uc.write(offset + OBJECT_SIZE * n + CLAIM_ROUND, NO_CLAIM);
    }

    // ------------------------------------------ COUNTING UNITS ON OUR TEAM ------------------------------------------
    public void checkIn() {
        final int typeNumber = uc.getType() == UnitType.BATTER ? BATTER_NUMBER : (uc.getType() == UnitType.CATCHER ? CATCHER_NUMBER : (uc.getType() == UnitType.PITCHER ? PITCHER_NUMBER : HQ_NUMBER));
        final int currentIndex = CHECK_IN_OFFSET + uc.getRound() * 4 + typeNumber;
        final int lastIndex = currentIndex - 4;
        final int lastRoundCount = uc.read(lastIndex);
        uc.write(lastIndex, lastRoundCount - 1);

        final int currentRoundCount = uc.read(currentIndex);
        uc.write(currentIndex, currentRoundCount + 1);
    }

    public int countBases() {
        return uc.read(BASE_OFFSET);
    }
    public int countStadiums() {
        return uc.read(STADIUM_OFFSET);
    }
    public int countBatters() {
        return countUnits(BATTER_NUMBER);
    }
    public int countCatchers() {
        return countUnits(CATCHER_NUMBER);
    }
    public int countPitchers() {
        return countUnits(PITCHER_NUMBER);
    }
    private int countUnits(int typeNumber) {
        final int currentIndex = CHECK_IN_OFFSET + uc.getRound() * 4 + typeNumber;
        final int lastIndex = currentIndex - 4;
        return uc.read(lastIndex) + uc.read(currentIndex);
    }

    // ------------------------------------------ BASE AND STADIUM CLAIMING ------------------------------------------
    /**
     * Lists all bases that have not yet been claimed.
     * @return int: the number of unclaimed bases
     * The base locations can be accessed in the public returnedLocations array.
     * The IDs (which are internal IDs used by the Communications class) can be accessed in the public returnedIds array.
     */
    public int listUnclaimedBases() {
        return listUnclaimedObjects(BASE_OFFSET);
    }

    /**
     * Lists all stadiums that have not yet been claimed.
     * @return int: the number of unclaimed stadiums
     * The base locations can be accessed in the public returnedLocations array
     * The IDs (which are internal IDs used by the Communications class) can be accessed in the public returnedIds array.
     */
    public int listUnclaimedStadiums() {
        return listUnclaimedObjects(STADIUM_OFFSET);
    }

    private int listUnclaimedObjects(int offset) {
        final int totalObjects = uc.read(offset);
        if (returnedLocations.length < totalObjects || returnedIds.length < totalObjects) {
            returnedLocations = new Location[totalObjects];
            returnedIds = new int[totalObjects];
        }

        int n = 0;
        for (int i = totalObjects - 1; i >= 0; --i) {
            if (readObjectProperty(offset, i, CLAIM_ID) == NO_CLAIM || readObjectProperty(offset, i, CLAIM_ROUND) + CLAIM_EXPIRATION < uc.getRound()) {
                returnedLocations[n] = new Location(readObjectProperty(offset, i, OBJECT_X), readObjectProperty(offset, i, OBJECT_Y));
                returnedIds[n++] = i;
            }
        }
        return n;
    }

    public void claimBase(int baseId) {
        uc.write(BASE_OFFSET + baseId * OBJECT_SIZE + CLAIM_ID, uc.getInfo().getID());
        updateClaimOnBase(baseId);
    }
    public void claimStadium(int stadiumId) {
        uc.write(STADIUM_OFFSET + stadiumId * OBJECT_SIZE + CLAIM_ID, uc.getInfo().getID());
        updateClaimOnStadium(stadiumId);
    }
    public void updateClaimOnBase(int baseId) {
        uc.write(BASE_OFFSET + baseId * OBJECT_SIZE + CLAIM_ROUND, uc.getRound());
    }
    public void updateClaimOnStadium(int stadiumId) {
        uc.write(STADIUM_OFFSET + stadiumId * OBJECT_SIZE + CLAIM_ROUND, uc.getRound());
    }

    private int readObjectProperty(int offset, int index, int property) {
        return uc.read(offset + OBJECT_SIZE * index + property);
    }

    // ------------------------------------------ ENEMY SIGHTINGS ------------------------------------------
    /**
     * Records a list of enemy sightings. Will try to combine each of these sightings with an existing sighting, if a nearby one exists.
     * Prioritizing combining first will ensure that we don't end up with too many items in the array.
     * If no nearby sighting exists, will try to replace an old sighting. Otherwise will add to end of the array.
     * For the sake of bytecode, this method doesn't always merge with a close sighting because that would require multiple passes for the merging step
     * @param enemies spotted enemies
     * @param urgencyFactor how urgently help is required (higher -> scarier)
     */
    public void reportEnemySightings(UnitInfo[] enemies, int urgencyFactor) {
//        if (uc.getRound() >= 1450) uc.println("start reportEnemySighting " + uc.getEnergyUsed());
        final int currentSightingCount = uc.read(ENEMY_SIGHTING_OFFSET);

        int idx = enemies.length - 1;
        if (idx < 0) return;
        for (int i = currentSightingCount - 1; i >= 0; --i) {
//            if (uc.getRound() >= 1450) uc.println(i + " " + uc.getEnergyUsed());
            while (enemies[idx].getLocation().distanceSquared(new Location(readSightingProperty(i, ENEMY_X), readSightingProperty(i, ENEMY_Y))) <= ENEMY_MERGE_DISTANCE) {
                final int oldUrgency = readSightingProperty(i, ENEMY_URGENCY);
                // sightings are close enough that we can merge them
                writeSightingProperty(i, ENEMY_URGENCY, oldUrgency + urgencyFactor * enemyUrgency(enemies[idx].getType()));
                --idx;
                while (idx >= 0 && (enemies[idx] == null || enemies[idx].getType() == UnitType.HQ)) --idx;
                if (idx < 0) return;
            }
        }
        for (int i = currentSightingCount - 1; i >= 0; --i) {
//            if (uc.getRound() >= 1450) uc.println(i + " " + uc.getEnergyUsed());
            if (readSightingProperty(i, ENEMY_URGENCY) <= 0) {  // sighting has decayed, we can replace it
                writeSightingProperty(i, ENEMY_X, enemies[idx].getLocation().x);
                writeSightingProperty(i, ENEMY_Y, enemies[idx].getLocation().y);
                writeSightingProperty(i, ENEMY_URGENCY, urgencyFactor * enemyUrgency(enemies[idx].getType()));
                --idx;
                while (idx >= 0 && (enemies[idx] == null || enemies[idx].getType() == UnitType.HQ)) --idx;
                if (idx < 0) return;
            }
        }

//        uc.println("adding sighting on round " + uc.getRound() + " at " + loc + ". urgencyFactor " + urgencyFactor);
        for (int i = idx; i >= 0; --i) {
            writeSightingProperty(currentSightingCount + i, ENEMY_X, enemies[idx].getLocation().x);
            writeSightingProperty(currentSightingCount + i, ENEMY_Y, enemies[idx].getLocation().y);
            writeSightingProperty(currentSightingCount + i, ENEMY_URGENCY, urgencyFactor * enemyUrgency(enemies[idx].getType()));
        }
        uc.write(ENEMY_SIGHTING_OFFSET, currentSightingCount + idx + 1);
    }
    public void reduceSightingUrgency(Location loc, int urgencyDecrease) {
        final int currentSightingCount = uc.read(ENEMY_SIGHTING_OFFSET);

        for (int i = currentSightingCount - 1; i >= 0; --i) {
            if (loc.x == readSightingProperty(i, ENEMY_X) && loc.y == readSightingProperty(i, ENEMY_Y)) {
                writeSightingProperty(i, ENEMY_URGENCY, readSightingProperty(i, ENEMY_URGENCY) - urgencyDecrease);
                return;
            }
        }
    }
    private int enemyUrgency(UnitType type) {
        if (type == UnitType.BATTER) return 10;
        if (type == UnitType.PITCHER) return 2;
        return 1;  // CATCHER
    }

    /**
     * Lists all enemy sighting locations and urgencies.
     * Urgencies take into account the distance between the unit and the enemy sighting
     * @return int: the number of enemy sightings
     * The enemy locations can be accessed in the public returnedLocations array.
     * The urgency of each sighting can be accessed in the public returnedUrgency array.
     * No IDs are returned. The returnedIds array is not modified and will likely contain stale data.
     */
    public int listEnemySightings() {
        final int totalEnemySightings = uc.read(ENEMY_SIGHTING_OFFSET);
        if (returnedLocations.length < totalEnemySightings || returnedUrgencies.length < totalEnemySightings) {
            returnedLocations = new Location[totalEnemySightings];
            returnedUrgencies = new int[totalEnemySightings];
        }
        int n = 0;
        for (int i = totalEnemySightings - 1; i >= 0; --i) {
            final Location loc = new Location(readSightingProperty(i, ENEMY_X), readSightingProperty(i, ENEMY_Y));
            final int calculatedUrgency = readSightingProperty(i, ENEMY_URGENCY) * 50 - uc.getLocation().distanceSquared(loc);
            if (calculatedUrgency > 0) {
                returnedLocations[n] = loc;
                returnedUrgencies[n] = calculatedUrgency;
                ++n;
            }
        }
        return n;
    }

    // called once per round by the HQ
    public void decayEnemySightingUrgencies() {
        int hi = uc.read(ENEMY_SIGHTING_OFFSET) - 1;
        for (int lo = 0; lo <= hi; ++lo) {
            while (lo <= hi && readSightingProperty(hi, ENEMY_URGENCY) <= 1) --hi;
            if (lo < hi && readSightingProperty(lo, ENEMY_URGENCY) <= 1) {
                writeSightingProperty(lo, ENEMY_X, readSightingProperty(hi, ENEMY_X));
                writeSightingProperty(lo, ENEMY_Y, readSightingProperty(hi, ENEMY_Y));
                writeSightingProperty(lo, ENEMY_URGENCY, readSightingProperty(hi, ENEMY_URGENCY) / 2);
                --hi;
            } else if (lo <= hi) {
                writeSightingProperty(lo, ENEMY_URGENCY, readSightingProperty(lo, ENEMY_URGENCY) / 2);
            }
        }
        uc.write(ENEMY_SIGHTING_OFFSET, hi + 1);
    }

    private int readSightingProperty(int index, int property) {
        return uc.read(ENEMY_SIGHTING_OFFSET + ENEMY_SIZE * index + property);
    }
    private void writeSightingProperty(int index, int property, int value) {
        uc.write(ENEMY_SIGHTING_OFFSET + ENEMY_SIZE * index + property, value);
    }

    // ------------------------------------------ GLOBAL MAP ------------------------------------------
    // we report grass instead of water to ensure we don't run into issues where water suddenly appears and blocks off a computed path
    // this way, we assume everything is blocked until we see otherwise
    // this must be called AFTER reportNewBases/Stadiums
    // ideally call this at the end of the turn
    public void reportNewGrassAtEndOfTurn(Location[] grass, int currentRound) {
//        uc.println("start grass " + uc.getEnergyUsed());
        final int mapCount = uc.read(MAP_OFFSET + DISTANCE_MAP_COUNT);
        for (int i = grass.length - 1; i >= 0 && uc.getRound() == currentRound; --i) {
            final int x = convertToInternalX(grass[i].x), y = convertToInternalY(grass[i].y);
            if (readMapLocation(0, x, y) < PASSABLE && !getSelfHQLocation().isEqual(grass[i])) {
                writeMapLocation(0, x, y, PASSABLE);
                if (x % 8 == 0 && y % 8 == 0) {
                    // don't need to update mapCount for the new maps since they haven't started processing yet
                    createDistanceMapIfNotExists(grass[i], 7);
                }

                boolean shouldAdd = false;
                for (int m = mapCount; m > 0; --m) {
                    if (readMapLocation(m, x, y) == INF) {
                        // INF signifies that location was previously processed back when we thought it wasn't passable
                        final int dist = Math.min(
                                Math.min(
                                        Math.min(
                                                infIfZero(readMapLocation(m, x - 1, y - 1)),
                                                infIfZero(readMapLocation(m, x - 1, y + 1))
                                        ),
                                        Math.min(
                                                infIfZero(readMapLocation(m, x + 1, y - 1)),
                                                infIfZero(readMapLocation(m, x + 1, y + 1))
                                        )
                                ) + DISTANCE_ROOT,
                                Math.min(
                                        Math.min(
                                                infIfZero(readMapLocation(m, x - 1, y)),
                                                infIfZero(readMapLocation(m, x, y - 1))
                                        ),
                                        Math.min(
                                                infIfZero(readMapLocation(m, x, y + 1)),
                                                infIfZero(readMapLocation(m, x + 1, y))
                                        )
                                ) + DISTANCE_UNIT
                        );
                        if (dist < INF) {
                            writeMapLocation(m, x, y, dist);
                            addToDistanceQueue(packMapIndexAndLocation(m, x, y));
                            shouldAdd = true;
                        }
                    }
                }
                if (shouldAdd && (x + x + y) % 7 == 0) {  // (x + x + y) % 7 generates a spread out pattern
                    addToScoutingQueue(x, y);
                }
            }
        }

        final int x = convertToInternalX(uc.getLocation().x), y = convertToInternalY(uc.getLocation().y);
        if (readMapLocation(0, x, y) == PASSABLE) writeMapLocation(0, x, y, SENSED);  // we need to include the check to make sure the HQ doesn't get marked as SENSED
        if (uc.getType() == UnitType.CATCHER || uc.getType() == UnitType.HQ) {
            if (readMapLocation(0, x - 1, y) == PASSABLE) writeMapLocation(0, x - 1, y, SENSED);
            if (readMapLocation(0, x, y - 1) == PASSABLE) writeMapLocation(0, x, y - 1, SENSED);
            if (readMapLocation(0, x, y + 1) == PASSABLE) writeMapLocation(0, x, y + 1, SENSED);
            if (readMapLocation(0, x + 1, y) == PASSABLE) writeMapLocation(0, x + 1, y, SENSED);
        }
//        uc.println("end grass " + uc.getEnergyUsed());
    }
    public Location getSelfHQLocation() {
        return new Location(uc.read(MAP_OFFSET + ORIGIN_X), uc.read(MAP_OFFSET + ORIGIN_Y));
    }
    public boolean grassAlreadySensedAtLocation() {
        if (uc.getType() == UnitType.CATCHER) {
            final int x = convertToInternalX(uc.getLocation().x), y = convertToInternalY(uc.getLocation().y);
            return (readMapLocation(0, x - 1, y) == UNINITIALIZED || readMapLocation(0, x - 1, y) == SENSED) &&
                    (readMapLocation(0, x, y - 1) == UNINITIALIZED || readMapLocation(0, x, y - 1) == SENSED) &&
                    (readMapLocation(0, x, y + 1) == UNINITIALIZED || readMapLocation(0, x, y + 1) == SENSED) &&
                    (readMapLocation(0, x + 1, y) == UNINITIALIZED || readMapLocation(0, x + 1, y) == SENSED);  // don't need to sense middle location, others cover it
        } else {
            return readMapLocation(0, convertToInternalX(uc.getLocation().x), convertToInternalY(uc.getLocation().y)) == SENSED;
        }
    }
    public boolean isPassable(Location externalLoc) {
        return readMapLocation(0, convertToInternalX(externalLoc.x), convertToInternalY(externalLoc.y)) != UNINITIALIZED;
    }

    // for all maps, both global and distance
    private int readMapLocation(int mapIdx, int internalX, int internalY) {
        return uc.read(MAP_OFFSET + MAP_SIZE * mapIdx + MAP_DIMENSION * internalX + internalY);
    }
    private void writeMapLocation(int mapIdx, int internalX, int internalY, int value) {
        uc.write(MAP_OFFSET + MAP_SIZE * mapIdx + MAP_DIMENSION * internalX + internalY, value);
    }
    private int convertToInternalX(int originalX) {
        return originalX - uc.read(MAP_OFFSET + ORIGIN_X) + 60;
    }
    private int convertToInternalY(int originalY) {
        return originalY - uc.read(MAP_OFFSET + ORIGIN_Y) + 60;
    }
    private int convertToExternalX(int originalX) {
        return originalX + uc.read(MAP_OFFSET + ORIGIN_X) - 60;
    }
    private int convertToExternalY(int originalY) {
        return originalY + uc.read(MAP_OFFSET + ORIGIN_Y) - 60;
    }

    // ------------------------------------------ DISTANCE QUEUE ------------------------------------------

    /**
     * Runs a distributed BFS, with one queue representing all the distance map queues
     * This BFS is rather inefficient because there are two different edge weights
     * This should be the last method called before yielding
     */
    public void useRemainingBytecode(int currentRound) {
        // as long as we have enough bytecode to get through the queueStart read/write, it's okay
        while (uc.getRound() == currentRound && uc.getEnergyLeft() >= 50) {
            // move queue pointer writes/reads to be as close together as possible
            final int queueStart = uc.read(DISTANCE_QUEUE_OFFSET + DISTANCE_QUEUE_START);
//            uc.println("queue: " + queueStart + " " + uc.read(DISTANCE_QUEUE_OFFSET + DISTANCE_QUEUE_END));
            if (queueStart == uc.read(DISTANCE_QUEUE_OFFSET + DISTANCE_QUEUE_END)) return;
            uc.write(DISTANCE_QUEUE_OFFSET + DISTANCE_QUEUE_START, (queueStart + 1) % DISTANCE_QUEUE_SIZE);

            final int cur = uc.read(DISTANCE_QUEUE_OFFSET + queueStart);
            if (cur == 0) uc.println("This is bad! cur = 0, queueStart = " + queueStart);
            final int mapIdx = (cur / MAP_DIMENSION) / MAP_DIMENSION, x = (cur / MAP_DIMENSION) % MAP_DIMENSION, y = cur % MAP_DIMENSION;
            final int dist = readMapLocation(mapIdx, x, y);
//            uc.println(mapIdx + " " + x + " " + y);

            checkLocation(mapIdx, x - 1, y, dist, DISTANCE_UNIT);
            checkLocation(mapIdx, x, y - 1, dist, DISTANCE_UNIT);
            checkLocation(mapIdx, x, y + 1, dist, DISTANCE_UNIT);
            checkLocation(mapIdx, x + 1, y, dist, DISTANCE_UNIT);
            checkLocation(mapIdx, x - 1, y - 1, dist, DISTANCE_ROOT);
            checkLocation(mapIdx, x - 1, y + 1, dist, DISTANCE_ROOT);
            checkLocation(mapIdx, x + 1, y - 1, dist, DISTANCE_ROOT);
            checkLocation(mapIdx, x + 1, y + 1, dist, DISTANCE_ROOT);
        }
    }

    /**
     * @param mapIdx     current map
     * @param newX       x value of coordinate to check
     * @param newY       y value of coordinate to check
     * @param dist       current distance
     * @param distChange amount that dist will be incremented for newX and newY
     */
    private void checkLocation(int mapIdx, int newX, int newY, int dist, int distChange) {
        if (readMapLocation(0, newX, newY) != UNINITIALIZED) {
            if (infIfZero(readMapLocation(mapIdx, newX, newY)) > dist + distChange) {
                writeMapLocation(mapIdx, newX, newY, dist + distChange);
                addToDistanceQueue(packMapIndexAndLocation(mapIdx, newX, newY));
            }
        } else {
            writeMapLocation(mapIdx, newX, newY, INF); // signify location has been processed but isn't (yet) known to be passable
        }
    }

    private int packMapIndexAndLocation(int mapIdx, int x, int y) {
        return (mapIdx * MAP_DIMENSION + x) * MAP_DIMENSION + y;
    }

    // adds to queue, trying to ensure that queue doesn't get messed up on bytecode overflows
    // some benchmarking suggests that one invocation of this method costs 30 bytecode + any wasted on the first line
    // removing the first line would save 5 bytecode for a normal invocation
    public void addToDistanceQueue(int value) {
        while (uc.getEnergyLeft() <= 31);  // wait until we run out of bytecode and next turn starts
        final int queueEnd = uc.read(DISTANCE_QUEUE_OFFSET + DISTANCE_QUEUE_END);
        uc.write(DISTANCE_QUEUE_OFFSET + queueEnd, value);
        uc.write(DISTANCE_QUEUE_OFFSET + DISTANCE_QUEUE_END, (queueEnd + 1) % DISTANCE_QUEUE_SIZE);
    }

    // ------------------------------------------ DISTANCE MAPS ------------------------------------------

    private int findBestDistanceMapIdx(int internalCurrentX, int internalCurrentY, int internalTargetX, int internalTargetY) {
        final int n = uc.read(MAP_OFFSET + DISTANCE_MAP_COUNT);
        int bestIdx = -1;
        int minDist = INF;
        for (int i = n; i > 0; --i) {
            final int computedDist = infIfZero(readMapLocation(i, internalCurrentX, internalCurrentY)) / 10 + infIfZero(readMapLocation(i, internalTargetX, internalTargetY)) * 2;
            // prioritize going to focal point. INF / 10 + INF * 2 should not overflow
            if (minDist > computedDist) {
                minDist = computedDist;
                bestIdx = i;
            }
        }
        return bestIdx;
    }
    public int findBestDistanceMapIdx(Location currentLoc, Location targetLoc) {
        return findBestDistanceMapIdx(convertToInternalX(currentLoc.x), convertToInternalY(currentLoc.y), convertToInternalX(targetLoc.x), convertToInternalY(targetLoc.y));
    }
    public int distanceFromFocalPoint(int focalPointIdx, Location externalLoc) {
        return infIfZero(readMapLocation(focalPointIdx, convertToInternalX(externalLoc.x), convertToInternalY(externalLoc.y)));
    }

    private void createDistanceMapIfNotExists(Location externalLoc, int buffer) {
        final int internalX = convertToInternalX(externalLoc.x), internalY = convertToInternalY(externalLoc.y);
        final int n = uc.read(MAP_OFFSET + DISTANCE_MAP_COUNT);
        if (n >= MAX_MAP_COUNT) return;
        for (int i = n; i > 0; --i) {
            if (Math.abs(uc.read(MAP_OFFSET + MAP_SIZE * i + ORIGIN_X) - internalX) <= buffer &&
                    Math.abs(uc.read(MAP_OFFSET + MAP_SIZE * i + ORIGIN_Y) - internalY) <= buffer) {
                return;
            }
        }

        uc.write(MAP_OFFSET + DISTANCE_MAP_COUNT, n + 1);
        uc.write(MAP_OFFSET + MAP_SIZE * (n + 1) + ORIGIN_X, internalX);
        uc.write(MAP_OFFSET + MAP_SIZE * (n + 1) + ORIGIN_Y, internalY);

        writeMapLocation(n + 1, internalX, internalY, INITIAL_DISTANCE);
        addToDistanceQueue(packMapIndexAndLocation(n + 1, internalX, internalY));
    }

    /**
     * Recommends a direction to move in, in order to reach a focal point close to a given location
     * Best used when location given is a focal point
     * Returns null if a recommendation can't be given (likely because the area hasn't been processed)
     * Returns Direction.ZERO if already at the given location
     * @param externalTargetLoc location to reach
     * @return a recommended direction or null if no recommendation can be made
     */
    public Direction directionViaFocalPoint(Location externalTargetLoc, int directionOkay) {
        final int curX = convertToInternalX(uc.getLocation().x), curY = convertToInternalY(uc.getLocation().y);
        final int bestIdx = findBestDistanceMapIdx(curX, curY, convertToInternalX(externalTargetLoc.x), convertToInternalY(externalTargetLoc.y));
        if (bestIdx == -1) return null;
//        uc.println("fp: " + bestIdx + " " + convertToExternalX(uc.read(MAP_OFFSET + bestIdx * MAP_SIZE + ORIGIN_X)) + " " + convertToExternalY(uc.read(MAP_OFFSET + bestIdx * MAP_SIZE + ORIGIN_Y)));

        final int currentDist = readMapLocation(bestIdx, curX, curY);
//        uc.println("going to " + externalTargetLoc + ", dist " + currentDist);
//        uc.println(readMapLocation(bestIdx, curX - 1, curY + 1) + " " + readMapLocation(bestIdx, curX, curY + 1) + " " + readMapLocation(bestIdx, curX + 1, curY + 1));
//        uc.println(readMapLocation(bestIdx, curX - 1, curY) + "         " + readMapLocation(bestIdx, curX + 1, curY));
//        uc.println(readMapLocation(bestIdx, curX - 1, curY - 1) + " " + readMapLocation(bestIdx, curX, curY - 1) + " " + readMapLocation(bestIdx, curX + 1, curY - 1));
        if (currentDist == 0) return null;
        if (currentDist == INITIAL_DISTANCE) return Direction.ZERO;

        Direction best = Direction.ZERO;
        int bestDist = INF;
        if ((directionOkay & 1) > 0 && uc.canMove(Direction.NORTH) && bestDist > infIfZero(readMapLocation(bestIdx, curX + Direction.NORTH.dx, curY + Direction.NORTH.dy)) + DISTANCE_UNIT) { best = Direction.NORTH; bestDist = readMapLocation(bestIdx, curX + Direction.NORTH.dx, curY + Direction.NORTH.dy) + DISTANCE_UNIT; }
        if (((directionOkay >> 6) & 1) > 0 && uc.canMove(Direction.EAST ) && bestDist > infIfZero(readMapLocation(bestIdx, curX + Direction.EAST.dx , curY + Direction.EAST.dy )) + DISTANCE_UNIT) { best = Direction.EAST ; bestDist = readMapLocation(bestIdx, curX + Direction.EAST.dx , curY + Direction.EAST.dy ) + DISTANCE_UNIT; }
        if (((directionOkay >> 4) & 1) > 0 && uc.canMove(Direction.SOUTH) && bestDist > infIfZero(readMapLocation(bestIdx, curX + Direction.SOUTH.dx, curY + Direction.SOUTH.dy)) + DISTANCE_UNIT) { best = Direction.SOUTH; bestDist = readMapLocation(bestIdx, curX + Direction.SOUTH.dx, curY + Direction.SOUTH.dy) + DISTANCE_UNIT; }
        if (((directionOkay >> 2) & 1) > 0 && uc.canMove(Direction.WEST ) && bestDist > infIfZero(readMapLocation(bestIdx, curX + Direction.WEST.dx , curY + Direction.WEST.dy )) + DISTANCE_UNIT) { best = Direction.WEST ; bestDist = readMapLocation(bestIdx, curX + Direction.WEST.dx , curY + Direction.WEST.dy ) + DISTANCE_UNIT; }
        if (((directionOkay >> 1) & 1) > 0 && uc.canMove(Direction.NORTHWEST) && bestDist > infIfZero(readMapLocation(bestIdx, curX + Direction.NORTHWEST.dx, curY + Direction.NORTHWEST.dy) + DISTANCE_ROOT)) { best = Direction.NORTHWEST; bestDist = readMapLocation(bestIdx, curX + Direction.NORTHWEST.dx, curY + Direction.NORTHWEST.dy) + DISTANCE_ROOT; }
        if (((directionOkay >> 7) & 1) > 0 && uc.canMove(Direction.NORTHEAST) && bestDist > infIfZero(readMapLocation(bestIdx, curX + Direction.NORTHEAST.dx, curY + Direction.NORTHEAST.dy) + DISTANCE_ROOT)) { best = Direction.NORTHEAST; bestDist = readMapLocation(bestIdx, curX + Direction.NORTHEAST.dx, curY + Direction.NORTHEAST.dy) + DISTANCE_ROOT; }
        if (((directionOkay >> 3) & 1) > 0 && uc.canMove(Direction.SOUTHWEST) && bestDist > infIfZero(readMapLocation(bestIdx, curX + Direction.SOUTHWEST.dx, curY + Direction.SOUTHWEST.dy) + DISTANCE_ROOT)) { best = Direction.SOUTHWEST; bestDist = readMapLocation(bestIdx, curX + Direction.SOUTHWEST.dx, curY + Direction.SOUTHWEST.dy) + DISTANCE_ROOT; }
        if (((directionOkay >> 5) & 1) > 0 && uc.canMove(Direction.SOUTHEAST) && bestDist > infIfZero(readMapLocation(bestIdx, curX + Direction.SOUTHEAST.dx, curY + Direction.SOUTHEAST.dy) + DISTANCE_ROOT)) { best = Direction.SOUTHEAST; bestDist = readMapLocation(bestIdx, curX + Direction.SOUTHEAST.dx, curY + Direction.SOUTHEAST.dy) + DISTANCE_ROOT; }

        // if we can only move further away, then just don't move
        // if the direction ordinal is even, it's N/E/S/W, otherwise it's diagonal
        // best == ZERO is an edge case where the above isn't true, but then both possible return values are ZERO, so it's fine
        return bestDist - (best.ordinal() % 2 == 0 ? DISTANCE_UNIT : DISTANCE_ROOT) < currentDist ? best : Direction.ZERO;
    }
    public int directionsFromFocalPoint(Location externalTargetLoc) {
        int curX = convertToInternalX(uc.getLocation().x), curY = convertToInternalY(uc.getLocation().y);
        final int bestIdx = findBestDistanceMapIdx(convertToInternalX(externalTargetLoc.x), convertToInternalY(externalTargetLoc.y), curX, curY);
        int dist = infIfZero(readMapLocation(bestIdx, curX, curY));
        int n = 0;
        while (dist > INITIAL_DISTANCE) {
            if (dist - DISTANCE_ROOT == readMapLocation(bestIdx, curX + 1, curY + 1)) {  // NORTHEAST
                dist -= DISTANCE_ROOT;
                ++curX; ++curY;
                returnedDirections[n++] = Direction.SOUTHWEST;
                continue;
            }
            if (dist - DISTANCE_ROOT == readMapLocation(bestIdx, curX + 1, curY - 1)) {  // SOUTHEAST
                dist -= DISTANCE_ROOT;
                ++curX; --curY;
                returnedDirections[n++] = Direction.NORTHWEST;
                continue;
            }
            if (dist - DISTANCE_ROOT == readMapLocation(bestIdx, curX - 1, curY - 1)) {  // SOUTHWEST
                dist -= DISTANCE_ROOT;
                --curX; --curY;
                returnedDirections[n++] = Direction.NORTHEAST;
                continue;
            }
            if (dist - DISTANCE_ROOT == readMapLocation(bestIdx, curX - 1, curY + 1)) {  // NORTHWEST
                dist -= DISTANCE_ROOT;
                --curX; ++curY;
                returnedDirections[n++] = Direction.SOUTHEAST;
                continue;
            }

            if (dist - DISTANCE_UNIT == readMapLocation(bestIdx, curX, curY + 1)) {  // NORTH
                dist -= DISTANCE_UNIT;
                ++curY;
                returnedDirections[n++] = Direction.SOUTH;
                continue;
            }
            if (dist - DISTANCE_UNIT == readMapLocation(bestIdx, curX + 1, curY)) {  // EAST
                dist -= DISTANCE_UNIT;
                ++curX;
                returnedDirections[n++] = Direction.WEST;
                continue;
            }
            if (dist - DISTANCE_UNIT == readMapLocation(bestIdx, curX, curY - 1)) {  // SOUTH
                dist -= DISTANCE_UNIT;
                --curY;
                returnedDirections[n++] = Direction.NORTH;
                continue;
            }
            if (dist - DISTANCE_UNIT == readMapLocation(bestIdx, curX - 1, curY)) {  // WEST
                dist -= DISTANCE_UNIT;
                --curX;
                returnedDirections[n++] = Direction.EAST;
                continue;
            }
            return -1;
        }
        return n;
    }

    public int lowerBoundDistance(Location loc1, Location loc2) {
        final int curX = convertToInternalX(loc1.x), curY = convertToInternalY(loc1.y);
        final int internalX = convertToInternalX(loc2.x), internalY = convertToInternalY(loc2.y);
        final int n = uc.read(MAP_OFFSET + DISTANCE_MAP_COUNT);
        int lbDist = (int)(Math.sqrt(uc.getLocation().distanceSquared(loc2)) * DISTANCE_UNIT);
        for (int i = n; i > 0; --i) {
            final int dist1 = readMapLocation(i, curX, curY);
            final int dist2 = readMapLocation(i, internalX, internalY);
            if (dist1 != 0 && dist1 != INF && dist2 != 0 && dist2 != INF) {
                lbDist = Math.max(lbDist, Math.abs(dist1 - dist2));
            }
        }
        return lbDist;
    }
    public int lowerBoundDistance(Location loc) {
        return lowerBoundDistance(uc.getLocation(), loc);
    }

    public boolean lowerBoundDistanceGreaterThan(Location loc1, Location loc2, int threshold) {
        if ((int)(Math.sqrt(uc.getLocation().distanceSquared(loc2)) * DISTANCE_UNIT) > threshold) return true;
        final int curX = convertToInternalX(loc1.x), curY = convertToInternalY(loc1.y);
        final int internalX = convertToInternalX(loc2.x), internalY = convertToInternalY(loc2.y);
        final int n = uc.read(MAP_OFFSET + DISTANCE_MAP_COUNT);
        for (int i = n; i > 0; --i) {
            final int dist1 = readMapLocation(i, curX, curY);
            final int dist2 = readMapLocation(i, internalX, internalY);
            if (dist1 != 0 && dist1 != INF && dist2 != 0 && dist2 != INF && Math.abs(dist1 - dist2) > threshold) {
                return true;
            }
        }
        return false;
    }

    private int infIfZero(int x) {
        return x == 0 ? INF : x;
    }

    // ------------------------------------------ SCOUTING QUEUE ------------------------------------------

    private void addToScoutingQueue(int x, int y) {
        while (uc.getEnergyLeft() <= 41);  // wait until we run out of bytecode and next turn starts
        final int queueEnd = uc.read(SCOUTING_QUEUE_OFFSET + SCOUTING_QUEUE_END);
        uc.write(SCOUTING_QUEUE_OFFSET + queueEnd, x * MAP_DIMENSION + y);
        uc.write(SCOUTING_QUEUE_OFFSET + SCOUTING_QUEUE_END, (queueEnd + 1) % SCOUTING_QUEUE_SIZE);
    }
    public Location popNearestScoutingQueue() {
        final int n = Math.max(1, uc.read(MAP_OFFSET + DISTANCE_MAP_COUNT));

        int queueStart = uc.read(SCOUTING_QUEUE_OFFSET + SCOUTING_QUEUE_START);
        final int queueEnd = Math.min(uc.read(SCOUTING_QUEUE_OFFSET + SCOUTING_QUEUE_END), queueStart + 400 / n);
        int bestDist = INF;
        int bestIdx = -1;
        Location bestLoc = null;
        for (int i = queueStart; i < queueEnd; ++i) {
            final int val = uc.read(SCOUTING_QUEUE_OFFSET + i);
            if (val == -1) {
                if (queueStart == i) {
                    ++queueStart;
                    uc.write(SCOUTING_QUEUE_OFFSET + SCOUTING_QUEUE_START, queueStart);
                }
                continue;
            }

            final int x = val / MAP_DIMENSION, y = val % MAP_DIMENSION;
            if (readMapLocation(0, x - 1, y - 1) == SENSED ||
                    readMapLocation(0, x - 1, y) == SENSED ||
                    readMapLocation(0, x - 1, y + 1) == SENSED ||
                    readMapLocation(0, x, y - 1) == SENSED ||
                    readMapLocation(0, x, y) == SENSED ||
                    readMapLocation(0, x, y + 1) == SENSED ||
                    readMapLocation(0, x + 1, y - 1) == SENSED ||
                    readMapLocation(0, x + 1, y) == SENSED ||
                    readMapLocation(0, x + 1, y + 1) == SENSED) {
                if (queueStart == i) {
                    ++queueStart;
                    uc.write(SCOUTING_QUEUE_OFFSET + SCOUTING_QUEUE_START, queueStart);
                }
                uc.write(SCOUTING_QUEUE_OFFSET + i, -1);
                continue;
            }

            Location loc = new Location(convertToExternalX(x), convertToExternalY(y));
            if (uc.getLocation().distanceSquared(loc) <= 9) {
                if (queueStart == i) {
                    ++queueStart;
                    uc.write(SCOUTING_QUEUE_OFFSET + SCOUTING_QUEUE_START, queueStart);
                }
                uc.write(SCOUTING_QUEUE_OFFSET + i, -1);
                continue;
            }
            if (!lowerBoundDistanceGreaterThan(uc.getLocation(), loc, bestDist + (int)(Util.movementDistance(getSelfHQLocation(), loc)))) {
                bestDist = lowerBoundDistance(loc) - (int)(Util.movementDistance(getSelfHQLocation(), loc));  // tiebreaks since lowerBoundDistance is in DISTANCE_UNITs
                bestIdx = i;
                bestLoc = loc;
            }
        }

        if (bestIdx != -1) uc.write(SCOUTING_QUEUE_OFFSET + bestIdx, -1);
        return bestLoc;
    }

    public void updateScoutingQueue() {
        int queueStart = uc.read(SCOUTING_QUEUE_OFFSET + SCOUTING_QUEUE_START);
        final int queueEnd = Math.min(uc.read(SCOUTING_QUEUE_OFFSET + SCOUTING_QUEUE_END), queueStart + 50);
        for (int i = queueStart; i < queueEnd; ++i) {
            final int val = uc.read(SCOUTING_QUEUE_OFFSET + i);
            if (val == -1) {
                if (queueStart == i) {
                    ++queueStart;
                    uc.write(SCOUTING_QUEUE_OFFSET + SCOUTING_QUEUE_START, queueStart);
                }
                continue;
            }

            final int x = val / MAP_DIMENSION, y = val % MAP_DIMENSION;
            if (readMapLocation(0, x - 1, y - 1) == SENSED ||
                    readMapLocation(0, x - 1, y) == SENSED ||
                    readMapLocation(0, x - 1, y + 1) == SENSED ||
                    readMapLocation(0, x, y - 1) == SENSED ||
                    readMapLocation(0, x, y) == SENSED ||
                    readMapLocation(0, x, y + 1) == SENSED ||
                    readMapLocation(0, x + 1, y - 1) == SENSED ||
                    readMapLocation(0, x + 1, y) == SENSED ||
                    readMapLocation(0, x + 1, y + 1) == SENSED) {
                if (queueStart == i) {
                    ++queueStart;
                    uc.write(SCOUTING_QUEUE_OFFSET + SCOUTING_QUEUE_START, queueStart);
                }
                uc.write(SCOUTING_QUEUE_OFFSET + i, -1);
                continue;
            }

            Location loc = new Location(convertToExternalX(x), convertToExternalY(y));
            if (uc.getLocation().distanceSquared(loc) <= 9) {
                if (queueStart == i) {
                    ++queueStart;
                    uc.write(SCOUTING_QUEUE_OFFSET + SCOUTING_QUEUE_START, queueStart);
                }
                uc.write(SCOUTING_QUEUE_OFFSET + i, -1);
            }
        }
    }
}