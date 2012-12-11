package util;

import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * User: pgouvas
 * Date: 12/8/12
 * Time: 1:40 PM
 * To change this template use File | Settings | File Templates.
 */
public class Generator {

    public static String getChallenge() {
        String challenge = "";
        Random rand = new Random();
        challenge = ""+Math.abs(rand.nextInt());
        return  challenge;
    }//EoM getChallenge

    public static String getSession() {
        String challenge = "";
        Random rand = new Random();
        challenge = ""+Math.abs(rand.nextInt());
        return  challenge;
    }//EoM getSession

}//EoC Generator
