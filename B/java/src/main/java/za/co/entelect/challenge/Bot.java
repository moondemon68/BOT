package za.co.entelect.challenge;

import za.co.entelect.challenge.entities.Building;
import za.co.entelect.challenge.entities.CellStateContainer;
import za.co.entelect.challenge.entities.GameState;
import za.co.entelect.challenge.enums.BuildingType;
import za.co.entelect.challenge.enums.PlayerType;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.lang.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;



public class Bot {

    private GameState gameState;

    private List<Integer> energyBuildingsCountAlly = new ArrayList<Integer>();
    private List<Integer> attackBuildingsCountAlly = new ArrayList<Integer>();
    private List<Integer> defenseBuildingsCountAlly = new ArrayList<Integer>();
    private List<Integer> energyBuildingsCountEnemy = new ArrayList<Integer>();
    private List<Integer> attackBuildingsCountEnemy = new ArrayList<Integer>();
    private List<Integer> defenseBuildingsCountEnemy = new ArrayList<Integer>();

    /**
     * Constructor
     *
     * @param gameState the game state
     **/
    public Bot(GameState gameState) {
        this.gameState = gameState;
        gameState.getGameMap();
        Precalculate();
    }

    private void debug(String txt) {
        try {
            Files.write(Paths.get("debug.txt"), txt.getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            // Pass
        }
    }

    /**
     * Run
     *
     * @return the result
     **/
    public String run() {
        String command = "";

        // Debug
        try {
            Files.write(Paths.get("debug.txt"), ("").getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            try {
                Files.write(Paths.get("debug.txt"), "Created\n".getBytes());
            } catch (IOException ee) {
              // pass
            }
        }

        // Energy building
        // Constants
        int lowEnergyBuildingCount = 10;
        int idealEnergyBuildingCountPerRow = 2;
        int totalEnergyBuildingsCountAlly = 0;
        for (int i = 0; i < 8; i++)
            totalEnergyBuildingsCountAlly += energyBuildingsCountAlly.get(i);
        // Move value
        int energyPos = -1; int energyVal = 0;
        for (int i = 0; i < gameState.gameDetails.mapHeight; i++) {
            int valRelToEn = (1) * (idealEnergyBuildingCountPerRow - energyBuildingsCountAlly.get(i));
            int valRelToAtDef = (-1) * (attackBuildingsCountEnemy.get(i) - attackBuildingsCountAlly.get(i) + 2 * defenseBuildingsCountAlly.get(i));
            int currentValue = valRelToEn + valRelToAtDef;
            if (currentValue >= energyVal) {
                energyPos = i;
                energyVal = currentValue;
            }
        }
        if (totalEnergyBuildingsCountAlly < lowEnergyBuildingCount)
            energyVal += 20;
        debug("Energy val: " + Integer.toString(energyVal) + "\n");

        // Attack building
        // Move value
        int attackPos = -1; int attackVal = 0;
        for (int i = 0; i < gameState.gameDetails.mapHeight; i++) {
            int valRelToEnemyAt = (10) * attackBuildingsCountEnemy.get(i);
            int valRelToAllyDef = (3) * defenseBuildingsCountAlly.get(i);
            int valRelToAllyAt = (-10) * attackBuildingsCountAlly.get(i);
            int currentValue = valRelToEnemyAt + valRelToAllyDef + valRelToAllyAt;
            if (currentValue >= attackVal) {
                attackPos = i;
                attackVal = currentValue;
            }
        }
        debug("Attack val: " + Integer.toString(attackVal) + "\n");

        // Defense building
        // Move value
        int defensePos = -1; int defenseVal = 0;
        for (int i = 0; i < gameState.gameDetails.mapHeight; i++) {
            int valRelToEnemyAt = (1) * attackBuildingsCountEnemy.get(i);
            int valRelToAllyBuilding = (2) * (3 - (energyBuildingsCountAlly.get(i) + attackBuildingsCountAlly.get(i) + defenseBuildingsCountAlly.get(i)));
            int valRelToAllyAt = (5) * attackBuildingsCountAlly.get(i);
            int currentValue = valRelToEnemyAt + valRelToAllyBuilding + valRelToAllyAt;
            if (currentValue >= defenseVal) {
                defensePos = i;
                defenseVal = currentValue;
            }
        }
        debug("Defense val: " + Integer.toString(defenseVal) + "\n\n");

        // Verdict
        int maxVal = Math.max(energyVal, Math.max(attackVal, defenseVal));
        if (energyVal == maxVal && canAffordBuilding(BuildingType.ENERGY)) {
            return placeBuildingInRowFromBack(BuildingType.ENERGY, energyPos);
        } else if (attackVal == maxVal && canAffordBuilding(BuildingType.ATTACK)) {
            return placeBuildingInRowFromBack(BuildingType.ATTACK, attackPos);
        } else if (defenseVal == maxVal && canAffordBuilding(BuildingType.DEFENSE)) {
            return placeBuildingInRowFromFront(BuildingType.DEFENSE, defensePos);
        } else {
            return "";
        }
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
     * Place building in row y nearest to the front
     *
     * @param buildingType the building type
     * @param y            the y
     * @return the result
     **/
    private String placeBuildingInRowFromFront(BuildingType buildingType, int y) {
        for (int i = (gameState.gameDetails.mapWidth / 2) - 1; i >= 0; i--) {
            if (isCellEmpty(i, y)) {
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
            if (isCellEmpty(i, y)) {
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

    // ---
    private Building getBuildingInPos(int x, int y) {
        return gameState.getGameMap().stream()
                .filter(c -> c.x == x && c.y == y)
                .flatMap(c -> c.getBuildings().stream())
                .collect(Collectors.toList())
                .get(0);
    }

    private void Precalculate() {
        for (int i = 0; i < gameState.gameDetails.mapHeight; i++) {
            energyBuildingsCountAlly.add(i, getAllBuildingsForPlayer(PlayerType.A, b -> b.buildingType == BuildingType.ENERGY, i).size());
            attackBuildingsCountAlly.add(i, getAllBuildingsForPlayer(PlayerType.A, b -> b.buildingType == BuildingType.ATTACK, i).size());
            defenseBuildingsCountAlly.add(i, getAllBuildingsForPlayer(PlayerType.A, b -> b.buildingType == BuildingType.DEFENSE, i).size());

            energyBuildingsCountEnemy.add(i, getAllBuildingsForPlayer(PlayerType.B, b -> b.buildingType == BuildingType.ENERGY, i).size());
            attackBuildingsCountEnemy.add(i, getAllBuildingsForPlayer(PlayerType.B, b -> b.buildingType == BuildingType.ATTACK, i).size());
            defenseBuildingsCountEnemy.add(i, getAllBuildingsForPlayer(PlayerType.B, b -> b.buildingType == BuildingType.DEFENSE, i).size());
        }
    }

    private void printArray(List<Integer> L) {
        try {
            for (int el : L) {
                Files.write(Paths.get("debug.txt"), (Integer.toString(el) + ",").getBytes(), StandardOpenOption.APPEND);
            }
            Files.write(Paths.get("debug.txt"), "\n".getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            // Pass
        }
    }
}
