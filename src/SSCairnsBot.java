
import battleship.*;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

/**
 * A Sample random shooter - Takes no precaution on double shooting and has no strategy once
 * a ship is hit - This is not a good solution to the problem!
 *
 * @author mark.yendt@mohawkcollege.ca (Dec 2021)
 */
public class SSCairnsBot implements BattleShipBot {
    private int gameSize;
    private BattleShip2 battleShip;
    private Random random;

    private int[][] heatMap;
    ArrayList<Integer> existingShips;

    // mode vars
    private boolean isHunt;
    private int parity;
    private int hitCount;



    /**
     * Constructor keeps a copy of the BattleShip instance
     * Create instances of any Data Structures and initialize any variables here
     * @param b previously created battleship instance - should be a new game
     */

    @Override
    public void initialize(BattleShip2 b) {
        battleShip = b;
        gameSize = b.BOARD_SIZE;

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
    }

    /**
     * Create a random shot and calls the battleship shoot method
     * Put all logic here (or in other methods called from here)
     * The BattleShip API will call your code until all ships are sunk
     */

    @Override
    public void fireShot() {

        if (isHunt) {
            hunt();
        } else { // target mode
            target();
        }

        // get x and y
        int x = random.nextInt(gameSize);
        int y = random.nextInt(gameSize);

        // Will return true if hit a ship
        boolean hit = battleShip.shoot(new Point(x,y));

        // check if mode needs to be changed
        if (hit && isHunt) { // hunting mode, but then hits something -> go into target mode
            isHunt = false;
            heatMap[y][x] = 0;
            hitCount++;
        } else if (hit && !isHunt){ // target mode, and hits -> stay in target mode
            heatMap[y][x] = 0;
        }


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

    private int[][] createHeatMap() {
        // create board
        for (int y = 0; y < heatMap.length; y++) {
            for (int x = 0; x < heatMap[y].length; x++) {
                // 100 for not hit, 0 for hit
                heatMap[y][x] = 100;

            }
        }

        return null;


    }

    private void hunt() {
        // open parity filter
        // parity++ after the shortest ship is struck

        // choose based on algorithm
        // or choose at random

        // hit
        // if still in hunt, hit + parity in one direction
        // continue in other directions
        // OR
        // hit mod parity

    }

    private void target() {
        // dont use parity filter
        // hit around current x y

        // hit all directions to find next part of ship
        // then after finding 2nd segment check opposite
        // alternate in both directions until miss in one direction
        // if miss, focus on opposite side until miss
        // after missing both, definitely sunk

        // call check if sunk
        if (hasDestroyedShip()) {
            hitCount = 0;
            changeMode(); // go back to hunt mode
        }

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

    /**
     * Checks if a ship was destroyed, and updates the existing ship list if it did.
     *
     * @return if a ship was destroyed at a given point
     * */
    private boolean hasDestroyedShip() {

        // the diff between battleship's length and existing ships keeps track of how many
        // ships are destroyed for the whole game
        // this compares it with the recently updated value taken from the ships sunk method
        // eg. if we know we have a diff of 2 ships from the original, but the
        // method returns 3 sunken ships, then we know a ship has been destroyed since
        if (battleShip.numberOfShipsSunk() >
                        ( battleShip.getShipSizes().length - existingShips.size() )){

            // update existing ship tracker
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
     * Authorship of the solution - must return names of all students that contributed to
     * the solution
     * @return names of the authors of the solution
     */

    @Override
    public String getAuthors() {
        return "Mark Yendt (CSAIT Professor)";
    }
}