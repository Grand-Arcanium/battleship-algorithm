
import battleship.*;
import java.awt.Point;
import java.util.*;

/**
 * <p>
 *      A simple Battleship bot, employing a simple Seek and Destroy algorithm with which it
 *      searches at random for the ships of the opponent, until it hits any of its spots, where
 *      it then will follow up and check adjacent spots until it has sunk it.
 * </p>
 * <p>
 *      Also implements multiple data structures, such as a hashset to keep track previously selected
 *      targets (and mark them as invalid) and stacks to implement a logical order to some operations.
 * </p>
 * <p>
 *      Future developments of a heatmap based algorithm and smarter and more tuned search
 *      operations are still a Work In Progress (Note to prof: This is a way for us to say
 *      that this was our <b>Minimum Viable Product</b> sir)
 * </p>
 *
 * @author Mari Shenzhen Uy
 * @author Mauricio Canul
 */
public class SSCairnsBot implements BattleShipBot {

    /**
     * Variable to implement smarter search while saving the base heat map to memory
     */
    private static int[][] initHeatmap = null;

    /**
     * Variable to define the size of the "board", whether it is a 8x8 or 15x15 grid
     * where ships are placed
     */
    private int gameSize;

    /**
     * The battleship game object, part of the BattleShip2API implementation
     */
    private BattleShip2 battleShip;

    /**
     * Our random number generator, at the moment used to hit random objectives
     */
    private Random random;

    /**
     * Variable to implement smarter search, to be dynamically altered
     */
    private int[][] heatMap;

    /**
     * ArrayList to keep track of ships that have not been sunk yet
     */
    ArrayList<Integer> existingShips;

    /**
     * ships for parity
     */
    Queue<Integer> shipsQ;

    /**
     * Seek and Destroy alternating boolean
     */
    private boolean seeking;

    /**
     * Int to define the parity of the search algorithm (basically, whether to
     * shoot each 2 squares or each X based on which ship has been sank already)
     */
    private int parity;

    /**
     * How many hits have been scored on the currently hunted ship
     */
    private int hitCount;

    /**
     * What tiles of the map have already been chosen to be fired at
     */
    private HashSet<Point> used;

    /**
     * Ship currently found after seeking, and that targeted for destruction
     */
    private Point huntedTarget;

    /**
     * The possible tiles surrounding the current target where the rest of
     * the ship could be
     */
    private Stack<Point> pTargets;

    /**
     * A stack of string to be used by pTargets to acknowledge in which
     * directions the possible targets are facing
     */
    private Stack<String> directions;

    /**
     * String to keep track of which direction is currently being checked as a
     * possible follow-up target, used to rule out a horizontal shot when a
     * vertical ship has been spotted.
     */
    private String checking;

    /**
     * String meant to be used in conjunction with checking and the directions stack
     * to rule out non-viable targets (this stores what direction a ship is heading)
     */
    private String successDir;

    /**
     * Stack used to keep track of the hit scored on the huntedTarget, this aids us to rule
     * out adjacent spots to the ship as possible targets, as no ship may be colliding with
     * another either vertically or horizontally according to the given rules
     */
    private Stack<Point> sinkingShip;

    /**
     * Constructor keeps a copy of the BattleShip instance
     * Create instances of any Data Structures and initialize any variables here
     * @param b previously created battleship instance - should be a new game
     */

    @Override
    public void initialize(BattleShip2 b) {
        battleShip = b;
        gameSize = BattleShip2.BOARD_SIZE;

        /*
        if (initHeatmap == null) // instantiate base heatmap
            createHeatmap();
        */

        heatMap = new int[gameSize][gameSize];
        seeking = true; // set mode
        parity = 2;
        hitCount = 0;

        // create list of ships
        int[] ships = battleShip.getShipSizes();
        shipsQ = new ArrayDeque<>();

        existingShips = new ArrayList<>(ships.length);
        for (int ship : ships) {
            existingShips.add(ship);
            shipsQ.add(ship);

        }

        // Need to use a Seed if you want the same results to occur from run to run
        // This is needed if you are trying to improve the performance of your code

        random = new Random();   // Needed for random shooter - not required for more systematic approaches

        used = new HashSet<>();
        pTargets = new Stack<>();
        directions = new Stack<>();
        sinkingShip = new Stack<>();
    }

    /**
     * Create a random shot and calls the battleship shoot method
     * Put all logic here (or in other methods called from here)
     * The BattleShip API will call your code until all ships are sunk
     */

    @Override
    public void fireShot() {
        Point objective;
        if (seeking) {
            objective = seeking();
        } else { // target mode
            objective = destroy();
        }
        // Will return true if hit a ship
        boolean hit = battleShip.shoot(objective);

        // check if mode needs to be changed
        if (hit && seeking) { // Seeking mode, but then hits something -> go into Destroy mode
            seeking = false;
            //heatMap[y][x] = 0;
            hitCount++;
            huntedTarget = objective;
            setUpTargets(objective);
            sinkingShip.push(objective);
            //heatMap[y][x] = 0;
        } else if(hit && !seeking){ // remains in target mode
            hitCount++;
            successDir = checking;
            sinkingShip.push(objective);
            followUpTarget(objective);
            trimMismatches();
        } else if (!hit){ // hunting but no hit
            //used.add(objective);

        }
        if(!seeking && pTargets.size() == 0) {
            updateParity(hitCount);
            hitCount = 0;
            seeking = true;
            successDir = null;
            eliminateAdjacent();
        }
        kludgeCheck();
    }

    /**
     * Function used as a stopping mechanism in the case that one somehow marks every possible
     * spot as having been fired at while simultaneously the API returns that not all ships
     * have been sunk, should not be included in final stable version, kept here as of now
     * for debugging’s sake
     */
    private void kludgeCheck() {
        if(used.size() == (gameSize * gameSize)) {
            /*
            System.out.println(used.size());
            System.out.println(battleShip.allSunk());
            */
            if(!battleShip.allSunk()){
                throw new RuntimeException("We ran out of places to search!");
            }
        }
    }

    /**
     *
     */
    private void createHeatmap() {
        // create board
        for (int y = 0; y < heatMap.length; y++) {
            for (int x = 0; x < heatMap[y].length; x++) {
                // 100 for not hit, 0 for hit
                heatMap[y][x] = 100;

            }
        }
    }

    /**
     *
     * @return
     */
    private Point seeking() {
        int x;
        int y;

        do {
            x = random.nextInt(gameSize * parity) / parity;
            y = random.nextInt(gameSize * parity) / parity;
        } while(!used.add(new Point(x, y)));

        //System.out.println("new hunt point: " + x + "," + y);
        return new Point(x, y);
    }

    /**
     * Function used after scoring a successful hit on a ship and entering destroy mode,
     * used to set up possible spots where the next part of the ship could be located,
     * (Up, down, left and right) while also making sure to not create one facing a
     * direction that has already been fired at and that there's a valid point in the
     * grid there.
     * @param curPoint The first hit scored on the currently hunted ship as a Point object
     */
    private void setUpTargets(Point curPoint) {
        if (leftAvail(curPoint.x) && !used.contains(new Point(curPoint.x - 1, curPoint.y))){
            targetEntry("horizontal", new Point(curPoint.x - 1, curPoint.y));
        }
        if (rightAvail(curPoint.x)  && !used.contains(new Point(curPoint.x + 1, curPoint.y))){
            targetEntry("horizontal", new Point(curPoint.x + 1, curPoint.y));
        }
        if (topAvail(curPoint.y)  && !used.contains(new Point(curPoint.x, curPoint.y - 1))){
            targetEntry("vertical", new Point(curPoint.x, curPoint.y - 1));
        }
        if (bottomAvail(curPoint.y) && !used.contains(new Point(curPoint.x, curPoint.y + 1))){
            targetEntry("vertical", new Point(curPoint.x, curPoint.y + 1));
        }
    }

    /**
     * Function used to push values into our two stacks used during "destroy" operations
     * (TODO: Could be replaced with a Key-Pair based stack in a future implementation)
     * @param direction Current "direction" of this point, based on its relative position from the first successful hit
     * @param pointToAdd The point entry to be fired at next
     */
    private void targetEntry(String direction, Point pointToAdd) {
        directions.push(direction);
        pTargets.push(pointToAdd);
    }

    /**
     * Function to be used if the algorithm is currently in "destroy" mode,
     * one enters this mode when successful hit has been made upon a ship,
     * this function encapsulates the needed operations to access one of the
     * valid targets
     * @return The next point to be fired at
     */
    private Point destroy() {

        Point cTarget = pTargets.pop();
        checking = directions.pop();
        used.add(cTarget);
        return cTarget;

        /*
        // call check if sunk
        if (hasDestroyedShip()) {
        hitCount = 0;
        changeMode(); // go back to hunt mode
        }
        */
    }

    /**
     * Function used to set up a new target in the pTargets stack in the case that a previous target
     * has been found as a successful hit, it will evaluate the direction it is heading based on its
     * position from the first target acquired during seek mode and also evaluate if there's a valid
     * position in that direction
     * @param nTarget the last target to be hit during Destroy mode
     */
    private void followUpTarget(Point nTarget){
        Point additionalTarget;

        int xDif = (nTarget.x - huntedTarget.x != 0) ? (nTarget.x - huntedTarget.x > 0) ? 1 : -1 : 0;
        int yDif = (nTarget.y - huntedTarget.y != 0) ? (nTarget.y - huntedTarget.y > 0) ? 1 : -1 : 0;

        additionalTarget = new Point(nTarget.x + xDif, nTarget.y + yDif);

        if(additionalTarget.x >= 0 && additionalTarget.x < gameSize && additionalTarget.y >= 0 && additionalTarget.y < gameSize){
            if(!used.contains(additionalTarget)){
                targetEntry(successDir, additionalTarget);
            }
        }
    }

    /**
     * Method used to eliminate other remaining possible targets if the currently
     * hunted ship has already been recognized as either facing horizontally or
     * vertically (it eliminates members in the "pTargets" stack facing in the wrong
     * direction)
     */
    private void trimMismatches(){
        while(directions.size() > 0 && !directions.peek().equals(successDir)){
            directions.pop();
            used.add(pTargets.pop());
        }
    }

    /**
     * Method used to check every spot that has been shot of the current hunted ship,
     * as the logic of this version of the game dictates, ships cannot have their
     * edges touch either vertically or horizontally, therefore, once a ship has been
     * shot down, each adjacent tile gets marked as an invalid target. (Ignoring diagonally
     * adjacent tiles).
     */
    private void eliminateAdjacent(){
        while (sinkingShip.size() > 0){
            Point prevHit = sinkingShip.pop();
            if (leftAvail(prevHit.x)){
                used.add(new Point(prevHit.x - 1, prevHit.y));
            }
            if (rightAvail(prevHit.x)){
                used.add(new Point(prevHit.x + 1, prevHit.y));
            }
            if (topAvail(prevHit.y)){
                used.add(new Point(prevHit.x, prevHit.y - 1));
            }
            if (bottomAvail(prevHit.y)){
                used.add(new Point(prevHit.x, prevHit.y + 1));
            }
        }
    }

    /**
     *
     * @param hitCount
     */
    private void updateParity(int hitCount) {

        int check = shipsQ.peek() == null? -1 : shipsQ.peek(); // get the front, which is the smallest ship
        if (check == hitCount) { // only increase parity if the smallest ship is gone
            parity++;
            shipsQ.remove();
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

    /**
     * Boolean evaluation function used to define whether the given coordinate
     * values are sitting in any of the corners of the 2d array grid
     * @param x the position to be checked in the 2d array's "X axis"
     * @param y the position to be checked in the 2d array's "Y axis"
     * @return True if the given coordinates touch any of the edges of the grid
     */
    private boolean isCorner(int x, int y) {
        return (!leftAvail(x) || !rightAvail(x)) && (!topAvail(y) || !bottomAvail(y));
    }

    /**
     * Boolean evaluation function used to define whether the given coordinate
     * values are sitting in any of the edges of the 2d array grid
     * @param x the position to be checked in the 2d array's "X axis"
     * @param y the position to be checked in the 2d array's "Y axis"
     * @return True if the given coordinates touch any of the corners of the grid
     */
    private boolean isEdge(int x, int y) {
        return (!leftAvail(x) || !rightAvail(x)) && (topAvail(y) || bottomAvail(y))  // left and right edge
            || (leftAvail(x) || rightAvail(x)) && (!topAvail(y) || !bottomAvail(y)); // up and down edge
    }

    /**
     * A function made for readability’s sake in order to evaluate if, in a 2D
     * array representing a grid, the current x value has a valid index before it
     * @param x the x value in the 2d grid
     * @return True if the x value has available space before it
     */
    private boolean leftAvail(int x){
        return x > 0;
    }

    /**
     * A function made for readability’s sake in order to evaluate if, in a 2D
     * array representing a grid, the current x value has a valid index next to it
     * @param x the x value in the 2d grid
     * @return True if the x value has available space next to it
     */
    private boolean rightAvail(int x){
        return x < gameSize - 1;
    }

    /**
     * A function made for readability’s sake in order to evaluate if, in a 2D
     * array representing a grid, the current y value has a valid index above it
     * @param y the y value in the 2d grid
     * @return True if the y value has available space above it
     */
    private boolean topAvail(int y){
        return y > 0;
    }

    /**
     * A function made for readability’s sake in order to evaluate if, in a 2D
     * array representing a grid, the current y value has a valid index below it
     * @param y the y value in the 2d grid
     * @return True if the y value has available space below it
     */
    private boolean bottomAvail(int y){
        return y < gameSize - 1;
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
