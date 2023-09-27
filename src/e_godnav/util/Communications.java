package e_godnav.util;

import aic2023.user.*;

public class Communications {
    private final UnitController uc;

    // TODO: for final code submissions, if we're tight on bytecode, we can inline the constants

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

    private final int MAX_OBJECT_COUNT = 4000; // > 60^2
    private final int BASE_OFFSET = 0;
    private final int STADIUM_OFFSET = MAX_OBJECT_COUNT * OBJECT_SIZE;

    private final int CHECK_IN_OFFSET = 50000;
    private final int BATTER_NUMBER = 0;
    private final int CATCHER_NUMBER = 1;
    private final int PITCHER_NUMBER = 2;
    private final int HQ_NUMBER = 3;

    private final int ENEMY_SIGHTING_OFFSET = 60000;
    private final int ENEMY_SIZE = 3;
    private final int ENEMY_X = 1;
    private final int ENEMY_Y = 2;
    private final int ENEMY_URGENCY = 3;
    private final int ENEMY_MERGE_DISTANCE = 40;

    private final int QUEUE_OFFSET = 100000;
    private final int QUEUE_SIZE = 100000 - 3;
    private final int QUEUE_START = QUEUE_SIZE;
    private final int QUEUE_END = QUEUE_SIZE + 1;
    private final int QUEUE_ITERATION_BYTECODE = 800;

    private final int MAP_OFFSET = 200000;
    private final int MAP_DIMENSION = 123;
    // assume our HQ is at (60, 60). This guess is off by at most 60 in either direction, se we need 120 to store the map
    private final int MAP_SIZE = 125 * 125;
    private final int ORIGIN_X = MAP_SIZE - 3, ORIGIN_Y = MAP_SIZE - 2;  // take advantage of extra buffer space
    private final int PASSABLE = 1;
    // the first map holds which locations are passable (0 = impassable, 1 = passable)
    // all locations start off as impassable
    // the other maps hold distance to some origin
    private final int DISTANCE_MAP_COUNT = MAP_SIZE - 1;

    private final int DISTANCE_UNIT = 10000;
    private final int DISTANCE_ROOT = 14142;
    private final int INITIAL_DISTANCE = 1;  // 0 represents INF, since array is 0-initialized

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
            createDistanceMapIfNotExists(uc.getLocation());
        }
    }

    // ------------------------------------------ BASE AND STADIUM LOCATIONS ------------------------------------------
    public void reportNewBases(Location[] bases) {
        reportNewObjects(bases, BASE_OFFSET);
    }

    public void reportNewStadiums(Location[] stadiums) {
        reportNewObjects(stadiums, STADIUM_OFFSET);
    }

    private void reportNewObjects(Location[] locs, int offset) {
        final int n = uc.read(offset);
        int m = 0;
        for (int locIdx = locs.length - 1; locIdx >= 0; --locIdx) {
            boolean alreadyExists = false;
            for (int i = n - 1; i >= 0 && !alreadyExists; --i) {
                alreadyExists = uc.read(offset + OBJECT_SIZE * i + OBJECT_X) == locs[locIdx].x && uc.read(offset + OBJECT_SIZE * i + OBJECT_Y) == locs[locIdx].y;
            }
            if (!alreadyExists) {
                uc.write(offset + OBJECT_SIZE * (n + m) + OBJECT_X, locs[locIdx].x);
                uc.write(offset + OBJECT_SIZE * (n + m) + OBJECT_Y, locs[locIdx].y);
                uc.write(offset + OBJECT_SIZE * (n + m) + CLAIM_ID, NO_CLAIM);
                uc.write(offset + OBJECT_SIZE * (n + m) + CLAIM_ROUND, NO_CLAIM);
                ++m;

                createDistanceMapIfNotExists(locs[locIdx]);
            }
        }
        uc.write(offset, n + m);
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
    private int enemyUrgency(UnitType type) {
        if (type == UnitType.BATTER) return 5;
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
    public void reportNewGrass(Location[] grass) {
        for (int i = grass.length - 1; i >= 0; --i) {
            Location internalLoc = convertToInternalCoordinates(grass[i]);
            writeMapLocation(0, internalLoc.x, internalLoc.y, 1);
            if (internalLoc.x % 10 == 0 && internalLoc.y % 10 == 0) {
                createDistanceMapIfNotExists(grass[i]);
            }
        }
    }
    public boolean isPassable(Location externalLoc) {
        final Location internalLoc = convertToInternalCoordinates(externalLoc);
        return readMapLocation(0, internalLoc.x, internalLoc.y) == 1;
    }

    // for all maps, both global and distance
    private int readMapLocation(int mapIdx, int internalX, int internalY) {
        return uc.read(MAP_OFFSET + MAP_SIZE * mapIdx + MAP_DIMENSION * internalX + internalY);
    }
    private void writeMapLocation(int mapIdx, int internalX, int internalY, int value) {
        uc.write(MAP_OFFSET + MAP_SIZE * mapIdx + MAP_DIMENSION * internalX + internalY, value);
    }
    private Location convertToInternalCoordinates(Location original) {
        return new Location(original.x - uc.read(MAP_OFFSET + ORIGIN_X) + 60, original.y - uc.read(MAP_OFFSET + ORIGIN_Y) + 60);
    }

    // ------------------------------------------ DISTANCE QUEUE ------------------------------------------

    /**
     * Runs a distributed 0-1 BFS, with one queue representing all the distance map queues
     * This should be the last method called before yielding
     * TODO: consider just running over the bytecode limit and then don't yield
     */
    public void useRemainingBytecode() {
        if (uc.getEnergyLeft() < QUEUE_ITERATION_BYTECODE) return;
        int queueStart = uc.read(QUEUE_OFFSET + QUEUE_START);
        int queueEnd = uc.read(QUEUE_OFFSET + QUEUE_END);
        while (uc.getEnergyLeft() >= QUEUE_ITERATION_BYTECODE && queueStart != queueEnd) {
            final int cur = uc.read(QUEUE_OFFSET + queueStart);

            queueStart = (queueStart + 1) % QUEUE_SIZE;
            final int mapIdx = (cur / MAP_DIMENSION) / MAP_DIMENSION, x = (cur / MAP_DIMENSION) % MAP_DIMENSION, y = cur % MAP_DIMENSION;
            final int dist = readMapLocation(mapIdx, x, y);

            if (checkLocation(queueStart - 1, mapIdx, x - 1, y - 1, dist, DISTANCE_ROOT)) queueStart = (queueStart + QUEUE_SIZE - 1) % QUEUE_SIZE;
            if (checkLocation(queueStart - 1, mapIdx, x - 1, y + 1, dist, DISTANCE_ROOT)) queueStart = (queueStart + QUEUE_SIZE - 1) % QUEUE_SIZE;
            if (checkLocation(queueStart - 1, mapIdx, x + 1, y - 1, dist, DISTANCE_ROOT)) queueStart = (queueStart + QUEUE_SIZE - 1) % QUEUE_SIZE;
            if (checkLocation(queueStart - 1, mapIdx, x + 1, y + 1, dist, DISTANCE_ROOT)) queueStart = (queueStart + QUEUE_SIZE - 1) % QUEUE_SIZE;
            if (checkLocation(queueEnd, mapIdx, x - 1, y, dist, DISTANCE_UNIT)) queueEnd = (queueEnd + 1) % QUEUE_SIZE;
            if (checkLocation(queueEnd, mapIdx, x, y - 1, dist, DISTANCE_UNIT)) queueEnd = (queueEnd + 1) % QUEUE_SIZE;
            if (checkLocation(queueEnd, mapIdx, x, y + 1, dist, DISTANCE_UNIT)) queueEnd = (queueEnd + 1) % QUEUE_SIZE;
            if (checkLocation(queueEnd, mapIdx, x + 1, y, dist, DISTANCE_UNIT)) queueEnd = (queueEnd + 1) % QUEUE_SIZE;
        }
        uc.write(QUEUE_OFFSET + QUEUE_START, queueStart);
        uc.write(QUEUE_OFFSET + QUEUE_END, queueEnd);
    }

    private boolean checkLocation(int queueIdx, int mapIdx, int newX, int newY, int dist, int distChange) {
        if (readMapLocation(0, newX, newY) == PASSABLE && infIfZero(readMapLocation(mapIdx, newX, newY)) > dist + distChange) {
            writeMapLocation(mapIdx, newX, newY, dist + distChange);
            uc.write(QUEUE_OFFSET + queueIdx, (mapIdx * MAP_DIMENSION + newX) * MAP_DIMENSION + newY);
            return true;
        }
        return false;
    }

    // ------------------------------------------ DISTANCE MAPS ------------------------------------------

    private int findBestDistanceMapIdx(Location internalCurrentLoc, Location internalTargetLoc) {
        final int n = uc.read(MAP_OFFSET + DISTANCE_MAP_COUNT);
        int bestIdx = -1;
        int minDist = 1_000_000_000;
        for (int i = n; i > 0; --i) {
            final int computedDist = infIfZero(readMapLocation(i, internalCurrentLoc.x, internalCurrentLoc.y)) + infIfZero(readMapLocation(i, internalTargetLoc.x, internalTargetLoc.y));
            if (minDist > computedDist) {
                minDist = computedDist;
                bestIdx = i;
            }
        }
        return bestIdx;
    }
    private void createDistanceMapIfNotExists(Location externalLoc) {
        final Location internalLoc = convertToInternalCoordinates(externalLoc);
        final int n = uc.read(MAP_OFFSET + DISTANCE_MAP_COUNT);
        for (int i = n; i > 0; --i) {
            if (uc.read(MAP_OFFSET + MAP_SIZE * i + ORIGIN_X) == internalLoc.x && uc.read(MAP_OFFSET + MAP_SIZE * i + ORIGIN_Y) == internalLoc.y) return;
        }

        uc.write(MAP_OFFSET + DISTANCE_MAP_COUNT, n + 1);
        uc.write(MAP_OFFSET + MAP_SIZE * (n + 1) + ORIGIN_X, internalLoc.x);
        uc.write(MAP_OFFSET + MAP_SIZE * (n + 1) + ORIGIN_Y, internalLoc.y);

        writeMapLocation(n, internalLoc.x, internalLoc.y, INITIAL_DISTANCE);

        final int queueEnd = uc.read(QUEUE_OFFSET + QUEUE_END);
        uc.write(QUEUE_OFFSET + queueEnd, (n * MAP_DIMENSION + internalLoc.x) * MAP_DIMENSION + internalLoc.y);
        uc.write(QUEUE_OFFSET + QUEUE_END, queueEnd + 1);
    }

    /**
     * Recommends a direction to move in, in order to reach a focal point close to a given location
     * Best used when location given is a focal point
     * Returns null if a recommendation can't be given (likely because the area hasn't been processed)
     * Returns Direction.ZERO if already at the given location
     * @param externalTargetLoc location to reach
     * @return a recommended direction or null if no recommendation can be made
     */
    public Direction directionViaFocalPoint(Location externalTargetLoc) {
        final Location curLoc = convertToInternalCoordinates(uc.getLocation());
        final int bestIdx = findBestDistanceMapIdx(curLoc, convertToInternalCoordinates(externalTargetLoc));
        if (bestIdx == -1) return null;

        final int currentDist = readMapLocation(bestIdx, curLoc.x, curLoc.y);
        if (currentDist == 0) return null;
        if (currentDist == INITIAL_DISTANCE) return Direction.ZERO;

        if (uc.canMove(Direction.NORTHWEST) && currentDist - DISTANCE_ROOT == readMapLocation(bestIdx, curLoc.x + Direction.NORTHWEST.dx, curLoc.y + Direction.NORTHWEST.dy)) return Direction.NORTHWEST;
        if (uc.canMove(Direction.NORTHEAST) && currentDist - DISTANCE_ROOT == readMapLocation(bestIdx, curLoc.x + Direction.NORTHEAST.dx, curLoc.y + Direction.NORTHEAST.dy)) return Direction.NORTHEAST;
        if (uc.canMove(Direction.SOUTHWEST) && currentDist - DISTANCE_ROOT == readMapLocation(bestIdx, curLoc.x + Direction.SOUTHWEST.dx, curLoc.y + Direction.SOUTHWEST.dy)) return Direction.SOUTHWEST;
        if (uc.canMove(Direction.SOUTHEAST) && currentDist - DISTANCE_ROOT == readMapLocation(bestIdx, curLoc.x + Direction.SOUTHEAST.dx, curLoc.y + Direction.SOUTHEAST.dy)) return Direction.SOUTHEAST;
        if (uc.canMove(Direction.NORTH) && currentDist - DISTANCE_UNIT == readMapLocation(bestIdx, curLoc.x + Direction.NORTH.dx, curLoc.y + Direction.NORTH.dy)) return Direction.NORTH;
        if (uc.canMove(Direction.EAST) && currentDist - DISTANCE_UNIT == readMapLocation(bestIdx, curLoc.x + Direction.EAST.dx, curLoc.y + Direction.EAST.dy)) return Direction.EAST;
        if (uc.canMove(Direction.SOUTH) && currentDist - DISTANCE_UNIT == readMapLocation(bestIdx, curLoc.x + Direction.SOUTH.dx, curLoc.y + Direction.SOUTH.dy)) return Direction.SOUTH;
        if (uc.canMove(Direction.WEST) && currentDist - DISTANCE_UNIT == readMapLocation(bestIdx, curLoc.x + Direction.WEST.dx, curLoc.y + Direction.WEST.dy)) return Direction.WEST;
        return Direction.ZERO;
    }
    public int directionsFromFocalPoint(Location externalTargetLoc) {
        Location curLoc = convertToInternalCoordinates(externalTargetLoc);
        final int bestIdx = findBestDistanceMapIdx(convertToInternalCoordinates(uc.getLocation()), curLoc);
        int dist = infIfZero(readMapLocation(bestIdx, curLoc.x, curLoc.y));
        int n = 0;
        while (dist > INITIAL_DISTANCE) {
            if (dist - DISTANCE_ROOT == readMapLocation(bestIdx, curLoc.x + Direction.NORTHEAST.dx, curLoc.y + Direction.NORTHEAST.dy)) {
                dist -= DISTANCE_ROOT;
                curLoc = curLoc.add(Direction.NORTHEAST);
                returnedDirections[n++] = Direction.SOUTHWEST;
                continue;
            }
            if (dist - DISTANCE_ROOT == readMapLocation(bestIdx, curLoc.x + Direction.SOUTHEAST.dx, curLoc.y + Direction.SOUTHEAST.dy)) {
                dist -= DISTANCE_ROOT;
                curLoc = curLoc.add(Direction.SOUTHEAST);
                returnedDirections[n++] = Direction.NORTHWEST;
                continue;
            }
            if (dist - DISTANCE_ROOT == readMapLocation(bestIdx, curLoc.x + Direction.SOUTHWEST.dx, curLoc.y + Direction.SOUTHWEST.dy)) {
                dist -= DISTANCE_ROOT;
                curLoc = curLoc.add(Direction.SOUTHWEST);
                returnedDirections[n++] = Direction.NORTHEAST;
                continue;
            }
            if (dist - DISTANCE_ROOT == readMapLocation(bestIdx, curLoc.x + Direction.NORTHWEST.dx, curLoc.y + Direction.NORTHWEST.dy)) {
                dist -= DISTANCE_ROOT;
                curLoc = curLoc.add(Direction.NORTHWEST);
                returnedDirections[n++] = Direction.SOUTHEAST;
                continue;
            }

            if (dist - DISTANCE_UNIT == readMapLocation(bestIdx, curLoc.x + Direction.NORTH.dx, curLoc.y + Direction.NORTH.dy)) {
                dist -= DISTANCE_UNIT;
                curLoc = curLoc.add(Direction.NORTH);
                returnedDirections[n++] = Direction.SOUTH;
                continue;
            }
            if (dist - DISTANCE_UNIT == readMapLocation(bestIdx, curLoc.x + Direction.EAST.dx, curLoc.y + Direction.EAST.dy)) {
                dist -= DISTANCE_UNIT;
                curLoc = curLoc.add(Direction.EAST);
                returnedDirections[n++] = Direction.WEST;
                continue;
            }
            if (dist - DISTANCE_UNIT == readMapLocation(bestIdx, curLoc.x + Direction.SOUTH.dx, curLoc.y + Direction.SOUTH.dy)) {
                dist -= DISTANCE_UNIT;
                curLoc = curLoc.add(Direction.SOUTH);
                returnedDirections[n++] = Direction.NORTH;
                continue;
            }
            if (dist - DISTANCE_UNIT == readMapLocation(bestIdx, curLoc.x + Direction.WEST.dx, curLoc.y + Direction.WEST.dy)) {
                dist -= DISTANCE_UNIT;
                curLoc = curLoc.add(Direction.WEST);
                returnedDirections[n++] = Direction.EAST;
                continue;
            }
            return -1;
        }
        return n;
    }

    private int infIfZero(int x) {
        return x == 0 ? 1_000_000_000 : x;
    }
}