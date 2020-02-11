package za.co.entelect.challenge;

import za.co.entelect.challenge.entities.Building;
import za.co.entelect.challenge.entities.CellStateContainer;
import za.co.entelect.challenge.entities.GameState;
import za.co.entelect.challenge.enums.BuildingType;
import za.co.entelect.challenge.enums.PlayerType;

import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.Random;

public class Bot {

    private GameState gameState;

    /**
     * Constructor
     *
     * @param gameState the game state
     **/
    public Bot(GameState gameState) {
        this.gameState = gameState;
        gameState.getGameMap();
    }

    /**
     * Run
     *
     * @return the result
     **/
    public String run() {
        String command = "";

        List<Action> actions = new ArrayList<>(gameState.gameDetails.mapHeight);

        Random rand = new Random();

        for (int i = 0; i < gameState.gameDetails.mapHeight; i++) {
            int enemyAttacks = getAllBuildingsForPlayer(
                    PlayerType.B,
                    b -> b.buildingType == BuildingType.ATTACK,
                    i
            ).size();
            int enemyDefenses = getAllBuildingsForPlayer(
                    PlayerType.B,
                    b -> b.buildingType == BuildingType.DEFENSE,
                    i
            ).size();
            int enemyEnergies = getAllBuildingsForPlayer(
                    PlayerType.B,
                    b -> b.buildingType == BuildingType.ENERGY,
                    i
            ).size();
            int enemyTesla = getAllBuildingsForPlayer(
                    PlayerType.B,
                    b -> b.buildingType == BuildingType.TESLA,
                    i
            ).size();
            int myAttacks = getAllBuildingsForPlayer(
                    PlayerType.A,
                    b -> b.buildingType == BuildingType.ATTACK,
                    i
            ).size();
            int myDefenses = getAllBuildingsForPlayer(
                    PlayerType.A,
                    b -> b.buildingType == BuildingType.DEFENSE,
                    i
            ).size();
            int myEnergies = getAllBuildingsForPlayer(
                    PlayerType.A,
                    b -> b.buildingType == BuildingType.ENERGY,
                    i
            ).size();
            int myTesla = getAllBuildingsForPlayer(
                    PlayerType.A,
                    b -> b.buildingType == BuildingType.TESLA,
                    i
            ).size();

            Action selectedAction = new Action(0, 0, 0, 0);
            Action testAction;

            // Placing Energy Building in First Column: 100;
            if (isCellEmpty(0, i) && canAffordBuilding(BuildingType.ENERGY)) {
                testAction = new Action(0, i, 2, 100 + rand.nextInt(5));
                if (testAction.greaterPriority(selectedAction)) {
                    selectedAction = testAction;
                }
            }

            // Placing Energy Building in Second Column: 70;
            if (isCellEmpty(1, i) && canAffordBuilding(BuildingType.ENERGY)) {
                testAction = new Action(1, i, 2, 70 + rand.nextInt(10));
                if (testAction.greaterPriority(selectedAction)) {
                    selectedAction = testAction;
                }
            }

            // Placing Attack Building to destroy Enemy Energy Building: 95;
            if (enemyEnergies > 0 && canAffordBuilding(BuildingType.ATTACK)) {
                testAction = placeBuildingInRowFromBack(1, i, 95 + rand.nextInt(20));
                if (testAction.greaterPriority(selectedAction)) {
                    selectedAction = testAction;
                }
            }

            // Placing Attack Building to destroy Enemy Attack Buildings: 60 * ATK
            if (enemyAttacks > 0 && canAffordBuilding(BuildingType.ATTACK)) {
                testAction = placeBuildingInRowFromBack(
                        1,
                        i,
                        60 + 5 * enemyAttacks + rand.nextInt(5)
                );
                if (testAction.greaterPriority(selectedAction)) {
                    selectedAction = testAction;
                }
            }

            // Placing Defense Building when 0< myAttacks =< enemyAttacks >= 2
            if (enemyAttacks >= 2 && myAttacks > 0 && myAttacks <= enemyAttacks && canAffordBuilding(BuildingType.DEFENSE)) {
                testAction = placeBuildingInRowFromFront(
                        0,
                        i,
                        55 + 5 * (enemyAttacks - myAttacks) + rand.nextInt(5)
                );
                if (testAction.greaterPriority(selectedAction)) {
                    selectedAction = testAction;
                }
            }

            if (enemyAttacks > 0 && myDefenses == 0 && myAttacks == 0 && canAffordBuilding(BuildingType.DEFENSE)) {
                testAction = placeBuildingInRowFromFront(
                        0,
                        i,
                        100 + 5 * enemyAttacks + rand.nextInt(5)
                );
                if (testAction.greaterPriority(selectedAction)) {
                    selectedAction = testAction;
                }
            }

            if (enemyAttacks > 0 && myDefenses > 0 && canAffordBuilding(BuildingType.ATTACK)) {
                testAction = placeBuildingInRowFromBack(
                        1,
                        i,
                        70 + 2 * enemyAttacks + rand.nextInt(5)
                );
                if (testAction.greaterPriority(selectedAction)) {
                    selectedAction = testAction;
                }
            }

            if (myDefenses > 1 && canAffordBuilding(BuildingType.TESLA)) {
                testAction = placeBuildingInRowFromBack(
                        1,
                        i,
                        55 + 2 * enemyAttacks + rand.nextInt(5)
                );
                if (testAction.greaterPriority(selectedAction)) {
                    selectedAction = testAction;
                }
            }


            actions.add(i, selectedAction);
        }

        Action maxAction = new Action(0, 0, 0, 0);
        for (Action action: actions) {
            if (action.greaterPriority(maxAction)) {
                maxAction = action;
            }
        }

        return maxAction.output();
        /*
        //If the enemy has an attack building and I don't have a blocking wall, then block from the front.
        for (int i = 0; i < gameState.gameDetails.mapHeight; i++) {
            int enemyAttackOnRow = getAllBuildingsForPlayer(PlayerType.B, b -> b.buildingType == BuildingType.ATTACK, i).size();
            int myDefenseOnRow = getAllBuildingsForPlayer(PlayerType.A, b -> b.buildingType == BuildingType.DEFENSE, i).size();

            if (enemyAttackOnRow > 0 && myDefenseOnRow == 0) {
                if (canAffordBuilding(BuildingType.DEFENSE))
                    command = placeBuildingInRowFromFront(BuildingType.DEFENSE, i);
                else
                    command = "";
                break;
            }
        }

        //If there is a row where I don't have energy and there is no enemy attack building, then build energy in the back row.
        if (command.equals("")) {
            for (int i = 0; i < gameState.gameDetails.mapHeight; i++) {
                int enemyAttackOnRow = getAllBuildingsForPlayer(PlayerType.B, b -> b.buildingType == BuildingType.ATTACK, i).size();
                int myEnergyOnRow = getAllBuildingsForPlayer(PlayerType.A, b -> b.buildingType == BuildingType.ENERGY, i).size();

                if (enemyAttackOnRow == 0 && myEnergyOnRow == 0) {
                    if (canAffordBuilding(BuildingType.ENERGY))
                        command = placeBuildingInRowFromBack(BuildingType.ENERGY, i);
                    break;
                }
            }
        }

        //If I have a defense building on a row, then build an attack building behind it.
        if (command.equals("")) {
            for (int i = 0; i < gameState.gameDetails.mapHeight; i++) {
                if (getAllBuildingsForPlayer(PlayerType.A, b -> b.buildingType == BuildingType.DEFENSE, i).size() > 0
                        && canAffordBuilding(BuildingType.ATTACK)) {
                    command = placeBuildingInRowFromFront(BuildingType.ATTACK, i);
                }
            }
        }

        //If I don't need to do anything then either attack or defend randomly based on chance (65% attack, 35% defense).
        if (command.equals("")) {
            if (getEnergy(PlayerType.A) >= getMostExpensiveBuildingPrice()) {
                if ((new Random()).nextInt(100) <= 35) {
                    return placeBuildingRandomlyFromFront(BuildingType.DEFENSE);
                } else {
                    return placeBuildingRandomlyFromBack(BuildingType.ATTACK);
                }
            }
        }
        */
    }

    /**
     * Place building in a random row nearest to the back
     *
     * @param buildingType the building type
     * @return the result
     **/
    private String placeBuildingRandomlyFromBack(BuildingType buildingType) {
        for (int i = 0; i < gameState.gameDetails.mapWidth / 2; i++) {
            List<CellStateContainer> listOfFreeCells = getListOfEmptyCellsForColumn(i);
            if (!listOfFreeCells.isEmpty()) {
                CellStateContainer pickedCell = listOfFreeCells.get((new Random()).nextInt(listOfFreeCells.size()));
                return buildCommand(pickedCell.x, pickedCell.y, buildingType);
            }
        }
        return "";
    }

    /**
     * Place building in a random row nearest to the front
     *
     * @param buildingType the building type
     * @return the result
     **/
    private String placeBuildingRandomlyFromFront(BuildingType buildingType) {
        for (int i = (gameState.gameDetails.mapWidth / 2) - 1; i >= 0; i--) {
            List<CellStateContainer> listOfFreeCells = getListOfEmptyCellsForColumn(i);
            if (!listOfFreeCells.isEmpty()) {
                CellStateContainer pickedCell = listOfFreeCells.get((new Random()).nextInt(listOfFreeCells.size()));
                return buildCommand(pickedCell.x, pickedCell.y, buildingType);
            }
        }
        return "";
    }

    /**
     * Place building in row y nearest to the back
     *
     * @param buildAction the building type
     * @param y            the y
     * @return the result
     **/
    private Action placeBuildingInRowFromBack(int buildAction, int y, int priority) {
        for (int i = 2; i < gameState.gameDetails.mapWidth / 2; i++) {
            if (isCellEmpty(i, y)) {
                return new Action(i, y, buildAction, priority);
            }
        }
        return new Action(0, 0, 0, 0);
    }

    /**
     * Place building in row y nearest to the front
     *
     * @param buildAction the building type
     * @param y            the y
     * @return the result
     **/
    private Action placeBuildingInRowFromFront(int buildAction, int y, int priority) {
        for (int i = gameState.gameDetails.mapWidth / 2 - 1; i > 1; i--) {
            if (isCellEmpty(i, y)) {
                return new Action(i, y, buildAction, priority);
            }
        }
        return new Action(0, 0, 0, 0);
    }
    /**
     * Construct build command
     *
     * @param x            the x
     * @param y            the y
     * @param buildingType the building type
     * @return the result
     **/
    private String buildCommand(int x, int y, BuildingType buildingType) {
        return String.format("%s,%d,%s", String.valueOf(x), y, buildingType.getCommandCode());
    }

    /**
     * Get all buildings for player in row y
     *
     * @param playerType the player type
     * @param filter     the filter
     * @param y          the y
     * @return the result
     **/
    private List<Building> getAllBuildingsForPlayer(PlayerType playerType, Predicate<Building> filter, int y) {
        return gameState.getGameMap().stream()
                .filter(c -> c.cellOwner == playerType && c.y == y)
                .flatMap(c -> c.getBuildings().stream())
                .filter(filter)
                .collect(Collectors.toList());
    }

    /**
     * Get all empty cells for column x
     *
     * @param x the x
     * @return the result
     **/
    private List<CellStateContainer> getListOfEmptyCellsForColumn(int x) {
        return gameState.getGameMap().stream()
                .filter(c -> c.x == x && isCellEmpty(x, c.y))
                .collect(Collectors.toList());
    }

    /**
     * Checks if cell at x,y is empty
     *
     * @param x the x
     * @param y the y
     * @return the result
     **/
    private boolean isCellEmpty(int x, int y) {
        Optional<CellStateContainer> cellOptional = gameState.getGameMap().stream()
                .filter(c -> c.x == x && c.y == y)
                .findFirst();

        if (cellOptional.isPresent()) {
            CellStateContainer cell = cellOptional.get();
            return cell.getBuildings().size() <= 0;
        } else {
            System.out.println("Invalid cell selected");
        }
        return true;
    }

    /**
     * Checks if building can be afforded
     *
     * @param buildingType the building type
     * @return the result
     **/
    private boolean canAffordBuilding(BuildingType buildingType) {
        return getEnergy(PlayerType.A) >= getPriceForBuilding(buildingType);
    }

    /**
     * Gets energy for player type
     *
     * @param playerType the player type
     * @return the result
     **/
    private int getEnergy(PlayerType playerType) {
        return gameState.getPlayers().stream()
                .filter(p -> p.playerType == playerType)
                .mapToInt(p -> p.energy)
                .sum();
    }

    /**
     * Gets price for building type
     *
     * @param buildingType the player type
     * @return the result
     **/
    private int getPriceForBuilding(BuildingType buildingType) {
        return gameState.gameDetails.buildingsStats.get(buildingType).price;
    }

    /**
     * Gets price for most expensive building type
     *
     * @return the result
     **/
    private int getMostExpensiveBuildingPrice() {
        return gameState.gameDetails.buildingsStats
                .values().stream()
                .mapToInt(b -> b.price)
                .max()
                .orElse(0);
    }
}
