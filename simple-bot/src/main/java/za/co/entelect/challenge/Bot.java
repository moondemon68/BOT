package za.co.entelect.challenge;

import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.BuildingType;
import za.co.entelect.challenge.enums.PlayerType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static za.co.entelect.challenge.enums.BuildingType.*;

public class Bot {
    private static final String NOTHING_COMMAND = "";
    private GameState gameState;
    private GameDetails gameDetails;
    private int gameWidth;
    private int gameHeight;
    private Player myself;
    private Player opponent;
    private List<CellStateContainer> map;
    private List<Building> buildings;
    private List<Missile> missiles;
    private int[] ourEnergyCount;
    private int totalEnergyCount;
    private int[] ourDefenseCount;
    private int[] ourAttackCount;
    private int[] ourTeslaCount;
    private int totalTeslaCount;
    private int[] theirEnergyCount;
    private int[] theirDefenseCount;
    private int[] theirAttackCount;
    private int[] theirTeslaCount;
    private int[] inboundMissileCount;
    private int[] frontline;
    private int[] frontmost;

    /**
     * Constructor
     *
     * @param gameState the game state
     **/
    public Bot(GameState gameState) {
        this.gameState = gameState;
        gameDetails = gameState.getGameDetails();
        gameWidth = gameDetails.mapWidth;
        gameHeight = gameDetails.mapHeight;
        myself = gameState.getPlayers().stream().filter(p -> p.playerType == PlayerType.A).findFirst().get();
        opponent = gameState.getPlayers().stream().filter(p -> p.playerType == PlayerType.B).findFirst().get();

        map = gameState.getGameMap();

        ourEnergyCount = new int[gameHeight];
        totalEnergyCount = 0;
        ourDefenseCount = new int[gameHeight];
        ourAttackCount = new int[gameHeight];
        ourTeslaCount = new int[gameHeight];
        theirEnergyCount = new int[gameHeight];
        theirDefenseCount = new int[gameHeight];
        theirAttackCount = new int[gameHeight];
        theirTeslaCount = new int[gameHeight];
        inboundMissileCount = new int[gameHeight];
        frontline = new int[gameHeight];
        frontmost = new int[gameHeight];
        totalTeslaCount = 0;

        buildings = gameState.getGameMap().stream()
                .flatMap(c -> c.getBuildings().stream())
                .collect(Collectors.toList());

        missiles = gameState.getGameMap().stream()
                .flatMap(c -> c.getMissiles().stream())
                .collect(Collectors.toList());
    }

    /**
     * Run
     *
     * @return the result
     **/
    public String run() {
        double[] urgencyList = new double[gameHeight];
        int maxPrio = 0;
        for (int i = 0; i < gameHeight; i++) {
            checkRow(i);
        }
        for (int i = 0; i < gameHeight; i++) {
            urgencyList[i] = checkRowUrgency(i);
            if (urgencyList[i] > urgencyList[maxPrio]) maxPrio = i;
        }
        return actOnRow(maxPrio);
    }

    private void checkRow(int row) {
        ourEnergyCount[row] = 0;
        ourDefenseCount[row] = 0;
        ourAttackCount[row] = 0;
        ourTeslaCount[row] = 0;

        theirEnergyCount[row] = 0;
        theirDefenseCount[row] = 0;
        theirAttackCount[row] = 0;
        theirTeslaCount[row] = 0;

        frontline[row] = -1;
        frontmost[row] = 0;

        inboundMissileCount[row] = 0;

        for (int i = 0; i < gameWidth; i++) {
            CellStateContainer c = getCell(i, row);
            inboundMissileCount[row] += c.getMissiles().stream().filter(mi -> mi.getPlayerType() == PlayerType.B).count();
            if (!c.getBuildings().isEmpty()) {
                BuildingType t = c.getBuildings().get(0).buildingType;
                if (i < gameWidth/2) {
                    switch (t) {
                        case ATTACK:
                            ourAttackCount[row]++;
                            break;
                        case DEFENSE:
                            ourDefenseCount[row]++;
                            break;
                        case ENERGY:
                            ourEnergyCount[row]++;
                            totalEnergyCount++;
                            break;
                        case TESLA:
                            ourTeslaCount[row]++;
                            totalTeslaCount++;
                            break;
                    }
                } else {
                    switch (t) {
                        case ATTACK:
                            theirAttackCount[row]++;
                            break;
                        case DEFENSE:
                            theirDefenseCount[row]++;
                            break;
                        case ENERGY:
                            theirEnergyCount[row]++;
                            break;
                        case TESLA:
                            theirTeslaCount[row]++;
                            break;
                    }
                }
            } else {
                if (i < gameWidth/2) {
                    if (frontline[row] < 0) {
                        frontline[row] = i;
                    }
                    frontmost[row] = i;
                }
            }
        }
    }

    private double checkRowUrgency(int row) {
        double urgency = 0;
        if (totalEnergyCount < 5) {
            urgency += 100 - theirAttackCount[row] - theirDefenseCount[row] - ourEnergyCount[row];
        } else {
            urgency += (theirAttackCount[row] - 0.5*ourAttackCount[row] - ourDefenseCount[row]);
            urgency += (theirDefenseCount[row] - ourAttackCount[row]);
            urgency += (theirEnergyCount[row] - ourEnergyCount[row]);
            urgency += inboundMissileCount[row]*2;
            if (inboundMissileCount[row] > 0) {
                if (ourDefenseCount[row] + ourAttackCount[row] + ourEnergyCount[row] == 0) {
                    urgency += 10;
                } else if (ourDefenseCount[row] == 0) {
                    urgency += 5;
                }
                urgency += 3;
            }
            if (theirTeslaCount[row] > 0) urgency += 8;
            if (theirAttackCount[row] == 0) urgency += 1;
            if (frontline[row] < 0) urgency -= 100;
        }
        return urgency;
    }

    private String actOnRow(int row) {
        if (inboundMissileCount[row] > 0 && ourDefenseCount[row] == 0) {
            return buildIfValid(DEFENSE, row);
        } else if (theirAttackCount[row] == 0 && theirDefenseCount[row] != 0) {
            return buildIfValid(ENERGY, row);
        } else if (inboundMissileCount[row] > 2 && ourDefenseCount[row] < 2) {
            return buildIfValid(DEFENSE, row);
        } else if (theirAttackCount[row] == 0) {
            return buildIfValid(ATTACK, row);
        } else if (ourDefenseCount[row] < 0.5*(theirAttackCount[row] - ourAttackCount[row])) {
            return buildIfValid(DEFENSE, row);
        } else if (theirAttackCount[row] - ourAttackCount[row] > 0) {
            return buildIfValid(ATTACK, row);
        } else {
            return buildIfValid(ENERGY, row);
        }
    }

    private String buildIfValid(BuildingType t, int row) {
        if (t == ENERGY && totalEnergyCount > 12) {
            t = ATTACK;
        }
        if (totalTeslaCount < 2 && t == ATTACK && myself.energy > 300 && ourTeslaCount[row] < 1) {
            if ((row+1 < gameHeight && ourTeslaCount[row+1] < 1) && (row-1 > 0 && ourTeslaCount[row-1] < 1)) t = TESLA;
        }
        if (myself.energy >= 100 && gameDetails.round % 30 == 0) {
            t = IRONCURTAIN;
        }
        int cost = t == ENERGY ? 20 : 30;
        if (myself.energy >= cost) {
            return t.buildCommand(t == DEFENSE ? frontmost[row] : frontline[row], row);
        } else if (myself.energy >= 20) {
            return ENERGY.buildCommand(frontline[row], row);
        } else {
            return "0,0,6";
        }
    }

    private CellStateContainer getCell(int x, int y) {
        return map.stream().filter(c -> c.x == x && c.y == y).findAny().get();
    }
}
