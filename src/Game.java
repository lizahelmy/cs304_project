
public class Game {
    static int score = 0;
    static int level = 0;
    static boolean running = false;
    static boolean won = false;
    static boolean lose = false;
    
    static Music Mclick = new Music("sounds//11. PAC-MAN - Eating The Fruit.wav", false),
            Eclick = new Music("sounds//13. PAC-MAN - Eating The Ghost.wav", false),
            Dclick = new Music("sounds//15. Fail.wav", false),

    mainMusic = new Music("sounds//17. Game Play.wav", true);
}