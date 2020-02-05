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

    /**
     * Precalculate some value
     *
     **/
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

    /**
     * Print debug to debug.txt
     *
     **/
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
        // Debug
        if (gameState.gameDetails.round == 0) {
            try {
              Files.write(Paths.get("debug.txt"), "Created\n\n".getBytes());
            } catch (IOException ee) {
              // pass
            }
        }
        debug("Round: " + Integer.toString(gameState.gameDetails.round) + "\n");

        // Pre
        int totalBuildingsCountAlly = 0;
        int totalBuildingsCountEnemy = 0;
        int totalEnergyCountAlly = 0;
        int totalAttackCountAlly = 0;
        int totalDefenseCountAlly = 0;
        int totalEnergyCountEnemy = 0;
        int totalAttackCountEnemy = 0;
        int totalDefenseCountEnemy = 0;
        for (int i = 0; i < gameState.gameDetails.mapHeight; i++) {
            totalBuildingsCountAlly += energyBuildingsCountAlly.get(i) + attackBuildingsCountAlly.get(i) + defenseBuildingsCountAlly.get(i);
            totalBuildingsCountEnemy += energyBuildingsCountEnemy.get(i) + attackBuildingsCountEnemy.get(i) + defenseBuildingsCountEnemy.get(i);
            totalEnergyCountAlly += energyBuildingsCountAlly.get(i);
            totalAttackCountAlly += attackBuildingsCountAlly.get(i);
            totalDefenseCountAlly += defenseBuildingsCountAlly.get(i);
            totalEnergyCountEnemy += energyBuildingsCountEnemy.get(i);
            totalAttackCountEnemy += attackBuildingsCountEnemy.get(i);
            totalDefenseCountEnemy += defenseBuildingsCountEnemy.get(i);
        }

        // Energy Building
        int idealEnergyPerRow = 2;
        int energyPos = -1; int energyVal = -1;
        for (int i = 0; i < gameState.gameDetails.mapHeight; i++) {
            int currentValue = 0;
            // Relative to attack building
            currentValue += Math.max((5) * (attackBuildingsCountAlly.get(i) - attackBuildingsCountEnemy.get(i)), 0);
            // Relative to ally energy
            currentValue += Math.max((5) * (idealEnergyPerRow - energyBuildingsCountAlly.get(i)), 0);
            // Check
            if (currentValue >= energyVal) {
                energyVal = currentValue;
                energyPos = i;
            }
        }
        debug("Energy val: " + Integer.toString(energyVal) + "\n");


        // Attack Building
        int attackPos = -1; int attackVal = -1; boolean needAttack = false;
        for (int i = 0; i < gameState.gameDetails.mapHeight; i++) {
            int currentValue = 0;
            // Relative to attack building
            if (attackBuildingsCountEnemy.get(i) > 0 && attackBuildingsCountAlly.get(i) == 0) {
                currentValue += 10; needAttack = true;
            }
            currentValue += Math.max((3) * (attackBuildingsCountEnemy.get(i) - attackBuildingsCountAlly.get(i)), 0);
            // Relative to ally attack
            if (attackBuildingsCountAlly.get(i) == 0)
                currentValue += 3;
            // Relative to ally defense
            if (defenseBuildingsCountAlly.get(i) > 0)
                currentValue += 5;
            // Check
            if (currentValue >= attackVal) {
                attackVal = currentValue;
                attackPos = i;
            }
        }
        debug("Attack val: " + Integer.toString(attackVal) + "\n");


        // Defense Building
        int defensePos = -1; int defenseVal = -1; boolean needDefense = false;
        for (int i = 0; i < gameState.gameDetails.mapHeight; i++) {
            int currentValue = 0;
            // Relative to ally attack and defense building
            if (defenseBuildingsCountAlly.get(i) == 0 && attackBuildingsCountAlly.get(i) > 0)
                currentValue += 10;
            else if (defenseBuildingsCountAlly.get(i) == 0)
                currentValue += 5;
            // Relative to enemy attack
            currentValue += (1) * attackBuildingsCountEnemy.get(i);
            if (defenseBuildingsCountAlly.get(i) == 0 && attackBuildingsCountEnemy.get(i) > 0)
                needDefense = true;
            // Check
            if (currentValue >= defenseVal) {
                defenseVal = currentValue;
                defensePos = i;
            }
        }
        debug("Defense val: " + Integer.toString(defenseVal) + "\n");


        // Verdict
        String command = "";
        debug("Energy: " + Integer.toString(getEnergy(PlayerType.A)) + "\nVerdict: ");
        // Build energy first
        int lowEnergyBuildingCount = 7;
        if (totalEnergyCountAlly < lowEnergyBuildingCount && canAffordBuilding(BuildingType.ENERGY)) {
            debug("Energy\n");
            command = placeBuildingInRowFromBack(BuildingType.ENERGY, energyPos);
        }
        // Build defense
        if (needDefense && canAffordBuilding(BuildingType.DEFENSE)) {
            debug("Defense\n");
            command = placeBuildingInRowFromFront(BuildingType.DEFENSE, defensePos);
        }
        // Build attack
        int ngegasTurn = 30;
        if ((needAttack || gameState.gameDetails.round < ngegasTurn) && canAffordBuilding(BuildingType.ATTACK)) {
            debug("Attack\n");
            command = placeBuildingInRowFromBack(BuildingType.ATTACK, 1, attackPos);
        }
        // Random attack or defense on best position
        if (command == "" && canAffordBuilding(BuildingType.DEFENSE)) {
            if ((new Random()).nextInt(100) <= 35) {
                debug("Random Defense\n");
                command = placeBuildingInRowFromFront(BuildingType.DEFENSE, defensePos);
                if (command == "")
                    command = placeBuildingInRowFromBack(BuildingType.ATTACK, 1, attackPos);
            } else {
                debug("Random Attack\n");
                command = placeBuildingInRowFromBack(BuildingType.ATTACK, 1, attackPos);
                if (command == "")
                    command = placeBuildingInRowFromFront(BuildingType.DEFENSE, defensePos);
            }
        }
        if (command == "")
            debug("Nop\n");
        debug("\n");
        return command;
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
     * @param minX         the minX
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
    private String placeBuildingInRowFromBack(BuildingType buildingType, int minX, int y) {
        for (int i = minX; i < gameState.gameDetails.mapWidth / 2; i++) {
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
}
