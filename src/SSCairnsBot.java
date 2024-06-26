
import battleship.*;

import java.awt.Point;
import java.util.*;

/**
 * A Sample random shooter - Takes no precaution on double shooting and has no strategy once
 * a ship is hit - This is not a good solution to the problem!
 *
 * @author mark.yendt@mohawkcollege.ca (Dec 2021)
 */
public class SSCairnsBot implements BattleShipBot {
    private static int[][] initHeatmap = null;
    private int gameSize;
    private BattleShip2 battleShip;
    private Random random;

    private int[][] heatMap;
    ArrayList<Integer> existingShips;

    // mode vars
    private boolean isHunt;
    private int parity;
    private int hitCount;
    private HashSet<Point> used;
    private Point huntedTarget;
    private Stack<Point> pTargets;

    /**
     * Constructor keeps a copy of the BattleShip instance
     * Create instances of any Data Structures and initialize any variables here
     * @param b previously created battleship instance - should be a new game
     */

    @Override
    public void initialize(BattleShip2 b) {
        battleShip = b;
        gameSize = BattleShip2.BOARD_SIZE;

        if (initHeatmap == null) // instantiate base heatmap
            createHeatmap();

        heatMap = new int[gameSize][gameSize];
        isHunt = true; // set mode
        parity = 0;
        hitCount = 0;

        // create list of ships
        int[] ships = battleShip.getShipSizes();
        existingShips = new ArrayList<>(ships.length);
        for (int ship : ships) {
            existingShips.add(ship);
        }

        // Need to use a Seed if you want the same results to occur from run to run
        // This is needed if you are trying to improve the performance of your code

        random = new Random(0xDEADBEEF);   // Needed for random shooter - not required for more systematic approaches
        used = new HashSet<>();
    }

    /**
     * Create a random shot and calls the battleship shoot method
     * Put all logic here (or in other methods called from here)
     * The BattleShip API will call your code until all ships are sunk
     */

    @Override
    public void fireShot() {

        Point objective;
        if (isHunt) {
            objective = hunt();
        } else { // target mode
            objective = target();
        }
        // Will return true if hit a ship
        boolean hit = battleShip.shoot(objective);

        // check if mode needs to be changed
        if (hit && isHunt) { // hunting mode, but then hits something -> go into target mode
            isHunt = false;
            //heatMap[y][x] = 0;
            hitCount++;
            huntedTarget = objective;
            pTargets = setUpTargets(objective);
            //heatMap[y][x] = 0;
        } else if (!hit && !isHunt){
            // This is what is killing 4th(ex), it will delete ref, then ref2, ref3 find nothing at ref4 and assume there was nothing to the right (ref is left direction)
            pTargets.pop();
            checkHunt();
        }

    }

    private Stack<Point> setUpTargets(Point curPoint) {
        Stack<Point> possibleTargets = new Stack<>();

        if (curPoint.x != 0 && !used.contains(new Point(curPoint.x - 1, curPoint.y))){
            possibleTargets.push(new Point(curPoint.x - 1, curPoint.y));
        }
        if (curPoint.x + 1 < gameSize  && !used.contains(new Point(curPoint.x + 1, curPoint.y))){
            possibleTargets.push(new Point(curPoint.x + 1, curPoint.y));
        }
        if (curPoint.y != 0  && !used.contains(new Point(curPoint.x, curPoint.y - 1))){
            possibleTargets.push(new Point(curPoint.x, curPoint.y - 1));
        }
        if (curPoint.y + 1 < gameSize  && !used.contains(new Point(curPoint.x, curPoint.y + 1))){
            possibleTargets.push(new Point(curPoint.x, curPoint.y + 1));
        }
        System.out.println(huntedTarget + "" + possibleTargets);

        return possibleTargets;
    }

    /*
    * - make board -> % chance of ship
    * - algorithm to find that %
    * - 2 states: hunt, target
    * - no hit = hunt, hit = target until no more ship
    * - update % after every hit
    * - hunt must target best %, target also gets best % relative to last hit
    *
    *
    * fireShot()
    *
    * if hunt mode
    * hunt() -> return x,y
    *
    * shoot(x,y)
    * check if hit
    *
    * if hunt
    * hit? -> change to target mode
    * no hit? -> stay hunt mode
    * LOOP
    *
    *
    * fireShot ()
    *
    * if target
    * target() -> return x,y
    *
    * shoot
    * check if hit
    *
    * if target
    * hit? -> stay target
    * no hit? -> stay target
    * some check if sunk ship -> change to hunt if sunk
    * LOOP
    *
    *
    * */

    private void createHeatmap() {
        // create board
        for (int y = 0; y < heatMap.length; y++) {
            for (int x = 0; x < heatMap[y].length; x++) {
                // 100 for not hit, 0 for hit
                heatMap[y][x] = 100;

            }
        }

    }

    private Point hunt() {
        // open parity filter
        // parity++ after the shortest ship is struck

        // choose based on algorithm
        // or choose at random

        // hit
        // if still in hunt, hit + parity in one direction
        // continue in other directions
        // OR
        // hit mod parity

        // get x and y
        int x;
        int y;
        do {
            x = random.nextInt(gameSize);
            y = random.nextInt(gameSize);
        } while(!used.add(new Point(x, y)));
        return new Point(x, y);
    }

    private Point target() {
        // dont use parity filter
        // hit around current x y

        // hit all directions to find next part of ship
        // then after finding 2nd segment check opposite
        // alternate in both directions until miss in one direction
        // if miss, focus on opposite side until miss
        // after missing both, definitely sunk

        // call check if sunk

        Point nTarget = pTargets.pop();

        Point additionalTarget;

        int xDif = nTarget.x - huntedTarget.x;
        int yDif = nTarget.y - huntedTarget.y;

        if((nTarget.x + xDif > 0) && (nTarget.y + yDif > 0) && (nTarget.x + xDif < gameSize) &&  (nTarget.y + yDif < gameSize)){
            additionalTarget = new Point(nTarget.x + xDif, nTarget.y + yDif);
            if(!used.contains(additionalTarget)){
                pTargets.push(additionalTarget);
            }
        }

        /*

        // call check if sunk
        if (hasDestroyedShip()) {
        hitCount = 0;
        changeMode(); // go back to hunt mode
        }

        */


        checkHunt();
        return nTarget;
    }

    private int updateParity() {
        int activeShips = battleShip.numberOfShipsSunk();
        // go from hit every 2, then 3 bc of 2 already being sunk, and so on
        return 0;

    }

    /**
     * Change the current mode of the Bot (Hunt Mode <-> Target Mode)
     * */
    private void changeMode() {
        isHunt = !isHunt;
    }

    private void checkHunt(){
        if(pTargets.size() == 0){
            changeMode();
        }
    }


    /**
     * Checks if a ship was destroyed, and updates the existing ship list if it did.
     *
     * @return if a ship was destroyed at a given point
     * */
    private boolean hasDestroyedShip() {

        /*
         the diff between battleship's length and existing ships keeps track of how many
         ships are destroyed for the whole game
         this compares it with the recently updated value taken from the ships sunk method
         eg. if we know we have a diff of 2 ships from the original, but the
         method returns 3 sunken ships, then we know a ship has been destroyed since

        ok so assume currently hitting a ship of size 4

        target()
        M R1 R2 R3 O

        with this function, hitting R4 then checking if sunk will already end the target

        M R1 R2 R3 R4 -> hunt()

        instead of having to check for an R5 that we know doesnt exist
        M R1 R2 R3 R4 0 -> target()
        M R1 R2 R3 R4 M -> hunt()

        */

        if (battleShip.numberOfShipsSunk() >
                        ( battleShip.getShipSizes().length - existingShips.size() )){
            // update existing ship tracker
            // TODO: should be a better way to find the right index to remove than iterating in a for loop
            for (int i = 0; i < existingShips.size(); i++) {
                if (existingShips.get(i) == hitCount) {
                    existingShips.remove(i);
                    break;
                }
            }
            return true;
        }

        return false;
    }

    private boolean isCorner(int x, int y) {
        int length = heatMap.length; // both x and y

        return (x == 0 || x == length - 1) && (y == 0 || y == length - 1);

    }

    private boolean isEdge(int x, int y) {
        int length = heatMap.length; // both x and y

        return ((x == 0 || x == length - 1) && (y < length - 1 && y > 0))  // left and right edge
            || ((y == 0 || y == length - 1) && (x < length - 1 && x > 0)); // up and down edge
    }

    /**
     * Authorship of the solution - must return names of all students that contributed to
     * the solution
     * @return names of the authors of the solution
     */

    @Override
    public String getAuthors() {
        return "Mauricio Canul and Mari Shenzhen Uy";
    }
}
