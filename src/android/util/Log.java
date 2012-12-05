package android.util;

/**
 * Created with IntelliJ IDEA.
 * User: pgouvas
 * Date: 9/15/12
 * Time: 1:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class Log {


    public static void w(String string, Exception ex) {
        System.out.println("Exception: "+string+" "+ex);
    }

    public static void w(String str1, String str2) {
        System.out.println("str1: "+str1+" str2:"+str2);
    }

    public static void d(String str1, String str2) {
        System.out.println("str1: "+str1+" str2:"+str2);
    }

    public static void e(String str1, String str2) {
        System.out.println("str1: "+str1+" str2:"+str2);
    }
}
