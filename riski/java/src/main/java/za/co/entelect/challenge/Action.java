package za.co.entelect.challenge;

public class Action {

    private int x, y, action, priority;

    public Action(int x, int y, int action, int priority) {
        this.x = x;
        this.y = y;
        this.action = action;
        this.priority = priority;
    }

    public boolean greaterPriority(Action a) {
        return this.priority > a.priority;
    }

    public String output() {
        if (this.priority == 0) {
            return "";
        }
        return String.valueOf(this.x) + ","
                + String.valueOf(this.y) + ","
                + String.valueOf(this.action);
    }
}
