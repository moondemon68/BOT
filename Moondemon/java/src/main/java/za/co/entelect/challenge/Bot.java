package za.co.entelect.challenge;

import za.co.entelect.challenge.entities.Building;
import za.co.entelect.challenge.entities.CellStateContainer;
import za.co.entelect.challenge.entities.GameState;
import za.co.entelect.challenge.enums.BuildingType;
import za.co.entelect.challenge.enums.PlayerType;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Bot {

    private GameState gameState;

    public Bot(GameState gameState) {
        this.gameState = gameState;
        gameState.getGameMap();
    }

    public String run() {
        String command = "";

        // Count total number of our and enemy buildings (for each type)
        int ourTotalAttack = 0, enemyTotalAttack = 0, ourTotalDefense = 0, enemyTotalDefense = 0, ourTotalEnergy = 0;
        for (int i = 0; i < gameState.gameDetails.mapHeight; i++) {
            ourTotalAttack += getAllBuildingsForPlayer(PlayerType.A, b -> b.buildingType == BuildingType.ATTACK, i).size();
            enemyTotalAttack += getAllBuildingsForPlayer(PlayerType.B, b -> b.buildingType == BuildingType.ATTACK, i).size();
            ourTotalDefense += getAllBuildingsForPlayer(PlayerType.A, b -> b.buildingType == BuildingType.DEFENSE, i).size();
            enemyTotalDefense += getAllBuildingsForPlayer(PlayerType.B, b -> b.buildingType == BuildingType.DEFENSE, i).size();
            ourTotalEnergy += getAllBuildingsForPlayer(PlayerType.A, b -> b.buildingType == BuildingType.ENERGY, i).size();
        }

        if (buildEnergy() != "") command = buildEnergy();

        // If our back column is full and enemy attack or defense is more than our attack, then build attack.
        if (command == "" || enemyTotalAttack > ourTotalAttack || enemyTotalDefense > ourTotalAttack) {
            if (buildAttack() != "") command = buildAttack();
        }
        
        // This function can override the previous attack command. This counters a common enemy strategy, focusing in only one row.
        if (buildAntiSerialAttack() != "") command = buildAntiSerialAttack();
        
        // If we are overwhelming enemy (our attack >= enemy attack + 12)
        if (ourTotalAttack >= enemyTotalAttack + 12) {
            // Check the 2 front columns, fill them with defense buildings. Prioritize row with least our defense.
            // If the defense is full, then build energy.
            if (ourTotalDefense <= 16) {
                if (buildDefense() != "") command = buildDefense();
            } else if (ourTotalEnergy <= 16) {
                if (buildOverwhelmingEnergy() != "") command = buildOverwhelmingEnergy();
            }
        }

        // If our energy is above 100, deploy iron curtain.
        if (getEnergy(PlayerType.A) >= 100) {
            if (buildIronCurtain() != "") command = buildIronCurtain();
        }

        // Yes, we don't have Tesla. Tesla is for spoiled rich kids.

        return command;
    }

    // If column 0, row 0 to 7 is not filled with energy, then build them. This can be overrided if enemy is building something other than energy.
    private String buildEnergy() {
        String command = "";
        int numberOfEnergyBuilding = 0;
        int availableRowToPlaceEnergy = -1;
        for (int i = 0; i < gameState.gameDetails.mapHeight; i++) {
            int currentRowEnergy = getAllBuildingsForPlayer(PlayerType.A, b -> b.buildingType == BuildingType.ENERGY, i).size();
            if (currentRowEnergy == 0) {
                availableRowToPlaceEnergy = i;
            }
        }
        if (availableRowToPlaceEnergy != -1) {
            if (canAffordBuilding(BuildingType.ENERGY))
                command = placeBuildingInRowFromBack(BuildingType.ENERGY, availableRowToPlaceEnergy);
        }
        return command;
    }

    // Build attack. Prioritize row with more enemy attack. If there is still a draw, prioritize row with less our attack. This command can be overrided.
    private String buildAttack() {
        String command = "";
        int minOurAttack = 10, maxEnemyAttack = 0;
        for (int i = 0; i < gameState.gameDetails.mapHeight; i++) {
            int ourAttack = getAllBuildingsForPlayer(PlayerType.A, b -> b.buildingType == BuildingType.ATTACK, i).size();
            int ourDefense = getAllBuildingsForPlayer(PlayerType.A, b -> b.buildingType == BuildingType.DEFENSE, i).size();
            int enemyAttack = getAllBuildingsForPlayer(PlayerType.B, b -> b.buildingType == BuildingType.ATTACK, i).size();
            int enemyDefense = getAllBuildingsForPlayer(PlayerType.B, b -> b.buildingType == BuildingType.DEFENSE, i).size();
            if (enemyAttack > maxEnemyAttack) {
                maxEnemyAttack = enemyAttack;
            }
            if (enemyAttack >= maxEnemyAttack) {
                if (ourAttack < minOurAttack) {
                    minOurAttack = ourAttack;
                    if (placeBuildingInRowFromSixthRow(BuildingType.ATTACK, i) != "") command = placeBuildingInRowFromSixthRow(BuildingType.ATTACK, i);
                }
            }
        }
        return command;
    }

    // For every row, if our attack + 2 <= their attack, if our defense * 2 is less than the difference, build defense, else build attack. Prioritize row with most difference.
    private String buildAntiSerialAttack() {
        String command = "";
        int maxDiff = 2;    // If the difference is still less than 2, don't do anything.
        for (int i = 0; i < gameState.gameDetails.mapHeight; i++) {
            int ourAttack = getAllBuildingsForPlayer(PlayerType.A, b -> b.buildingType == BuildingType.ATTACK, i).size();
            int ourDefense = getAllBuildingsForPlayer(PlayerType.A, b -> b.buildingType == BuildingType.DEFENSE, i).size();
            int enemyAttack = getAllBuildingsForPlayer(PlayerType.B, b -> b.buildingType == BuildingType.ATTACK, i).size();
            if (enemyAttack - ourAttack >= maxDiff) {
                maxDiff = enemyAttack - ourAttack;
                if (ourDefense*2 <= enemyAttack - ourAttack) {
                    if (placeBuildingInRowFromFront(BuildingType.DEFENSE, i) != "")
                        command = placeBuildingInRowFromFront(BuildingType.DEFENSE, i);
                } else {
                    command = placeBuildingInRowFromSixthRow(BuildingType.ATTACK, i);
                }
            }
        }
        return command;
    }

    private String buildDefense() {
        String command = "";
        // If we are overwhelming enemy (attack >= enemy + 12) and our frontline is not full of defenses, build defense. Prioritize row with least our defense.
        int minOurDefense = 10;
        for (int i = 0; i < gameState.gameDetails.mapHeight; i++) {
            int ourDefense = getAllBuildingsForPlayer(PlayerType.A, b -> b.buildingType == BuildingType.DEFENSE, i).size();
            if (ourDefense < minOurDefense) {
                minOurDefense = ourDefense;
                command = placeBuildingInRowFromFront(BuildingType.DEFENSE, i);
            }
        }
        return command;
    }

    private String buildOverwhelmingEnergy() {
        String command = "";
        int buildingEnergy = 0;
        // Prioritize row with least energy.
        int minOurEnergy = 10;
        for (int i = 0; i < gameState.gameDetails.mapHeight; i++) {
            int ourEnergy = getAllBuildingsForPlayer(PlayerType.A, b -> b.buildingType == BuildingType.ENERGY, i).size();
            if (ourEnergy < minOurEnergy) {
                minOurEnergy = ourEnergy;
                command = placeBuildingInRowFromFront(BuildingType.ENERGY, i);
                buildingEnergy = 1;
            }
        }
        return command;
    }

    private String buildIronCurtain() {
        return "5,5,5";
    }

    /**
     * Place building in row y nearest to the front
     *
     * @param buildingType the building type
     * @param y            the y
     * @return the result
     **/
    private String placeBuildingInRowFromFront(BuildingType buildingType, int y) {
        for (int i = (gameState.gameDetails.mapWidth / 2) - 1; i >= 0; i--) {
            if (isCellEmpty(i, y) && canAffordBuilding(buildingType)) {
                return buildCommand(i, y, buildingType);
            }
        }
        return "";
    }

    /**
     * Place building in row y nearest to the 6th row
     * This is usefull to place attack building
     *
     * @param buildingType the building type
     * @param y            the y
     * @return the result
     **/
    private String placeBuildingInRowFromSixthRow(BuildingType buildingType, int y) {
        for (int i = gameState.gameDetails.mapWidth / 2 - 3; i >= 0; i--) {
            if (isCellEmpty(i, y) && canAffordBuilding(buildingType)) {
                return buildCommand(i, y, buildingType);
            }
        }
        return "";
    }

    /**
     * Place building in row y nearest to the back
     *
     * @param buildingType the building type
     * @param y            the y
     * @return the result
     **/
    private String placeBuildingInRowFromBack(BuildingType buildingType, int y) {
        for (int i = 0; i < gameState.gameDetails.mapWidth / 2; i++) {
            if (isCellEmpty(i, y) && canAffordBuilding(buildingType)) {
                return buildCommand(i, y, buildingType);
            }
        }
        return "";
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

}
