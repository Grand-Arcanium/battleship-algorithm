
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
    Queue<Integer> shipsQ; // ships for parity

    // mode vars
    private boolean isHunt;
    private int parity;
    private int hitCount;
    private HashSet<Point> used;
    private Point huntedTarget;
    private Stack<Point> pTargets;
    private Stack<String> directions;
    private String checking;
    private String successDir;
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
        isHunt = true; // set mode
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
            setUpTargets(objective);
            sinkingShip.push(objective);
            //heatMap[y][x] = 0;
        } else if(hit && !isHunt){ // remains in target mode
            hitCount++;
            successDir = checking;
            sinkingShip.push(objective);
            followUpTarget(objective);
            trimMismatches();
        } else if (!hit){ // hunting but no hit
            //used.add(objective);

        }
        if(!isHunt && pTargets.size() == 0) {
            updateParity(hitCount);
            hitCount = 0;
            isHunt = true;
            successDir = null;
            eliminateAdjacent();
        }
        kludgeCheck();
    }

    private void kludgeCheck() {
        if(used.size() == (gameSize * gameSize)) {
/*            System.out.println(used.size());
            System.out.println(battleShip.allSunk());*/
            if(!battleShip.allSunk()){
                throw new RuntimeException("We ran out of places to search!");
            }
        }
    }

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
        int x;
        int y;

        do {
            x = random.nextInt(gameSize * parity) / parity;
            y = random.nextInt(gameSize * parity) / parity;
        } while(!used.add(new Point(x, y)));

        //System.out.println("new hunt point: " + x + "," + y);
        return new Point(x, y);
    }

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

    private void targetEntry(String direction, Point pointToAdd) {
        directions.push(direction);
        pTargets.push(pointToAdd);
    }

    private Point target() {

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

    private void followUpTarget(Point nTarget){
        Point additionalTarget;

        int xDif = (nTarget.x - huntedTarget.x != 0) ? (nTarget.x - huntedTarget.x > 0) ? 1 : -1 : 0;
        int yDif = (nTarget.y - huntedTarget.y != 0) ? (nTarget.y - huntedTarget.y > 0) ? 1 : -1 : 0;

        additionalTarget = new Point(nTarget.x + xDif, nTarget.y + yDif);

        if(additionalTarget.x >= 0 && additionalTarget.x < gameSize && additionalTarget.y >= 0 && additionalTarget.y < gameSize){
            if(!used.contains(additionalTarget)){
                directions.push(successDir);
                pTargets.push(additionalTarget);
            }
        }
    }

    private void trimMismatches(){
        while(directions.size() > 0 && !directions.peek().equals(successDir)){
            directions.pop();
            used.add(pTargets.pop());
        }
    }

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

    private void updateParity(int hitCount) {

        int check = shipsQ.peek() == null? -1 : shipsQ.peek(); // get the front, which is the smallest ship
        if (check == hitCount) { // only increase parity if the smallest ship is gone
            parity++;
            shipsQ.remove();
        }


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
        return (!leftAvail(x) || !rightAvail(x)) && (!topAvail(y) || !bottomAvail(y));
    }

    private boolean isEdge(int x, int y) {
        return (!leftAvail(x) || !rightAvail(x)) && (topAvail(y) || bottomAvail(y))  // left and right edge
            || (leftAvail(x) || rightAvail(x)) && (!topAvail(y) || !bottomAvail(y)); // up and down edge
    }

    private boolean leftAvail(int x){
        return x > 0;
    }
    private boolean rightAvail(int x){
        return x < gameSize - 1;
    }
    private boolean topAvail(int y){
        return y > 0;
    }
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
