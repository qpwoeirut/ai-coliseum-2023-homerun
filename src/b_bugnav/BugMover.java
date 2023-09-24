package b_bugnav;

import aic2023.user.*;

public class BugMover {

    private final UnitController uc;
    private final int INF = 10000;
    private int startvar = 0;
    private Direction prev_move = Direction.SOUTH;
    private final int [] perm = {1,2,0,5,4,6,7,3};
    BugMover(UnitController uc) {
        this.uc = uc;
    }

    Direction move (Location fin)
    {
        Location loc = uc.getLocation();
        int minDist = INF;

        if (uc.canMove(loc.directionTo(fin)))
        {
            prev_move = loc.directionTo(fin);
            return prev_move;
        }

        if (uc.canMove(prev_move))
        {
            return prev_move;
        }

        for (int i = 0; i < 8; i++)
        {
            Direction current_dir = Direction.values()[i];
            if (current_dir.isEqual(prev_move))
            {
                Direction next_dir = Direction.values()[(i + 2) % 8];
                Direction prev_dir = Direction.values()[(i + 6) % 8];
                if (uc.canMove(next_dir))
                {
                    prev_move = next_dir;
                    return prev_move;
                } else if (uc.canMove(prev_dir))
                {
                    prev_move = prev_dir;
                    return prev_move;
                }
            }
        }
        prev_move = Direction.NORTHEAST;
        return prev_move;
    }

    Direction move_to_object (MapObject object)
    {
        uc.println("startvar: " + startvar + " " + Direction.values()[startvar]);
        Location loc = uc.getLocation();
        Location[] objects = uc.senseObjects(object, INF);
        int current_index = 0;
        for (int i = 0; i < 8; i++)
        {
            Direction current_dir = Direction.values()[i];
            if (current_dir.isEqual(prev_move))
            {
                current_index = i;
            }
        }

        if ((int)(uc.getRandomDouble() * 30) == 0 && uc.canMove(Direction.values()[(current_index + 1) % 8]))
        {
            prev_move = Direction.values()[(current_index + 1) % 8];
            return prev_move;
        }

        if ((int)(uc.getRandomDouble() * 30) == 0 && uc.canMove(Direction.values()[(current_index + 5) % 8]))
        {
            prev_move = Direction.values()[(current_index + 5) % 8];
            return prev_move;
        }
        if (uc.canMove(prev_move))
        {
            return prev_move;
        }
        if (objects.length != 0)
        {
            //report existence of object
        }

        Direction next_dir = Direction.values()[(current_index + 2) % 8];
        Direction prev_dir = Direction.values()[(current_index + 6) % 8];

        if ((int)(uc.getRandomDouble() * 2) == 0)
        {
            if (uc.canMove(next_dir))
            {
                prev_move = next_dir;
                return prev_move;
            } else if (uc.canMove(prev_dir))
            {
                prev_move = prev_dir;
                return prev_move;
            }
        } else
        {
            if (uc.canMove(prev_dir))
            {
                prev_move = prev_dir;
                return prev_move;
            } else if (uc.canMove(next_dir))
            {
                prev_move = next_dir;
                return prev_move;
            }
        }

//        float vision_range = uc.getType().getStat(UnitStat.VISION_RANGE);
        for (int i = startvar; i < 8; i++)
        {
            Direction current_dir = Direction.values()[i];
            if (!uc.canMove(current_dir))
            {
                prev_move = current_dir;
                return prev_move;
            }
        }
        Direction final_dir = Direction.NORTHEAST;
        uc.println("here again");
        prev_move = final_dir;
        return final_dir;
    }
}
