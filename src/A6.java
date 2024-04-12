import battleship.BattleShip2;

/**
 * Starting code for COMP10205 - Assignment#6 - Version 2 of BattleShip
 * @author mark.yendt@mohawkcollege.ca (Dec 2021)
 */

public class A6 {
    public static void main(String[] args) {

        // DO NOT add any logic to this code
        // All logic must be added to your Bot implementation
        // see fireShot in the ExampleBot class

        final int NUMBEROFGAMES = 1; // 10000
        System.out.println(NUMBEROFGAMES);
        System.out.println(BattleShip2.getVersion());
        BattleShip2 battleShip = new BattleShip2(NUMBEROFGAMES, new SSCairnsBot());
        int [] gameResults = battleShip.run();

        // You may add some analysis code to look at all the game scores that are returned in gameResults
        // This can be useful for debugging purposes.

        // print result for each game

        // print if perfect or waaaaay too high
        for (int i = 0; i < gameResults.length; i++){
            if (gameResults[i] == 24 || gameResults[i] >= 225) System.out.println("Result of Game #" + i + ": " + gameResults[i]);
        }


        battleShip.reportResults();
    }
}
