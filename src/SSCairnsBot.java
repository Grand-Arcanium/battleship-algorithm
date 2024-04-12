
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

    // main data
    private static double[][] initHeatmap;
    private int gameSize;
    private BattleShip2 battleShip;
    private Random random;

    // heatmap + ships
    ArrayList<Integer> existingShips;
    Queue<Integer> smallestShipQueue; // ships for parity
    private double[][] realheatmap;
    private boolean s2, s3, s4a, s4b, s5, s6; // ship + size
    private Point prevHit;

    // target + hunt modes
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

        // instantiate main data
        battleShip = b;
        gameSize = BattleShip2.BOARD_SIZE;

        isHunt = true; // set mode
        parity = 2; // smallest by default
        hitCount = 0;

        used = new HashSet<>();
        pTargets = new Stack<>();
        directions = new Stack<>();
        sinkingShip = new Stack<>();

        // instantiate all ships
        s2 = true;
        s3 = true;
        s4a = true;
        s4b = true;
        s5 = true;
        s6 = true;

        // create heatmap

        //realheatmap = new double[gameSize][gameSize];
        //createHeatmap();
        if (initHeatmap == null) {
            initHeatmap = createHeatmap();
        }

        // copy from init heatmap
        realheatmap = new double[gameSize][];
        for(int i = 0; i < gameSize; i++)
        {
            realheatmap[i] = initHeatmap[i].clone();
        }

        // printMap(realheatmap);

        // create list of ships
        int[] ships = battleShip.getShipSizes();
        smallestShipQueue = new ArrayDeque<>();

        existingShips = new ArrayList<>(ships.length);
        for (int ship : ships) {
            existingShips.add(ship);
            smallestShipQueue.add(ship);

        }

        // Need to use a Seed if you want the same results to occur from run to run
        // This is needed if you are trying to improve the performance of your code

        random = new Random();   // Needed for random shooter - not required for more systematic approaches


    }

    /**
     * Create a random shot and calls the battleship shoot method
     * Put all logic here (or in other methods called from here)
     * The BattleShip API will call your code until all ships are sunk
     */

    @Override
    public void fireShot() {

        //printMap(realheatmap);

        Point objective;

        if (isHunt) {
            objective = hunt();
        } else { // target mode
            objective = target();
        }

        // Will return true if hit a ship
        System.out.println("shooting: " + objective.x + "," + objective.y);
        boolean hit = battleShip.shoot(objective);

        //printMap(realheatmap);

        updateHeatmap(objective); // update heatmap

        // check if mode needs to be changed
        if (hit && isHunt) { // hunting mode, but then hits something -> go into target mode
            System.out.println("Let the Hunt begin!"); // <- ik it technically goes into target mode but this is a destiny ref so
            isHunt = false;
            hitCount++;
            huntedTarget = objective;
            setUpTargets(objective);
            sinkingShip.push(objective);

        } else if(hit && !isHunt){ // remains in target mode
            hitCount++;
            successDir = checking;
            sinkingShip.push(objective);
            followUpTarget(objective);
            trimMismatches();
        }

        // sunk
        if(!isHunt && pTargets.size() == 0) {
            System.out.println("Let the Wolves rest.");
            updateParity(hitCount);
            sunk(hitCount);
            hitCount = 0;
            isHunt = true;
            successDir = null;
            eliminateAdjacent();
        }
        kludgeCheck();
    }

    private void kludgeCheck() {
        if(used.size() == (gameSize * gameSize)) {
/*          System.out.println(used.size());
            System.out.println(battleShip.allSunk());*/
            if(!battleShip.allSunk()){
                throw new RuntimeException("We ran out of places to search!");
            }
        }
    }

    private double toPercent(double num) {
        double prob = num / (gameSize * gameSize);
        return Math.round((prob * 100.0) * 10.0) / 10.0;
    }

    /**
     * Toggle ship's existence
     *
     * @param size ship size (2-6)
     * */
    private void sunk(int size) {
        switch (size) {
            case 2:
                if (s2) s2 = false;
                //System.out.println("Sunk size 2!");
                break;
            case 3:
                if (s3) s3 = false;
                // System.out.println("Sunk size 3!");
                break;
            case 4:
                if (s4a) {
                    s4a = false; //System.out.println("Sunk size 4 #1!");
                }
                else {
                    s4b = false; //System.out.println("Sunk size 4 #2!");
                }
                break;
            case 5:
                if (s5) s5 = false;
                // System.out.println("Sunk size 5!");
                break;
            case 6:
                if (s6) s6 = false;
                // System.out.println("Sunk size 6!");
                break;
            default:
                break;

        }

    }

    /**
     * Check if a point is valid by being within boundaries and not shot at
     * */
    private boolean pointIsValid(Point p) {

        //return p.x >= 0 && p.x < gameSize && p.y >= 0 && p.y < gameSize && !used.contains(p);

        return pointIsValid(p.x, p.y);
    }

    private boolean pointIsValid(int x, int y) {

        return x >= 0 && x < gameSize && y >= 0 && y < gameSize &&
                !used.contains(new Point(x,y));


    }

    private boolean pointIsValid(int x, int y, boolean shotDoesNotMatter) {
        return x >= 0 && x < gameSize && y >= 0 && y < gameSize;


    }

    private double highest;

    private double[][] createHeatmap() {

        double[][] map = new double[gameSize][gameSize];

        int totalValue = 0;
        int total = 0;

        for (int y = 0; y < gameSize; y++) {
            for (int x = 0; x < gameSize; x++) {

                Point curPoint = new Point(x, y);
                double prob = 0;

                if (pointIsValid(curPoint)) {
                    // check right, left, down, up
                    // individual rather than for loop for O(1) access

                    /*
                    if (s2) { // size 2 exists
                        if (pointIsValid(new Point(x + 1, y))) {
                            prob++;
                        }

                        if (pointIsValid(new Point(x - 1, y))) {
                            prob++;
                        }

                        if (pointIsValid(new Point(x, y + 1))) {
                            prob++;
                        }

                        if (pointIsValid(new Point(x, y - 1))) {
                            prob++;
                        }

                    }

                    */

                    if (s3) { // size 3 exists
                        if (pointIsValid(new Point(x + 1, y)) &&
                                pointIsValid(new Point(x + 2, y))) {
                            prob++;
                        }

                        if (pointIsValid(new Point(x - 1, y)) &&
                                pointIsValid(new Point(x - 2, y))) {
                            prob++;
                        }

                        if (pointIsValid(new Point(x, y + 1)) &&
                                pointIsValid(new Point(x, y + 2))) {
                            prob++;
                        }

                        if (pointIsValid(new Point(x, y - 1)) &&
                                pointIsValid(new Point(x, y - 2))) {
                            prob++;
                        }

                    }

                    if (s4a || s4b) { // size 4 exists
                        if (pointIsValid(new Point(x + 1, y)) &&
                                pointIsValid(new Point(x + 2, y)) &&
                                pointIsValid(new Point(x + 3, y))) {
                            prob++;
                        }

                        if (pointIsValid(new Point(x - 1, y)) &&
                                pointIsValid(new Point(x - 2, y)) &&
                                pointIsValid(new Point(x - 3, y))) {
                            prob++;
                        }

                        if (pointIsValid(new Point(x, y + 1)) &&
                                pointIsValid(new Point(x, y + 2)) &&
                                pointIsValid(new Point(x, y + 3))) {
                            prob++;
                        }

                        if (pointIsValid(new Point(x, y - 1)) &&
                                pointIsValid(new Point(x, y - 2)) &&
                                pointIsValid(new Point(x, y - 3))) {
                            prob++;
                        }

                    }
                    if (s5) { // size 5 exists
                        if (pointIsValid(new Point(x + 1, y)) &&
                                pointIsValid(new Point(x + 2, y)) &&
                                pointIsValid(new Point(x + 3, y)) &&
                                pointIsValid(new Point(x + 4, y))) {
                            prob++;
                        }

                        if (pointIsValid(new Point(x - 1, y)) &&
                                pointIsValid(new Point(x - 2, y)) &&
                                pointIsValid(new Point(x - 3, y)) &&
                                pointIsValid(new Point(x - 4, y))) {
                            prob++;
                        }

                        if (pointIsValid(new Point(x, y + 1)) &&
                                pointIsValid(new Point(x, y + 2)) &&
                                pointIsValid(new Point(x, y + 3)) &&
                                pointIsValid(new Point(x, y + 4))) {
                            prob++;
                        }

                        if (pointIsValid(new Point(x, y - 1)) &&
                                pointIsValid(new Point(x, y - 2)) &&
                                pointIsValid(new Point(x, y - 3)) &&
                                pointIsValid(new Point(x, y - 4))) {
                            prob++;
                        }

                    }
                    if (s6) { // size 6 exists
                        if (pointIsValid(new Point(x + 1, y)) &&
                                pointIsValid(new Point(x + 2, y)) &&
                                pointIsValid(new Point(x + 3, y)) &&
                                pointIsValid(new Point(x + 4, y)) &&
                                pointIsValid(new Point(x + 5, y))) {
                            prob++;
                        }

                        if (pointIsValid(new Point(x - 1, y)) &&
                                pointIsValid(new Point(x - 2, y)) &&
                                pointIsValid(new Point(x - 3, y)) &&
                                pointIsValid(new Point(x - 4, y)) &&
                                pointIsValid(new Point(x - 5, y))) {
                            prob++;
                        }

                        if (pointIsValid(new Point(x, y + 1)) &&
                                pointIsValid(new Point(x, y + 2)) &&
                                pointIsValid(new Point(x, y + 3)) &&
                                pointIsValid(new Point(x, y + 4)) &&
                                pointIsValid(new Point(x, y + 5))) {
                            prob++;
                        }

                        if (pointIsValid(new Point(x, y - 1)) &&
                                pointIsValid(new Point(x, y - 2)) &&
                                pointIsValid(new Point(x, y - 3)) &&
                                pointIsValid(new Point(x, y - 4)) &&
                                pointIsValid(new Point(x, y - 5))) {
                            prob++;
                        }

                    }

                }
                else {
                    map[y][x] = 0;

                }

                /*
                //weigh corners + edges more?
                if (isEdge(x,y)) {
                    prob *= 1.25;
                } else if (isCorner(x,y)) {
                    prob *= 1.5;
                }

                */

                double result = prob;

                //totalValue += result;
                //total += prob;
                map[y][x] = result; // add to heat map

            }

        }
/*
        // Normalize probabilities
        for (int i = 0; i < gameSize; i++) {
            for (int j = 0; j < gameSize; j++) {
                map[i][j] /= totalValue;
            }
        }

*/
        normalize(map);

        return map;
    }

    private void normalize(double[][] map) {
        double total = 0;
        highest = 0;

        for (int i = 0; i < gameSize; i++) {
            for (int j = 0; j < gameSize; j++) {
                total += (map[i][j]);
                highest = Math.max(map[i][j], highest);
            }
        }

        for (int i = 0; i < gameSize; i++) {
            for (int j = 0; j < gameSize; j++) {
                map[i][j] = map[i][j] * 100.0 / total;
            }
        }

    }

    private void updateHeatmap(Point target) {

        // check every direction from target
        for (int dy = -5; dy <= 5; dy++) {
            for (int dx = -5; dx <= 5; dx++) {

                // check this point
                int newX = target.x + dx;
                int newY = target.y + dy;
                if (pointIsValid(newX, newY, true)) {
                    double distance = Math.sqrt(dx * dx + dy * dy); // distance between target and new point

                    // get a value to subtract % on all sides depending on distance
                    double decreasePercentage = calculateDecreasePercentage(distance);

                    double result = adjustPercentage(realheatmap[newY][newX], decreasePercentage);

                    realheatmap[newY][newX] = result;

                    if (realheatmap[newY][newX] < 0 ) realheatmap[newY][newX] = 0;

                }
            }
        }

        normalize(realheatmap);

    }

    /**
     * Using a Gaussian-like function (normal distribution) to get the smooth function for
     * gradually decreasing the % to remove the further the point is from the center point
     *
     * */
    private double calculateDecreasePercentage(double distance) {
        double sigma = 10; // to fine tune: smaller decreases = higher sigma
        return Math.exp(-distance * distance / (2 * sigma * sigma));
    }

    private double adjustPercentage(double original, double toAdjustBy) {
        double newPercent = original - toAdjustBy;

        if (newPercent < 0) { newPercent = 0; }
        else if (newPercent > 1) { newPercent = 1; }

        return newPercent;

    }

    public void printMap(double[][] map) {
        for (int y = 0; y < 15; y++) {
            for (int x = 0; x < 15; x++) {
                System.out.print(map[y][x] + " | ");
            }
            System.out.println();


        }

    }

    public ArrayList<Point> getBest() {


        ArrayList<Point> choices = new ArrayList<>();
        // System.out.println("parity " + parity);
        // compare for highest, O(n^2)
        for (int y = 0; y < gameSize; y++) {
            for (int x = 0; x < gameSize; x++) {

                if ((realheatmap[y][x] >= highest)) {

                    highest = realheatmap[y][x]; // replace
                    choices.add(new Point(x,y)); // add to list


                }
            }
        }
        return choices;

    }

    private Point hunt() {
        int x;
        int y;

        ArrayList<Point> choices = getBest();

        // get from heatmap
        if (choices.size() != 0) {
            for (int c = choices.size() - 1; c >= 0; c--) { // get last item, which is highest item

                Point p = new Point(choices.get(c));

                if ((p.x % parity) == 0 || (p.y % parity) == 0) {
                    if (used.add(new Point(p.x, p.y))) {
                        System.out.println("map chosen!");
                        return p;

                    }
                }
            }
        }

        // get random (not likely to happen)
        System.out.println("rand chosen!");
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

        int check = smallestShipQueue.peek() == null? -1 : smallestShipQueue.peek(); // get the front, which is the smallest ship
        if (check == hitCount) { // only increase parity if the smallest ship is gone
            parity++;
            smallestShipQueue.remove();
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
     * Checks if a ship was destroyed, and updates the existing ship list and booleans if it did.
     *
     * @return if a ship was destroyed at a given point
     * */
    private boolean wasShipDestroyed() {

        /*
         the diff between battleship's length and existing ships keeps track of how many
         ships are destroyed for the whole game
         this compares it with the recently updated value taken from the ships sunk method
         eg. if we know we have a diff of 2 ships from the original, but the
         method returns 3 sunken ships, then we know a ship has been destroyed since.
        */

        if (battleShip.numberOfShipsSunk() >
                        ( battleShip.getShipSizes().length - existingShips.size() )){
            // update existing ship tracker
            // TODO: should be a better way to find the right index to remove than iterating in a for loop
            for (int i = 0; i < existingShips.size(); i++) {
                if (existingShips.get(i) == hitCount) {
                    existingShips.remove(i); // remove from existing ships
                    sunk(hitCount); // sink the ship
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
